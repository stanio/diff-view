/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import javax.swing.tree.TreePath;

/**
 * A tree node that provides a {@code TreePath} to itself.
 */
public interface TreePathNode {

    /**
     * The tree path to this node.  The last path component is this node.
     *
     * @return  The tree path to this node.
     */
    TreePath getTreePath();

}
