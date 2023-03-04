/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.text;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * A {@code ViewFactory} producing box views that know to paint their own
 * background.
 * <p>
 * {@link StyleConstants#Background Background} attribute may be set on the
 * element directly, but is not inherited from parent elements.  If not set
 * on the element it is resolved from the associated logical style.</p>
 */
public class BoxBackgroundFactory implements ViewFactory {

    private final ViewFactory viewFactory;

    /**
     * Constructs a new {@code BoxBackgroundViewFactory}.
     *
     * @param   viewFactory  original factory for producing views for all
     *          elements not directly supported by this factory.
     */
    public BoxBackgroundFactory(ViewFactory viewFactory) {
        this.viewFactory = viewFactory;
    }

    @Override
    public View create(Element elem) {
        String kind = elem.getName();
        // A dynamic proxy/subclass would be nice to handle any possibility.
        if (AbstractDocument.ParagraphElementName.equals(kind)) {
            return new ParagraphView(elem);
        } else if (AbstractDocument.SectionElementName.equals(kind)) {
            return new BoxView(elem);
        }
        return viewFactory.create(elem);
    }

    @SuppressWarnings("serial")
    public static StyledEditorKit newEditorKit() {
        return new StyledEditorKit() {
            private final BoxBackgroundFactory boxbgFactory =
                    new BoxBackgroundFactory(super.getViewFactory());
            @Override public ViewFactory getViewFactory() {
                return boxbgFactory;
            }
        };
    }

    static void paintBackground(View view, Graphics g, Shape a) {
        Color bg = null;
        AttributeSet atts = view.getAttributes();
        if (atts.isDefined(StyleConstants.Background)) {
            bg = (Color) atts.getAttribute(StyleConstants.Background);
        } else {
            AttributeSet parent = atts.getResolveParent();
            if (parent instanceof Style)
                bg = (Color) parent.getAttribute(StyleConstants.Background);
        }
        if (bg == null) return;

        Rectangle rect = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
        g.setColor(bg);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }


    private static class BoxView extends javax.swing.text.BoxView {

        BoxView(Element elem) {
            super(elem, View.Y_AXIS);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            paintBackground(this, g, a);
            super.paint(g, a);
        }

    }


    private static class ParagraphView extends javax.swing.text.ParagraphView {

        ParagraphView(Element elem) {
            super(elem);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            paintBackground(this, g, a);
            super.paint(g, a);
        }

    }


}
