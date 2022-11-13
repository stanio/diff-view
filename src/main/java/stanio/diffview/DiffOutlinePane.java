/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;

@SuppressWarnings("serial")
class DiffOutlinePane extends JPanel {

    private JTree tree;

    private JTextField filter;

    public DiffOutlinePane() {
        this.tree = new JTree();
        this.filter = new JTextField();
        initUI();
        expandTree();
    }

    private void initUI() {
        if (tree == null) {
            return;
        }
        super.removeAll();
        super.setLayout(new BorderLayout(5, 5));

        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);
        tree.getSelectionModel()
                .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        super.add(new JScrollPane(tree), BorderLayout.CENTER);
        super.add(filter, BorderLayout.PAGE_START);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        initUI();
    }

    private void expandTree() {
        for (int n = tree.getRowCount(); n >= 0; n--) {
            tree.expandRow(n);
        }
    }

}
