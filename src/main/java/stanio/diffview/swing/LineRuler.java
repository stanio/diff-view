/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

/**
 * Renders a line ruler for a given text component.
 * <p>
 * Standard setup is as a row header view of a {@code JScrollPane}:</p>
 * <pre>
 *     JTextComponent text;
 *     ...
 *     JScrollPane scrollPane = new JScrollPane(text);
 *     scrollPane.setRowHeaderView(new LineRuler(text));</pre>
 * <p>
 * Basic usage of the ruler is to provide line numbers for the source text.
 * Ruler lines are intended to match the size and position of the source text.
 * Source lines are determined by logical {@linkplain
 * AbstractDocument#getParagraphElement(int) paragraph elements} as implemented
 * by {@code AbstractDocument}s.  That is a single logical line could be wrapped
 * into multiple visual lines.  The ruler should adjust its individual line
 * sizing/spacing appropriately.</p>
 * <p>
 * If the associated text component's {@code Document} is not a subclass of
 * {@code AbstractDocument}, ruler lines may not be adjusted automatically.</p>
 * <p>
 * The ruler content is a {@link StyledDocument} that may be augmented with
 * icons for source-folding, error, info or other indicators.  See
 * {@link JTextPane#insertIcon(javax.swing.Icon)} and {@link
 * StyleConstants#setIcon(javax.swing.text.MutableAttributeSet, javax.swing.Icon)}.</p>
 *
 * @see #showLineNumbers()
 */
public class LineRuler extends NowrapTextPane {

    private static final long serialVersionUID = -6067981992403520371L;

    private JTextComponent textComponent;

    // TODO: Handle partial/incremental updates on source document changes
    // and text component resizing.
    private transient TextComponentListener documentListener;

    private LineRuler() {
        super(newDefaultStyledDocument());
        this.documentListener = new TextComponentListener();
    }

    public LineRuler(JTextComponent textComponent) {
        this();
        setTextComponent(textComponent);
        resetUI();
    }

    private static DefaultStyledDocument newDefaultStyledDocument() {
        DefaultStyledDocument numbers = new DefaultStyledDocument();
        Style defaultStyle = numbers.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setAlignment(defaultStyle, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setTabSet(defaultStyle, new TabSet(new TabStop[0]));
        return numbers;
    }

    private void resetUI() {
        setEditable(false);
        setFocusable(false);
        putClientProperty("caretWidth", 0);

        Color bg = UIManager.getColor("Panel.background");
        if (bg != null) setBackground(bg);

        Color fg = getForeground();
        setForeground(new ColorUIResource(
                new Color(fg.getRGB() & 0x00FFFFFF | 0x80000000, true)));

        Insets insets = getInsets();
        setBorder(BorderFactory
                .createEmptyBorder(insets.top, 0, insets.bottom, 0));
        Style defaultStyle = getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setLeftIndent(defaultStyle, insets.left);
        StyleConstants.setRightIndent(defaultStyle, insets.right);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        resetUI();
    }

    public JTextComponent getTextComponent() {
        return textComponent;
    }

    void setTextComponent(JTextComponent textComponent) {
        JTextComponent oldValue = this.textComponent;
        if (oldValue != null) {
            oldValue.getDocument().removeDocumentListener(documentListener);
        }
        this.textComponent = textComponent;
        firePropertyChange("textComponent", oldValue, textComponent);
        if (textComponent != null) {
            textComponent.getDocument().addDocumentListener(documentListener);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return isMinimumSizeSet() ? getMinimumSize()
                                  : getPreferredSize();
    }

    /**
     * This would likely develop into a mode for automatic line numbering.
     */
    public void showLineNumbers() {
        Document original = (textComponent != null)
                            ? textComponent.getDocument()
                            : null;
        if (original instanceof AbstractDocument) {
            initLineNumbers((AbstractDocument) original);
        }
    }

    private void initLineNumbers(AbstractDocument source) {
        Document numbersText = getDocument();
        try {
            numbersText.remove(0, numbersText.getLength());

            Element last = null;
            Element paragraph = source.getParagraphElement(0);
            int lineNo = 1;
            while (paragraph != last) {
                numbersText.insertString(numbersText.getLength(),
                                         Integer.toString(lineNo++), null);
                numbersText.insertString(numbersText.getLength(), "\n", null);
                last = paragraph;
                paragraph = source.getParagraphElement(last.getEndOffset());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    private class TextComponentListener implements DocumentListener {

        TextComponentListener() {/* no-op */}

        @Override
        public void removeUpdate(DocumentEvent e) {
            debugEvent(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            debugEvent(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            debugEvent(e);
        }

        private void debugEvent(DocumentEvent e) {
            System.out.append("<> ").println(e);
        }

    } // class TextComponentListener


}
