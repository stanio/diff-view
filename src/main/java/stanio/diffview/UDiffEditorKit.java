/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

import stanio.diffview.swing.text.BoxBackgroundFactory;

@SuppressWarnings("serial")
class UDiffEditorKit extends StyledEditorKit {

    private final ViewFactory viewFactory;

    UDiffEditorKit() {
        this.viewFactory = new BoxBackgroundFactory(super.getViewFactory());
    }

    @Override
    public ViewFactory getViewFactory() {
        return viewFactory;
    }

}
