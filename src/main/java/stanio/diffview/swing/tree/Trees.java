/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public final class Trees {

    static final boolean DEBUG = true;
    static final boolean TRACE = false;

    //public static TreePath concat(TreePath path1, TreePath path2) {
    //    TreePath parent = path2.getParentPath();
    //    if (parent != null) {
    //        path1 = concat(path1, parent);
    //    }
    //    return path1.pathByAddingChild(path2.getLastPathComponent());
    //}

    public static void expandAll(JTree tree) {
        expandTree(tree, Integer.MAX_VALUE);
    }

    public static void expandAll(JTree tree, TreePath path) {
        expandTree(tree, path, Integer.MAX_VALUE);
    }

    public static void expandTree(JTree tree, int depth) {
        TreeModel model = tree.getModel();
        if (model == null) return;

        Object root = model.getRoot();
        if (root == null) return;

        expandTree(tree,
                root instanceof TreePathNode
                ? ((TreePathNode) root).getTreePath()
                : new TreePath(root),
                depth);
    }

    public static void expandTree(JTree tree, TreePath path, int depth) {
        if (depth <= 0) return;

        tree.expandPath(path);
        expandChildren(tree, path, depth - 1);
    }

    static void expandChildren(JTree tree, TreePath path, int depth) {
        if (depth <= 0) return;

        TreeModel model = tree.getModel();
        int count = model.getChildCount(path.getLastPathComponent());
        for (int i = 0; i < count; i++) {
            expandTree(tree, childPath(path, model
                    .getChild(path.getLastPathComponent(), i)), depth);
        }
    }

    private static TreePath childPath(TreePath parent, Object child) {
        if (child instanceof TreePathNode) {
            return ((TreePathNode) child).getTreePath();
        }
        return parent.pathByAddingChild(child);
    }

    /**
     * Automatically expand branches of newly inserted nodes and nodes having
     * structure changed.
     *
     * @param   tree  the tree to keep expanded
     * @see     TreeModelListener#treeNodesInserted(TreeModelEvent)
     * @see     TreeModelListener#treeStructureChanged(TreeModelEvent)
     */
    public static void keepExpanded(JTree tree) {
        KeepExpandedListener.register(tree);
    }

    public static TreePath getRootPath(TreePath path) {
        TreePath parent = path.getParentPath();
        while (parent != null) {
            path = parent;
            parent = path.getParentPath();
        }
        return path;
    }

    public static Object getRootComponent(TreePath path) {
        return path.getPathComponent(0);
    }

    public static TreePath subPath(TreePath path, int begin) {
        TreePath subPath = new TreePath(path.getPathComponent(begin));
        for (int i = begin + 1, len = path.getPathCount(); i < len; i++) {
            subPath = subPath.pathByAddingChild(path.getPathComponent(i));
        }
        return subPath;
    }

    static void debug(String message, Object... args) {
        if (DEBUG) {
            System.err.append("[DEBUG] ").printf(message, args);
            System.err.println();
            if (TRACE) {
                StackWalker.getInstance().forEach(frame ->
                        System.err.append("\tat ").println(frame));
            }
        }
    }

    static void traceEvent(Object receiver, String eventType, TreeModelEvent event) {
        if (TRACE) {
            System.err.append("[TRACE] ")
                    .append(receiver.getClass().getSimpleName()).append('@')
                    .append(Integer.toHexString(System.identityHashCode(receiver)))
                    .append('.').append(eventType).append(": ")
                    .append(event.getSource().getClass().getSimpleName()).append('@')
                    .append(Integer.toHexString(System.identityHashCode(event.getSource())))
                    .append(' ').println(event);
        }
    }

    private Trees() {/* no instances */}


    private static class KeepExpandedListener
            implements TreeModelListener, PropertyChangeListener {

        private final JTree tree;

        private KeepExpandedListener(JTree tree) {
            this.tree = tree;
        }

        static void register(JTree tree) {
            KeepExpandedListener listener = new KeepExpandedListener(tree);
            tree.addPropertyChangeListener("model", listener);

            TreeModel model = tree.getModel();
            if (model != null) {
                model.addTreeModelListener(listener);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            Object oldModel = event.getOldValue();
            if (oldModel instanceof TreeModel) {
                ((TreeModel) oldModel).removeTreeModelListener(this);
            }

            Object newModel = event.getNewValue();
            if (newModel instanceof TreeModel) {
                ((TreeModel) newModel).addTreeModelListener(this);
                //expandAll(tree);
            }
        }

        @Override
        public void treeNodesChanged(TreeModelEvent event) {
            // do nothing
        }

        @Override
        public void treeNodesInserted(TreeModelEvent event) {
            TreePath path = event.getTreePath();
            if (path == null) return;

            for (Object child : event.getChildren()) {
                expandAll(tree, path.pathByAddingChild(child));
            }
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent event) {
            // do nothing
        }

        /**
         * {@link TreeModelEvent#TreeModelEvent(Object, Object[])}:
         * <blockquote>
         *   <b>Note:</b><br>
         *   JTree collapses all nodes under the specified node, so that only
         *   its immediate children are visible.
         * </blockquote>
         */
        @Override
        public void treeStructureChanged(TreeModelEvent event) {
            TreePath path = event.getTreePath();
            if (path == null) return;

            // Schedule the expansion later to ensure it happens after
            // the JTree collapses the children.
            SwingUtilities.invokeLater(() ->
                    expandChildren(tree, path, Integer.MAX_VALUE));
        }

    } // class KeepExpandedListener


} // class Trees
