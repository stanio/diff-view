/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Point2D;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import stanio.diffview.swing.LineRuler;
import stanio.diffview.swing.NowrapTextPane;
import stanio.diffview.swing.text.BoxBackgroundFactory;
import stanio.diffview.udiff.UDiffEditorKit;

/**
 * Shows alternatively the unified or split (side-by-side) views/components.
 */
@SuppressWarnings("serial")
class DiffCardPane extends JPanel {

    public static final String CARD_UNIFIED = "unified";
    public static final String CARD_SPLIT = "split";

    DiffTextPane unifiedPane;

    FixedSplitPane<JScrollPane, JScrollPane> splitPane;

    private boolean splitPaneInitialized;

    public DiffCardPane() {
        super();
        unifiedPane = new DiffTextPane();
        splitPane = new FixedSplitPane<>();
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent event) {
                initSplitPane();
            }
        });
        initUI();
    }

    private void initUI() {
        if (unifiedPane == null) {
            return;
        }
        super.setLayout(new CardLayout());
        super.removeAll();
        super.add(unifiedPane, CARD_UNIFIED);
        super.add(splitPane, CARD_SPLIT);
    }

    /**
     * Lazy initialization of the split pane using data from the unified pane.
     */
    private void initSplitPane() {
        if (splitPaneInitialized) return;

        JScrollPane left = setUpSplit(false);
        JScrollPane right = setUpSplit(true);

        left.getViewport().addChangeListener(event -> {
            Point2D scrollPosition = getScrollPosition((JComponent)
                    left.getViewport().getView());
            scrollToPosition((JComponent)
                    right.getViewport().getView(), scrollPosition);
        });
        splitPane.setLeftComponent(left);
        splitPane.setRightComponent(right);
        //splitPane.setGapSize(1);
        splitPane.validate();
        splitPaneInitialized = true;
    }

    private JScrollPane setUpSplit(boolean added) {
        JTextPane sourceText = unifiedPane.diffPane;
        JTextPane textPane = new NowrapTextPane();
        textPane.setEditorKit(new UDiffEditorKit());
        textPane.setDocument(sourceText.getStyledDocument());
        textPane.setEditable(false);
        textPane.setFont(sourceText.getFont());
        textPane.setBackground(sourceText.getBackground());
        textPane.setForeground(sourceText.getForeground());
        textPane.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                textPane.getCaret().setVisible(true);
            }
        });

        JScrollPane left = new JScrollPane(textPane,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JTextPane ruler = new LineRuler(textPane);
        ruler.setEditorKit(BoxBackgroundFactory.newEditorKit());
        ruler.setFont(textPane.getFont());
        ruler.putClientProperty("JComponent.minimumWidth", 0);
        ruler.setDocument(added ? unifiedPane.toRuler.getDocument()
                                : unifiedPane.fromRuler.getDocument());
        //DiffStyles.addTo(ruler.getStyledDocument());
        left.setRowHeaderView(ruler);
        return left;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        initUI();
    }

    private static ThreadLocal<Rectangle> visibleRect = new ThreadLocal<>() {
        @Override protected Rectangle initialValue() { return new Rectangle(); }
    };
    private static ThreadLocal<Point2D> scrollPosition = new ThreadLocal<>() {
        @Override protected Point2D initialValue() { return new Point2D.Double(); }
    };

    static Point2D getScrollPosition(JComponent comp) {
        int width = comp.getWidth();
        int height = comp.getHeight();
        Rectangle rect = visibleRect.get();
        comp.computeVisibleRect(rect);
        Point2D position = scrollPosition.get();
        position.setLocation((double) rect.x / (width - rect.width),
                             (double) rect.y / (height - rect.height));
        return position;
    }

    static void scrollToPosition(JComponent comp, Point2D position) {
        int width = comp.getWidth();
        int height = comp.getHeight();
        Rectangle rect = visibleRect.get();
        comp.computeVisibleRect(rect);
        rect.setLocation((int) Math.round(position.getX() * (width - rect.width)),
                         (int) Math.round(position.getY() * (height - rect.height)));
        comp.scrollRectToVisible(rect);
    }

    public void showSplit(boolean split) {
        if (split) {
            initSplitPane();
            // TODO: Rulers should have separate documents and this should be
            // set up just in setUpSplit()
            StyledDocument leftRuler = unifiedPane.fromRuler.getStyledDocument();
            Style defaultStyle = leftRuler.getStyle(StyleContext.DEFAULT_STYLE);
            final float inset = 2f;
            StyleConstants.setRightIndent(defaultStyle, inset);

            StyledDocument rightRuler = unifiedPane.toRuler.getStyledDocument();
            defaultStyle = rightRuler.getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setLeftIndent(defaultStyle, inset);
        } else {
            StyledDocument leftRuler = unifiedPane.fromRuler.getStyledDocument();
            Style defaultStyle = leftRuler.getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setRightIndent(defaultStyle, 0);

            StyledDocument rightRuler = unifiedPane.toRuler.getStyledDocument();
            defaultStyle = rightRuler.getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setLeftIndent(defaultStyle,
                    getFontMetrics(splitPane.leftComponent.getFont()).charWidth('0'));
        }

        LayoutManager layout = getLayout();
        if (layout instanceof CardLayout) {
            ((CardLayout) layout).show(this, split ? CARD_SPLIT : CARD_UNIFIED);
        }
    }

    public String getVisibleComponent() {
        return splitPane.isVisible() ? CARD_SPLIT : CARD_UNIFIED;
    }

    @Override
    public boolean requestFocusInWindow() {
        Component comp = CARD_SPLIT.equals(getVisibleComponent())
                         ? splitPane.leftComponent.getViewport().getView()
                         : unifiedPane.diffPane;
        return comp.requestFocusInWindow();
    }

}
