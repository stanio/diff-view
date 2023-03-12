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
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
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
import stanio.diffview.udiff.DiffStyles;
import stanio.diffview.udiff.UDiffDocument.Attribute;
import stanio.diffview.udiff.UDiffDocument.StyleName;
import stanio.diffview.udiff.UDiffEditorKit;

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
        initRuler(fromRuler, 0, Attribute.FROM_LINE);
        initRuler(toRuler, 1, Attribute.TO_LINE);

        JPanel ruler = new JPanel(new BorderLayout());
        ruler.add(fromRuler, BorderLayout.LINE_START);
        ruler.add(toRuler, BorderLayout.LINE_END);
        super.setRowHeaderView(ruler);
    }

    private void initRuler(JTextPane lineRuler, float leftGap, Attribute lineAttr) {
        lineRuler.setEditorKit(BoxBackgroundFactory.newEditorKit());
        Font f = diffPane.getFont();
        lineRuler.setFont(f);
        lineRuler.putClientProperty("JComponent.minimumWidth", 0);
        lineRuler.setText("  ");

        StyledDocument numbers = lineRuler.getStyledDocument();
        Style defaultStyle = numbers.getStyle(StyleContext.DEFAULT_STYLE);

        StyleConstants.setAlignment(defaultStyle, StyleConstants.ALIGN_RIGHT);
        final float inset = 2f;
        StyleConstants.setLeftIndent(defaultStyle,
                leftGap == 0 ? inset : leftGap * getFontMetrics(f).charWidth('0'));
        StyleConstants.setRightIndent(defaultStyle, leftGap > 0 ? inset : 0);

        diffPane.getDocument().addDocumentListener(
                new DiffNumbersListener((AbstractDocument) numbers, lineAttr));

        // Prevent ruler from automatic scroll
        lineRuler.addCaretListener(event -> lineRuler.setCaretPosition(0));

        DiffStyles.addTo(numbers);
    }

    private SwingWorker<Void, Void> loader;

    void load(Input input, Runnable callback) {
        if (loader != null) {
            loader.cancel(true);
        }

        EditorKit kit = diffPane.getEditorKit();
        Document doc = diffPane.getDocument();
        if (input.url != null) {
            doc.putProperty(Document.StreamDescriptionProperty, input.url);
        } else if (input.file != null) {
            try {
                doc.putProperty(Document.StreamDescriptionProperty, input.file.toUri().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace(); // Huh, c'est la vie.
            }
        }
        diffPane.putClientProperty("charset", input.charset.name());

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
                    callback.run();
                    return;
                } catch (CancellationException | InterruptedException e) {
                    System.err.println(e);
                } catch (ExecutionException | TimeoutException e) {
                    Throwable cause = e;
                    if (e instanceof ExecutionException && e.getCause() != null) {
                        cause = e.getCause();
                    }
                    cause.printStackTrace(); // XXX: debug
                    JOptionPane.showMessageDialog(DiffTextPane.this,
                            cause.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                DiffView.close(SwingUtilities.windowForComponent(DiffTextPane.this));
            }
        };
        loader.execute();
    }

    private JLabel initStickyHeader() {
        JLabel context = new JLabel();
        context.setFont(diffPane.getFont());
        context.setForeground(DiffStyles.colorWithAlpha(context.getForeground(), 0.5f));
        {
            Insets insets = diffPane.getInsets();
            context.setBorder(BorderFactory
                    .createEmptyBorder(3, insets.left, 3, insets.right));
        }

        Point p = new Point();
        JScrollBar verticalScroll = super.getVerticalScrollBar();
        verticalScroll.addAdjustmentListener(event -> {
            if (event.getValueIsAdjusting()) return;

            int h0 = context.getPreferredSize().height;
            p.y = event.getValue();
            String oldLabel = context.getText();
            context.setText(findLastFile(diffPane.viewToModel2D(p)));
            if (oldLabel == null && context.getText() != null) {
                super.setColumnHeaderView(context);
            } else if (context.getText() == null) {
                super.setColumnHeaderView(null);
            }
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
            String file = (String) paragraph.getAttributes().getAttribute(Attribute.FILE);
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


    private static class DiffNumbersListener implements DocumentListener {

        private final AbstractDocument ruler;

        private final Attribute lineAttr;

        private boolean updateScheduled;
        private int start = -1;
        private int end = -1;
        private AbstractDocument doc;

        DiffNumbersListener(AbstractDocument ruler, Attribute lineAttr) {
            this.ruler = ruler;
            this.lineAttr = lineAttr;
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            new Exception(event.toString()).printStackTrace();
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            invalidateRuler(event);
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            invalidateRuler(event);
        }

        private synchronized void invalidateRuler(DocumentEvent event) {
            int offset = event.getOffset();
            start = (start < 0) ? offset : Math.min(start, offset);
            int length = event.getLength();
            end = (end < 0) ? offset + length
                            : Math.max(end, offset + length);
            if (updateScheduled) {
                return;
            }
            doc = (AbstractDocument) event.getDocument();
            SwingUtilities.invokeLater(() -> {
                try {
                    updateRuler();
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
            updateScheduled = true;
        }

        private synchronized void updateRuler() throws BadLocationException {
            int lineCount = ruler.getDefaultRootElement().getElementCount();
            int lineNo = doc.getDefaultRootElement().getElementIndex(end - 1);
            for (int i = lineCount; i < lineNo; i++) {
                ruler.insertString(ruler.getLength(), "\n", null);
            }

            Element paragraph = doc.getParagraphElement(start);
            while (paragraph.getEndOffset() < end) {
                lineNo = paragraph.getParentElement()
                                  .getElementIndex(paragraph.getStartOffset());
                updateNumber(lineNo, paragraph);
                paragraph = doc.getParagraphElement(paragraph.getEndOffset());
            }
            start = -1;
            end = -1;
            doc = null;
            updateScheduled = false;
        }

        private void updateNumber(int lineNo, Element source) throws BadLocationException {
            Element rulerLine = ruler.getDefaultRootElement().getElement(lineNo);
            AttributeSet attributes = source.getAttributes();
            Object numStr = attributes.getAttribute(lineAttr);

            String styleName = StyleContext.DEFAULT_STYLE;
            if (numStr instanceof String) {
                ruler.replace(rulerLine.getStartOffset(),
                        rulerLine.getEndOffset() - rulerLine.getStartOffset() - 1,
                        numStr.toString(), null);

                if (lineAttr == Attribute.FROM_LINE
                        && attributes.getAttribute(Attribute.TO_LINE) == null) {
                    styleName = StyleName.DELETED_NUMBER;
                } else if (lineAttr == Attribute.TO_LINE
                        && attributes.getAttribute(Attribute.FROM_LINE) == null) {
                    styleName = StyleName.INSERTED_NUMBER;
                }
            } else if (lineAttr == Attribute.FROM_LINE
                    && attributes.getAttribute(Attribute.TO_LINE) != null) {
                styleName = StyleName.INSERTED_NUMBER;
            } else if (lineAttr == Attribute.TO_LINE
                    && attributes.getAttribute(Attribute.FROM_LINE) != null) {
                styleName = StyleName.DELETED_NUMBER;
            }

            StyledDocument rulerDoc = (StyledDocument) ruler;
            Style style = rulerDoc.getStyle(styleName);
            if (style == null) style = rulerDoc.getStyle(StyleContext.DEFAULT_STYLE);
            rulerDoc.setLogicalStyle(rulerLine.getStartOffset(), style);
        }

    } // class DiffNumbersListener


} // class DiffTextPane
