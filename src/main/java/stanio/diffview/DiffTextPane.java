/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.io.Reader;
import java.net.MalformedURLException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import stanio.diffview.DiffView.Input;
import stanio.diffview.swing.LineRuler;
import stanio.diffview.swing.NowrapTextPane;
import stanio.diffview.swing.text.BoxBackgroundFactory;
import stanio.diffview.swing.tree.Trees;

@SuppressWarnings("serial")
class DiffTextPane extends JScrollPane {

    JTextPane diffPane;
    JTextPane fromRuler;
    JTextPane toRuler;

    public DiffTextPane() {
        super(VERTICAL_SCROLLBAR_ALWAYS,
                HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.diffPane = new NowrapTextPane();
        this.fromRuler = new LineRuler(diffPane);
        this.toRuler = new LineRuler(diffPane);
        setUpTextPane();
        initRuler();
        super.setViewportView(diffPane);
        JLabel header = initStickyHeader();
        diffPane.addPropertyChangeListener("font", event -> {
            Font f = diffPane.getFont();
            fromRuler.setFont(f);
            toRuler.setFont(f);
            header.setFont(f);
        });
    }

    private void setUpTextPane() {
        JTextPane textPane = diffPane;
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        textPane.setEditorKit(new UDiffEditorKit());

        Color bg = textPane.getBackground();
        Color fg = textPane.getForeground();
        textPane.setEditable(false);
        textPane.setBackground(new Color(bg.getRGB()));
        textPane.setForeground(new Color(fg.getRGB()));

        textPane.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                textPane.getCaret().setVisible(true);
            }
        });
    }

    private void initRuler() {
        initRuler(fromRuler, 0);
        initRuler(toRuler, 1);

        JPanel ruler = new JPanel(new BorderLayout());
        ruler.add(fromRuler, BorderLayout.LINE_START);
        ruler.add(toRuler, BorderLayout.LINE_END);
        super.setRowHeaderView(ruler);
    }

    private void initRuler(JTextPane lineRuler, float leftGap) {
        lineRuler.setEditorKit(BoxBackgroundFactory.newEditorKit());
        Font f = diffPane.getFont();
        lineRuler.setFont(f);
        lineRuler.putClientProperty("JComponent.minimumWidth", 0);

        StyledDocument numbers = lineRuler.getStyledDocument();
        Style defaultStyle = numbers.getStyle(StyleContext.DEFAULT_STYLE);

        StyleConstants.setAlignment(defaultStyle, StyleConstants.ALIGN_RIGHT);
        final float inset = 2f;
        StyleConstants.setLeftIndent(defaultStyle,
                leftGap == 0 ? inset : leftGap * getFontMetrics(f).charWidth('0'));
        StyleConstants.setRightIndent(defaultStyle, leftGap > 0 ? inset : 0);

        DiffStyles.addTo(numbers);
    }

    private StyledDocument createDocument() {
        StyledDocument doc = (StyledDocument)
                diffPane.getEditorKit().createDefaultDocument();
        //Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);
        DiffStyles.addTo(doc);
        return doc;
    }

    private SwingWorker<Void, Void> loader;

    void load(Input input) {
        if (loader != null) {
            loader.cancel(true);
        }

        EditorKit kit = diffPane.getEditorKit();
        Document doc = createDocument();
        if (input.url != null) {
            doc.putProperty(Document.StreamDescriptionProperty, input.url);
        } else if (input.file != null) {
            try {
                doc.putProperty(Document.StreamDescriptionProperty, input.file.toUri().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace(); // Huh, c'est la vie.
            }
        }

        loader = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try (Reader r = input.stream) {
                    kit.read(r, doc, 0);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get(1, TimeUnit.MILLISECONDS);
                    diffPane.setDocument(doc);
                    diffPane.putClientProperty("charset", input.charset.name());
                    initLineNumbers();
                    return;
                } catch (CancellationException | InterruptedException e) {
                    System.err.println(e);
                } catch (ExecutionException | TimeoutException e) {
                    Throwable cause = e;
                    if (e instanceof ExecutionException && e.getCause() != null) {
                        cause = e.getCause();
                    }
                    JOptionPane.showMessageDialog(DiffTextPane.this,
                            cause.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                DiffView.close(SwingUtilities.windowForComponent(DiffTextPane.this));
            }
        };
        loader.execute();
    }

    void initLineNumbers() {
        DiffOutlinePane outline = getDiffViewAncestor().getOutlinePane();
        DiffLines.initNumbersAndOutline(diffPane, fromRuler, toRuler, outline.fileTree);
        Trees.expandAll(outline.tree);
    }

    private DiffView getDiffViewAncestor() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof DiffView) {
                return (DiffView) parent;
            }
            parent = parent.getParent();
        }
        throw new IllegalStateException("Could not find DiffView ancestor");
    }

    private JLabel initStickyHeader() {
        JLabel context = new JLabel();
        context.setFont(diffPane.getFont());
        context.setForeground(Colors.withAlpha(context.getForeground(), 0.5f));
        super.setColumnHeaderView(context);

        Point p = new Point();
        JScrollBar verticalScroll = super.getVerticalScrollBar();
        verticalScroll.addAdjustmentListener(event -> {
            if (event.getValueIsAdjusting()) return;

            int h0 = context.getPreferredSize().height;
            p.y = event.getValue();
            context.setText(findLastFile(diffPane.viewToModel2D(p)));
            int h1 = context.getPreferredSize().height;
            if (h1 - h0 != 0) {
                SwingUtilities.invokeLater(() ->
                        verticalScroll.getModel().setValue(p.y + (h1 - h0)));
            }
        });
        return context;
    }

    private String findLastFile(int fromIndex) {
        AbstractDocument doc = (AbstractDocument) diffPane.getDocument();
        int pos = fromIndex;
        do {
            Element paragraph = doc.getParagraphElement(pos);
            String file = (String) paragraph.getAttributes().getAttribute("file");
            if (file != null) {
                return file;
            }
            pos = paragraph.getStartOffset() - 1;
        } while (pos >= 0);

        return null;
    }

    /**
     * Defers the execution of the given task to after the next full event
     * processing cycle.
     *
     * @param   task  the task to execute
     * @see     SwingUtilities#invokeLater(Runnable)
     */
    public static void invokeMuchLater(Runnable task) {
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(task));
    }

}
