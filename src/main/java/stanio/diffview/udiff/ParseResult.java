/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

public interface ParseResult {


    enum Type {
        MESSAGE,
        INDEX,
        DIFF_CMD,
        FROM_FILE,
        TO_FILE,
        HUNK,
        CONTEXT,
        ADDED,
        REMOVED,
        NONL_ATEOF
    }


    Type getType();

    int getFromLine();
    int getToLine();

    int getTermStart();
    int getTermEnd();

}