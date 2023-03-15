/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import static stanio.diffview.swing.tree.Trees.getRootPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Not thread-safe.  Should be accessed only on the EDT.
 */
class PathFoldingTreeNode implements PathFoldingTreeModel.Node {

    private static final
    List<PathFoldingTreeNode> EMPTY_CHILDREN = Collections.emptyList();

    private TreePath foldedPath;

    private List<PathFoldingTreeNode> children;

    private String label;

    private PathFoldingTreeNode(TreePath folded) {
        this.foldedPath = folded;
    }

    static PathFoldingTreeNode rootFor(TreeModel sourceModel) {
        Object root = sourceModel.getRoot();
        if (root == null) {
            return null;
        }
        PathFoldingTreeNode ourRoot = new PathFoldingTreeNode(new TreePath(root));
        ourRoot.reloadChildren(sourceModel); // load eagerly
        return ourRoot;
    }

    static PathFoldingTreeNode nodeFor(TreeModel sourceModel, Object sourceNode) {
        PathFoldingTreeNode ourNode = new PathFoldingTreeNode(
                getFolded(sourceModel, Objects.requireNonNull(sourceNode, "null tree node")));
        ourNode.reloadChildren(sourceModel); // load eagerly
        return ourNode;
    }

    static TreePath getFolded(TreeModel model, Object node) {
        return getFolded(model, new TreePath(node));
    }

    static TreePath getFolded(TreeModel model, TreePath path) {
        int count = model.getChildCount(path.getLastPathComponent());
        while (count == 1) {
            Object child = model.getChild(path.getLastPathComponent(), 0);
            if (model.isLeaf(child))
                break;

            path = path.pathByAddingChild(child);
            count = model.getChildCount(child);
        }
        return path;
    }

    @Override
    public TreePath getFoldedPath() {
        return foldedPath;
    }

    /**
     * {@code foldedPath.lastPathComponent}
     */
    Object getSourceNode() {
        return getFoldedPath().getLastPathComponent();
    }

    List<PathFoldingTreeNode> getChildren() {
        return children;
    }

    void reloadChildren(TreeModel sourceModel) {
        Object sourceNode = getSourceNode();
        int count = sourceModel.getChildCount(sourceNode);
        if (count == 0) {
            children = EMPTY_CHILDREN;
            return;
        }

        List<PathFoldingTreeNode> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(nodeFor(sourceModel, sourceModel.getChild(sourceNode, i)));
        }
        children = list;
    }

    void reload(TreeModel sourceModel) {
        foldedPath = getFolded(sourceModel, getRootPath(getFoldedPath()));
        invalidateLabel().reloadChildren(sourceModel);
    }

    PathFoldingTreeNode invalidateLabel() {
        label = null;
        return this;
    }

    @Override
    public String toString() {
        if (label == null) {
            label = toString(getFoldedPath());
        }
        return label;
    }

    private static String toString(TreePath path) {
        if (path.getParentPath() == null) { // length == 1
            return path.getLastPathComponent().toString();
        }

        List<Object> components = new ArrayList<>();
        for ( ; path != null; path = path.getParentPath()) {
            components.add(path.getLastPathComponent());
        }

        final char spearator = '/';
        StringBuilder buf = new StringBuilder(components.size() * 10);
        for (int i = components.size() - 1; i >= 0; i--) {
            if (buf.length() > 0) {
                buf.append(spearator);
            }
            buf.append(components.get(i));
        }
        return buf.toString();
    }

}
