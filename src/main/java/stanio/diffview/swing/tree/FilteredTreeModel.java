/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Provides filtered view of another {@code TreeModel}.
 */
public class FilteredTreeModel
        extends ProxyTreeModel<FilteredTreeModel.Node> {


    public static interface Node { //extends TreePathNode {

        Object getSourceNode();

    }


    private static final Predicate<TreePath> NO_FILTER = any -> true;

    private Predicate<TreePath> filter;

    private FilteredTreeNode filterBase;

    public FilteredTreeModel(TreeModel source) {
        super(source, FilteredTreeNode.rootFor(source));
        this.filter = NO_FILTER;
    }

    public void filter(Predicate<TreePath> filter) {
        this.filter = (filter == null) ? NO_FILTER : filter;
        FilteredTreeNode root = (FilteredTreeNode) getRoot();
        if (root != null) {
            filter(root);
            listeners.notify(TreeModelListener::treeStructureChanged,
                             () -> new TreeModelEvent(this, root.treePath));
        }
    }

    private void filter(FilteredTreeNode node) {
        //FilteredTreeNode node = (FilteredTreeNode) path.getLastPathComponent();
        if (node.children.isEmpty()) { // leaf, test path
            node.setVisiblePath(filter == NO_FILTER
                                || filter.test(node.sourcePath));
            return;
        }
        node.visible = (filter == NO_FILTER);
        node.children.forEach(this::filter);
    }

    //private static void setVisiblePath(TreePath leaf, boolean visible) {
    //    FilteredTreeNode node = (FilteredTreeNode) leaf.getLastPathComponent();
    //    node.visible = visible;
    //    if (!visible) return;
    //
    //    TreePath parent = leaf.getParentPath();
    //    while (parent != null) {
    //        node = (FilteredTreeNode) parent.getLastPathComponent();
    //        if (node.visible) break;
    //
    //        node.visible = true;
    //        parent = parent.getParentPath();
    //    }
    //}

    //private static TreePath getSourcePath(TreePath ourPath) {
    //    //Object[] path = ourPath.getPath();
    //    //for (int i = 0; i < path.length; i++) {
    //    //    path[i] = ((Node) path[i]).getSourceNode();
    //    //}
    //    //return new TreePath(path);
    //    return ((FilteredTreeNode) ourPath.getLastPathComponent()).sourcePath;
    //}

    public void clearFilter() {
        filter(NO_FILTER);
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((FilteredTreeNode) parent).visibleChild(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((FilteredTreeNode) parent).visibleCount();
    }

    @Override
    public boolean isLeaf(Object node) {
        FilteredTreeNode ourNode = (FilteredTreeNode) node;
        return ourNode.children.isEmpty()
                && source.isLeaf(ourNode.getSourceNode());
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        FilteredTreeNode ourNode =
                (FilteredTreeNode) path.getLastPathComponent();
        source.valueForPathChanged(ourNode.sourcePath, newValue);
        // sourceNodesChanged() would be notified, next
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((FilteredTreeNode) parent).visibleIndex((FilteredTreeNode) child);
    }

    @Override
    protected void sourceNodesChanged(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceNodesChanged",
                sourceEvent, this::scheduleFilter);
    }

    @Override
    protected void sourceNodesInserted(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceNodesInserted", sourceEvent, ourPath -> {
            FilteredTreeNode ourNode =
                    (FilteredTreeNode) ourPath.getLastPathComponent();
            int[] indices = sourceEvent.getChildIndices();
            Object[] sourceNodes = sourceEvent.getChildren();
            if (ourNode.children == Collections.EMPTY_LIST) {
                ourNode.children = new ArrayList<>(sourceNodes.length);
            } else {
                ((ArrayList<?>) ourNode.children)
                        .ensureCapacity(ourNode.children.size() + sourceNodes.length);
            }
            for (int i = 0; i < indices.length; i++) {
                ourNode.children.add(indices[i],
                        FilteredTreeNode.childOf(ourNode, source, sourceNodes[i]));
            }
            scheduleFilter(ourPath);
        });
    }

    @Override
    protected void sourceNodesRemoved(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceNodesRemoved", sourceEvent, ourPath -> {
            FilteredTreeNode ourNode =
                    (FilteredTreeNode) ourPath.getLastPathComponent();
            int[] indices = sourceEvent.getChildIndices();
            for (int i = indices.length - 1; i >= 0; i--) {
                ourNode.children.remove(indices[i]);
            }
            scheduleFilter(ourPath);
        });
    }

    @Override
    protected void sourceStructureChanged(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceStructureChanged", sourceEvent, ourPath -> {
            FilteredTreeNode ourNode =
                    (FilteredTreeNode) ourPath.getLastPathComponent();
            ourNode.loadChildren(source);
            scheduleFilter(ourPath);
        });
    }

    private void handleSourceEvent(String eventType,
                                   TreeModelEvent sourceEvent,
                                   Consumer<TreePath> handler) {
        Trees.traceEvent(this, eventType, sourceEvent);

        Trees.traceEvent(this, eventType, sourceEvent);

        TreePath sourcePath = sourceEvent.getTreePath();
        if (sourcePath == null) {
            setRoot(null); // Assume sourceStructureChanged
            return;
        }

        TreePath ourPath = findOurPath(sourceEvent.getTreePath());
        if (ourPath == null) {
            // REVISIT: Find common ancestor, if any, and reload that;
            // Otherwise replace root.
            if (getRoot() != null) {
                Trees.debug("%s: Source event for path not found in this tree:%n\t%s -> %s",
                        this, sourceEvent.getSource(), sourceEvent.getTreePath());
            }
            setRoot(FilteredTreeNode.rootFor(source));
        } else {
            handler.accept(ourPath);
            scheduleFilter(ourPath);
        }
    }

    private void scheduleFilter(TreePath path) {
        if (filterBase == null) {
            filterBase = (FilteredTreeNode) path.getLastPathComponent();
            SwingUtilities.invokeLater(() -> {
                try {
                    filter(filterBase);
                    listeners.notify(TreeModelListener::treeStructureChanged,
                                     () -> new TreeModelEvent(this, filterBase.treePath));
                } finally {
                    filterBase = null;
                }
            });
            return;
        }
        // Find common ancestor
        //Object[] path1 = filterBase.getPath();
        //Object[] path2 = path.getPath();
        //int i = 0;
        //while (i < path1.length && i < path2.length) {
        //    if (path1[i] != path2[i]) {
        //        break;
        //    }
        //    i++;
        //}
        //filterBase = (i == 0) ? (FilteredTreeNode) getRoot()
        //                      : new TreePath(path2);
        filterBase = (FilteredTreeNode) getRoot();
    }

    private TreePath findOurPath(TreePath sourcePath) {
        return findOurPath(sourcePath.getPath(), 0, new TreePath(getRoot()));
    }

    private static TreePath findOurPath(Object[] sourcePath,
                                        int index, TreePath currentPath) {
        FilteredTreeNode currentNode =
                (FilteredTreeNode) currentPath.getLastPathComponent();
        if (sourcePath[index] != currentNode.getSourceNode())
            return null;

        int nextIndex = index + 1;
        if (nextIndex >= sourcePath.length)
            return currentPath;

        for (FilteredTreeNode child : currentNode.children) {
            TreePath found = findOurPath(sourcePath,
                    //nextIndex, currentPath.pathByAddingChild(child));
                    nextIndex, child.treePath);
            if (found != null)
                return found;
        }
        return null;
    }


} // class FilteredTreeModel
