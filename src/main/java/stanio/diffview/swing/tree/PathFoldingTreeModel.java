/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Provides a "folded-path" view of another {@code TreeModel}.
 * <p>
 * With the exception of the root node, non-leaf nodes fold single non-leaf
 * child recursively representing the folded path as a single node.</p>
 */
public class PathFoldingTreeModel
        extends ProxyTreeModel<PathFoldingTreeModel.Node> {


    /**
     * A node in a {@code PathFoldingTreeModel}.
     */
    public static interface Node {

        /**
         * Tree path of the source nodes folded into this node.  The path is
         * rooted at the first source node folded into this node.  If no nodes
         * are folded at this level, the path has length of 1.
         *
         * @return  Tree path of the source nodes folded into this node.
         */
        TreePath getFoldedPath();

    }


    public PathFoldingTreeModel(TreeModel source) {
        super(source, PathFoldingTreeNode.rootFor(source));
    }

    @Override
    public Node getChild(Object parent, int index) {
        return getChildAt((PathFoldingTreeNode) parent, index);
    }

    PathFoldingTreeNode getChildAt(PathFoldingTreeNode parent, int index) {
        return children(parent).get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return getChildCount((PathFoldingTreeNode) parent);
    }

    int getChildCount(PathFoldingTreeNode parent) {
        return children(parent).size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return source.isLeaf(((PathFoldingTreeNode) node).getSourceNode());
    }

    /**
     * @implNote  This implementation does nothing.  Editing folded tree
     * directly is not supported.
     */
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        if (Trees.DEBUG) {
            System.err.append("[WARN] Not editable: ")
                      .println(getClass().getName());
            StackWalker.getInstance().forEach(frame ->
                    System.err.append("\tat ").println(frame));
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) {
            return -1;
        }
        if (parent instanceof PathFoldingTreeNode
                && child instanceof PathFoldingTreeNode) {
            return getIndex((PathFoldingTreeNode) parent, child);
        }
        return -1;
    }

    int getIndex(PathFoldingTreeNode parent, Object child) {
        return children(parent).indexOf(child);
    }

    int getIndex(TreePath child) {
        TreePath parent = child.getParentPath();
        return getIndex((PathFoldingTreeNode) parent.getLastPathComponent(),
                        child.getLastPathComponent());
    }

    private List<PathFoldingTreeNode> children(PathFoldingTreeNode parent) {
        //return parent.children(source); // may load lazily
        return parent.getChildren();
    }

    @Override
    protected void sourceNodesChanged(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceNodesChanged", sourceEvent);
    }

    @Override
    protected void sourceNodesInserted(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceNodesInserted", sourceEvent);
    }

    @Override
    protected void sourceNodesRemoved(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceNodesRemoved", sourceEvent);
    }

    @Override
    protected void sourceStructureChanged(TreeModelEvent sourceEvent) {
        handleSourceEvent("sourceStructureChanged", sourceEvent);
    }

    private void handleSourceEvent(String eventType, TreeModelEvent sourceEvent) {
        Trees.traceEvent(this, eventType, sourceEvent);

        TreePath sourcePath = sourceEvent.getTreePath();
        if (sourcePath == null) {
            setRoot(null); // Assume sourceStructureChanged
            return;
        }

        TreePath ourPath = findOurPath(sourcePath);
        if (ourPath == null) {
            // REVISIT: Find common ancestor, if any, and reload that;
            // Otherwise replace root.
            if (sourcePath.getParentPath() != null) {
                Trees.debug("%s: Source event for path not found in this tree:%n\t%s -> %s",
                        this, sourceEvent.getSource(), sourceEvent.getTreePath());
            }
            setRoot(PathFoldingTreeNode.rootFor(source));
        } else {
            reload(ourPath);
        }
    }

    private void reload(TreePath ourPath) {
        PathFoldingTreeNode ourNode = (PathFoldingTreeNode) ourPath.getLastPathComponent();
        TreePath parentPath = ourPath.getParentPath();
        if (parentPath == null) {
            assert (ourNode == getRoot());
            // Don't fold into the root node.
            ourNode.invalidateLabel().reloadChildren(source);
            listeners.notify(TreeModelListener::treeNodesChanged,
                             () -> new TreeModelEvent(this, ourPath));
        } else {
            ourNode.reload(source);
            listeners.notify(TreeModelListener::treeNodesChanged,
                    () -> new TreeModelEvent(this, parentPath,
                                             new int[] { getIndex(ourPath) },
                                             new Object[] { ourNode }));
        }
        listeners.notify(TreeModelListener::treeStructureChanged,
                         () -> new TreeModelEvent(this, ourPath));
    }

    private TreePath findOurPath(TreePath sourcePath) {
        if (getRoot() == null) return null;

        return findOurPath(sourcePath.getPath(), 0, new TreePath(getRoot()));
    }

    private TreePath findOurPath(Object[] sourcePath,
            int beginIndex, TreePath currentPath) {
        PathFoldingTreeNode currentNode =
                (PathFoldingTreeNode) currentPath.getLastPathComponent();
        int nextIndex = prefixIndex(sourcePath, beginIndex,
                                    currentNode.getFoldedPath());
        if (nextIndex == 0) {
            return null;
        } else if (nextIndex < 0) {
            return currentPath;
        }

        for (PathFoldingTreeNode child : children(currentNode)) {
            TreePath found = findOurPath(sourcePath,
                    nextIndex, currentPath.pathByAddingChild(child));
            if (found != null) return found;
        }
        return null;
    }

    /*
     * > 0  first (sub-path) contains second, ends at index (+ begin)
     * = 0  disparate paths/branches
     * < 0  second contains first (sub-path), up to -index - 1
     */
    private static int prefixIndex(Object[] first, int begin, TreePath second) {
        int index = 0;
        int firstLength = first.length;
        if (begin >= firstLength) {
            return 0;
        }

        int firstPos = begin;
        int secondLength = second.getPathCount();
        while (firstPos < firstLength
                && index < secondLength) {
            if (!first[firstPos]
                    .equals(second.getPathComponent(index))) {
                return 0;
            }
            index++;
            firstPos++;
        }
        return firstPos >= firstLength
                ? -(secondLength - index) - 1
                : firstPos;
    }

} // class PathFoldingTreeModel
