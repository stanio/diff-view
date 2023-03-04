/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import java.lang.ref.WeakReference;
import java.util.Objects;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Builds upon another {@code TreeModel}.
 */
public abstract class ProxyTreeModel<T extends Object> implements TreeModel {

    protected final TreeModel source;

    protected TreeModelListeners listeners = new TreeModelListeners();

    private T root;

    protected ProxyTreeModel(TreeModel source) {
        this.source = Objects.requireNonNull(source, "null source model");
        SourceModelListener.register(this);
    }

    protected ProxyTreeModel(TreeModel source, T root) {
        this(source);
        this.root = root;
    }

    public TreeModel getSource() {
        return source;
    }

    @Override
    public T getRoot() {
        return root;
    }

    protected void setRoot(T root) {
        if (root != this.root) {
            this.root = root;

            // Couldn't find it specified by TreeModelListener.treeStructureChanged(),
            // TreeModelEvent(source, path), and/or TreeModelEvent.getTreePath()
            // but DefaultTreeModel.setRoot(null) fires treeStructureChanged with
            // null treePath.
            TreePath path = (root == null) ? null : new TreePath(root);
            listeners.notify(TreeModelListener::treeStructureChanged,
                             () -> new TreeModelEvent(this, path));
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Handles {@code treeNodesChanged} from the source model.
     *
     * @see  TreeModelListener#treeNodesChanged(TreeModelEvent)
     */
    protected abstract void sourceNodesChanged(TreeModelEvent sourceEvent);

    /**
     * Handles {@code treeNodesInserted} from the source model.
     *
     * @see  TreeModelListener#treeNodesInserted(TreeModelEvent)
     */
    protected abstract void sourceNodesInserted(TreeModelEvent sourceEvent);

    /**
     * Handles {@code treeNodesRemoved} from the source model.
     *
     * @see  TreeModelListener#treeNodesRemoved(TreeModelEvent)
     */
    protected abstract void sourceNodesRemoved(TreeModelEvent sourceEvent);

    /**
     * Handles {@code treeStructureChanged} from the source model.
     *
     * @see  TreeModelListener#treeStructureChanged(TreeModelEvent)
     */
    protected abstract void sourceStructureChanged(TreeModelEvent sourceEvent);


    private static class SourceModelListener implements TreeModelListener {

        // Prevent the model being strongly reachable via the listener.
        private final WeakReference<ProxyTreeModel<?>> owner;

        private final TreeModel source;

        private SourceModelListener(ProxyTreeModel<?> model) {
            owner = new WeakReference<>(model);
            source = model.source;
        }

        static void register(ProxyTreeModel<?> model) {
            SourceModelListener listener = new SourceModelListener(model);
            listener.source.addTreeModelListener(listener);
        }

        /*
         * Unregisters this listener automatically whenever the associated
         * ProxyTreeModel is garbage-collected.
         */
        private ProxyTreeModel<?> model() {
            ProxyTreeModel<?> model = owner.get();
            if (model == null) {
                source.removeTreeModelListener(this);
            }
            return model;
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            ProxyTreeModel<?> model = model();
            if (model != null) model.sourceNodesChanged(e);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            ProxyTreeModel<?> model = model();
            if (model != null) model.sourceNodesInserted(e);
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            ProxyTreeModel<?> model = model();
            if (model != null) model.sourceNodesRemoved(e);
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            ProxyTreeModel<?> model = model();
            if (model != null) model.sourceStructureChanged(e);
        }

    } // class SourceModelListener


} // class ProxyTreeModel
