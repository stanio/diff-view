/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import stanio.diffview.swing.tree.FilteredTreeModel;
import stanio.diffview.swing.tree.PathFoldingTreeModel;
import stanio.diffview.swing.tree.PathFoldingTreeModel.Node;
import stanio.diffview.swing.tree.Trees;

@SuppressWarnings("serial")
class DiffOutlinePane extends JPanel {

    FileTreeModel fileTree;
    FilteredTreeModel filteredTree;

    JTree tree;

    private JTextField filterField;
    JTextComponent diffText;

    public DiffOutlinePane() {
        this.fileTree = new FileTreeModel();
        this.filteredTree = new FilteredTreeModel(fileTree);
        this.tree = new JTree(new PathFoldingTreeModel(filteredTree));
        this.filterField = new JTextField();
        initUI();
    }

    private void initUI() {
        if (tree == null) {
            return;
        }
        super.removeAll();
        super.setLayout(new BorderLayout());

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel()
                .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.putClientProperty("JTree.lineStyle", "None");
        Trees.expandAll(tree);
        Trees.keepExpanded(tree);

        ActionMap actionMap = filterField.getActionMap();
        actionMap.put("goto-tree", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                tree.setSelectionRow(0);
                tree.requestFocusInWindow();
            }
        });
        actionMap.put("focus-filter", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                filterField.requestFocusInWindow();
            }
        });

        actionMap = tree.getActionMap();
        actionMap.put("activate-node", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                TreePath path = tree.getSelectionPath();
                if (path == null) return;

                Node node = (Node) path.getLastPathComponent();
                if (filteredTree.isLeaf(node.getFoldedPath().getLastPathComponent())) {
                    scrollIntoView(path);
                } else if (tree.isCollapsed(path)) {
                    tree.expandPath(path);
                } else {
                    tree.collapsePath(path);
                }
            }
        });
        Action upAction = actionMap.get(tree.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)));
        actionMap.put("goto-filter", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (tree.getLeadSelectionRow() == 0) {
                    tree.clearSelection();
                    filterField.requestFocusInWindow();
                } else if (upAction != null) {
                    upAction.actionPerformed(event);
                }
            }
        });
        Action rightAction = actionMap.get(tree.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)));
        actionMap.put("scroll2-file", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                TreePath path = tree.getSelectionPath();
                if (path == null) return;

                Node node = (Node) path.getLastPathComponent();
                if (filteredTree.isLeaf(node.getFoldedPath().getLastPathComponent())) {
                    scrollIntoView(path, false);
                } else if (rightAction != null) {
                    rightAction.actionPerformed(event);
                }
            }
        });

        filterField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "goto-tree");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "goto-filter");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate-node");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "scroll2-file");

        filterField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) {
                filterField.selectAll();
            }
        });
        //FileIcons.install(tree);

        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    TreePath path = tree.getUI().getClosestPathForLocation(tree, event.getX(), event.getY());
                    if (path == null) return;

                    Node node = (Node) path.getLastPathComponent();
                    if (filteredTree.isLeaf(node.getFoldedPath().getLastPathComponent())) {
                        scrollIntoView(path);
                    }
                }
            }
        });

        filterField.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke
                .getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "focus-filter");

        // https://www.formdev.com/flatlaf/customizing/
        // https://www.formdev.com/flatlaf/components/textfield/
        // https://www.formdev.com/flatlaf/client-properties/#JTextField
        filterField.putClientProperty("JTextField.placeholderText", "Filter (Ctrl+T)");
        filterField.putClientProperty("JTextField.showClearButton", true);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void removeUpdate(DocumentEvent e) { updateFilter(); }
            @Override public void insertUpdate(DocumentEvent e) { updateFilter(); }
            @Override public void changedUpdate(DocumentEvent e) {/* */}
        });

        super.add(new JScrollPane(tree), BorderLayout.CENTER);
        super.add(filterField, BorderLayout.PAGE_START);
    }

    void scrollIntoView(TreePath path) {
        scrollIntoView(path, true);
    }

    void scrollIntoView(TreePath path, boolean focus) {
        String filePath = Stream.of(path.getPath())
                                .map(Object::toString)
                                .collect(Collectors.joining("/"))
                                .replaceFirst("^<root>/", "");
        AbstractDocument doc = (AbstractDocument) diffText.getDocument();
        Element p = doc.getParagraphElement(0);
        Rectangle2D rect = null;
        Element section = null;
        while (p != null) {
            String fileAttr = (String) p.getAttributes().getAttribute("file");
            //System.out.println(p.getStartOffset() + ", " + p.getEndOffset() + ": " + fileAttr);
            try {
                if (filePath.equals(fileAttr)) {
                    section = p;
                    rect = diffText.modelToView2D(p.getStartOffset());
                } else if (fileAttr != null && rect != null) {
                    rect.add(diffText.modelToView2D(p.getStartOffset() - 1));
                    break;
                }
            } catch (BadLocationException e) {
                // c'est la vie
                e.printStackTrace();
            }
            Element next = doc.getParagraphElement(p.getEndOffset());
            p = (next == p) ? null : next;
        }
        if (rect == null) {
            return;
        }
        if (p == null) {
            try {
                rect.add(diffText.modelToView2D(doc.getLength() - 1));
            } catch (BadLocationException e) {
                // c'est la vie
                e.printStackTrace();
            }
        }
        rect.setRect(rect.getX(), rect.getY(),
                Math.min(rect.getWidth(), diffText.getParent().getWidth()),
                Math.min(rect.getHeight(), diffText.getParent().getHeight()));
        diffText.scrollRectToVisible(rect instanceof Rectangle
                                     ? (Rectangle) rect
                                     : rect.getBounds());
        diffText.setCaretPosition(section.getStartOffset());
        if (focus) {
            diffText.requestFocusInWindow();
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        initUI();
    }

    void updateFilter() {
        filteredTree.filter(filterFor(
                filterField.getText().toLowerCase(Locale.ROOT)));
    }

    private static Predicate<TreePath> filterFor(String token) {
        return path -> {
            TreePath current = path;
            while (current != null) {
                if (current.getLastPathComponent().toString()
                        .toLowerCase(Locale.ROOT).contains(token)) {
                    return true;
                }
                current = current.getParentPath();
            }
            return false;
        };
    }

}
