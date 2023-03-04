/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

class FilteredTreeNode implements FilteredTreeModel.Node {

    final TreePath treePath;

    final TreePath sourcePath;

    //final Object sourceNode;

    List<FilteredTreeNode> children;

    boolean visible = true;

    private FilteredTreeNode(Object sourceNode) {
        this.treePath = new TreePath(this);
        this.sourcePath = new TreePath(sourceNode);
        //this.sourceNode = sourceNode;
    }

    private FilteredTreeNode(FilteredTreeNode parent, Object sourceNode) {
        this.treePath = parent.treePath.pathByAddingChild(this);
        this.sourcePath = parent.sourcePath.pathByAddingChild(sourceNode);
    }

    static FilteredTreeNode rootFor(TreeModel sourceModel) {
        Object root = sourceModel.getRoot();
        if (root == null) {
            return null;
        }
        FilteredTreeNode ourRoot = new FilteredTreeNode(root);
        ourRoot.loadChildren(sourceModel);
        return ourRoot;
    }

    static FilteredTreeNode childOf(FilteredTreeNode parent,
                                    TreeModel sourceModel, Object sourceNode) {
        FilteredTreeNode ourChild = new FilteredTreeNode(parent,
                Objects.requireNonNull(sourceNode, "null tree node"));
        ourChild.loadChildren(sourceModel);
        return ourChild;
    }

    void loadChildren(TreeModel sourceModel) {
        Object sourceNode = getSourceNode();
        int count = sourceModel.getChildCount(sourceNode);

        if (children == null) {
            children = (count == 0) ? Collections.emptyList()
                                    : new ArrayList<>(count);
        }
        if (count > 0 && children == Collections.EMPTY_LIST) {
            children = new ArrayList<>(count);
        } else if (children instanceof ArrayList) {
            ((ArrayList<?>) children).ensureCapacity(count);
        }
        children.clear();

        for (int i = 0; i < count; i++) {
            children.add(childOf(this,
                    sourceModel, sourceModel.getChild(sourceNode, i)));
        }
    }

    //@Override
    //public TreePath getTreePath() {
    //    return treePath;
    //}

    @Override
    public Object getSourceNode() {
        return sourcePath.getLastPathComponent();
    }

    private FilteredTreeNode getParent() {
        TreePath parentPath = treePath.getParentPath();
        return parentPath != null
                ? (FilteredTreeNode) parentPath.getLastPathComponent()
                : null;
    }

    //TreePath getSourcePath() {
    //    //Object[] nodes = treePath.getPath();
    //    //for (int i = 0, len = nodes.length; i < len; i++) {
    //    //    nodes[i] = ((FilteredTreeNode) nodes[i]).getSourceNode();
    //    //}
    //    //return new TreePath(nodes);
    //    return sourcePath;
    //}

    void setVisiblePath(boolean visible) {
        this.visible = visible;
        if (!visible) return;

        FilteredTreeNode parent = getParent();
        while (parent != null && !parent.visible) {
            parent.visible = true;
            parent = parent.getParent();
        }
    }

    int visibleCount() {
        List<FilteredTreeNode> childList = children;
        if (childList == null) return 0;

        int count = 0;
        for (int i = 0, len = childList.size(); i < len; i++) {
            if (childList.get(i).visible) count++;
        }
        return count;
    }

    FilteredTreeNode visibleChild(int index) {
        List<FilteredTreeNode> childList = children;
        for (int i = 0, len = childList.size(), visible = 0; i < len; i++) {
            FilteredTreeNode item = childList.get(i);
            if (item.visible && index == visible++)
                return item;
        }
        throw new NoSuchElementException("index " + index);
    }

    int visibleIndex(FilteredTreeNode node) {
        List<FilteredTreeNode> childList = children;
        for (int i = 0, len = childList.size(), visible = -1; i < len; i++) {
            FilteredTreeNode item = childList.get(i);
            if (item.visible) {
                visible++;
            }
            if (item == node) {
                return visible;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return getSourceNode().toString();
    }

} // class FilteredTreeNode