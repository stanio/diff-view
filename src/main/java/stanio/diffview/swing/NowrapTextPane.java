/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing;

import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.plaf.TextUI;
import javax.swing.text.StyledDocument;

/**
 * Text pane that prevents lines/paragraphs of text being wrapped (while in
 * a {@code JViewport}).  Line wrapping may be optionally turned on.
 *
 * @see  JEditorPane#getPreferredSize()
 */
@SuppressWarnings("serial")
public class NowrapTextPane extends JTextPane {

    private boolean wrapEnabled;

    public NowrapTextPane() {
        super();
    }

    public NowrapTextPane(StyledDocument doc) {
        super(doc);
    }

    public boolean isWrapEnabled() {
        return wrapEnabled;
    }

    public void setWrapEnabled(boolean wrapEnabled) {
        boolean oldValue = wrapEnabled;
        this.wrapEnabled = wrapEnabled;
        if (oldValue != wrapEnabled) {
            invalidate();
            firePropertyChange("wrapEnabled", oldValue, wrapEnabled);
        }
    }

    @Override public Dimension getPreferredSize() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        return (parent instanceof JViewport) && !isWrapEnabled()
                ? getUI().getPreferredSize(this)
                : super.getPreferredSize();
    }

    @Override public boolean getScrollableTracksViewportWidth() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        return (parent instanceof JViewport)
                && (parent.getWidth() > preferredScrollableWidth());
    }

    private int preferredScrollableWidth() {
        TextUI textUI = getUI();
        Dimension size = isWrapEnabled() ? textUI.getMinimumSize(this)
                                         : textUI.getPreferredSize(this);
        return size.width;
    }

    @Override public boolean getScrollableTracksViewportHeight() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        return (parent instanceof JViewport)
                && (parent.getHeight() > preferredScrollableHeight());
    }

    private int preferredScrollableHeight() {
        TextUI textUI = getUI();
        Dimension size = isWrapEnabled() ? textUI.getMinimumSize(this)
                                         : textUI.getPreferredSize(this);
        return size.height;
    }

}
