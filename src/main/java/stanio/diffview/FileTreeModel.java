/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import stanio.diffview.swing.tree.TreeModelListeners;

class FileTreeModel implements TreeModel {


    static class Node {

        final String name;

        List<Node> children = Collections.emptyList();

        Node(String name) {
            this.name = Objects.requireNonNull(name);
        }

        Node get(String child) {
            List<Node> list = children;
            for (int i = 0, len = list.size(); i < len; i++) {
                Node item = list.get(i);
                if (item.name.equals(child)) {
                    return item;
                }
            }
            return null;
        }

        void add(Node child) {
            if (children.isEmpty()) {
                children = new ArrayList<>(1);
            }
            children.add(child);
        }

        @Override
        public String toString() {
            return name;
        }

    } // class Node


    private final Node root;

    private TreeModelListeners listeners = new TreeModelListeners();

    private boolean loading;

    public FileTreeModel() {
        this("<root>");
    }

    public FileTreeModel(String name) {
        root = new Node(name);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((Node) parent).children.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((Node) parent).children.size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((Node) node).children.isEmpty();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // No user editing allowed.
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((Node) parent).children.indexOf(child);
    }

    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        listeners.remove(listener);
    }

    public void addPath(String path) {
        addPath(parsePath(path));
    }

    protected String[] parsePath(String path) {
        return path.split("/");
    }

    public void addPath(String[] path) {
        Node current = root;
        TreePath treePath = new TreePath(current);
        for (int i = 0, len = path.length; i < len; i++) {
            String name = path[i];
            if (name.trim().isEmpty()) continue;

            Node child = current.get(name);
            if (child == null) {
                Node newChild = new Node(name);
                current.add(newChild);
                if (!loading) {
                    TreePath parentPath = treePath;
                    Node parentNode = current;
                    listeners.notify(TreeModelListener::treeNodesInserted,
                            () -> new TreeModelEvent(this, parentPath,
                                    new int[] { parentNode.children.size() - 1 },
                                    new Object[] { newChild }));
                }
                child = newChild;
            }
            treePath = treePath.pathByAddingChild(child);
            current = child;
        }
    }

    public void startLoading() {
        loading = true;
    }

    public void doneLoading() {
        if (loading) {
            loading = false;
            listeners.notify(TreeModelListener::treeStructureChanged,
                    () -> new TreeModelEvent(this, new TreePath(root)));
        }
    }

}
