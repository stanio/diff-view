/*
 * 2023 <license>
 */
package stanio.diffview;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/**
 * Simple split pane.  Allocates equal width to the left and right components,
 * no resizing.
 *
 * @param  L  the type of the left component
 * @param  R  the type of the right component
 *
 * @see  javax.swing.JSplitPane
 */
@SuppressWarnings("serial")
class SimpleSplitPane<L extends Component, R extends Component> extends JPanel {

    L leftComponent;
    R rightComponent;

    int gapSize;

    public SimpleSplitPane() {
        //super();
    }

    private void initUI() {
        super.setLayout(new GridLayout(1, 0, gapSize, gapSize));
        updateLayout();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        initUI();
    }

    public void setLeftComponent(L component) {
        this.leftComponent = component;
        updateLayout();
    }

    public void setRightComponent(R component) {
        this.rightComponent = component;
        updateLayout();
    }

    public void setGapSize(int gapSize) {
        this.gapSize = gapSize;

        LayoutManager layout = getLayout();
        if (layout instanceof GridLayout) {
            ((GridLayout) layout).setHgap(gapSize);
            ((GridLayout) layout).setVgap(gapSize);
        }
    }

    private void updateLayout() {
        super.removeAll();
        if (leftComponent == null) return;

        super.add(leftComponent);
        if (rightComponent == null) return;

        super.add(rightComponent);
    }

}
