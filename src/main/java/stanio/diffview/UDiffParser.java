/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see  <a href="https://www.gnu.org/software/diffutils/manual/html_node/Unified-Format.html"
 *              >Unified Format</a> <i>(GNU Diffutils)</i>
 */
class UDiffParser {

    enum Type { MESSAGE, DIFF_CMD, FROM_FILE, TO_FILE, FILE, HUNK, CONTEXT, ADDED, REMOVED }

    private final Matcher diffCmd  = Pattern.compile("^diff ").matcher("");
    private final Matcher fromFile = Pattern.compile("^--- (.+)").matcher("");
    private final Matcher toFile   = Pattern.compile("^\\+\\+\\+ (.+)").matcher("");
    private final Matcher hunk    = Pattern.compile("^@@ -([0-9]+)(?:,([0-9]+))? \\+([0-9]+)(?:,([0-9]+))? @@").matcher("");
    private final Matcher removed  = Pattern.compile("^-").matcher("");
    private final Matcher added    = Pattern.compile("^\\+").matcher("");

    Type type = Type.MESSAGE;

    int fromLine;
    int toLine;

    int termStart;
    int termEnd;

    private int fromRemaining = 0;
    private int toRemaining = 0;

    void update(CharSequence line) {
        termStart = 0;
        termEnd = line.length();
        if (hunk.reset(line).find()) {
            fromLine = Integer.parseInt(hunk.group(1)) - 1;
            String fl = hunk.group(2);
            fromRemaining = fl == null ? 1 : Integer.parseInt(fl);
            toLine = Integer.parseInt(hunk.group(3)) - 1;
            String sl = hunk.group(4);
            toRemaining = sl == null ? 1 : Integer.parseInt(sl);
            type = Type.HUNK;
            termEnd = hunk.end();
        } else if (type == Type.MESSAGE) {
            if (diffCmd.reset(line).find()) {
                type = Type.DIFF_CMD;
            } else if (fromFile.reset(line).find()) {
                type = Type.FROM_FILE;
                termStart = 4;
            }
        } else if (type == Type.DIFF_CMD) {
            if (fromFile.reset(line).find()) {
                type = Type.FROM_FILE;
                termStart = 4;
            } else {
                type = Type.MESSAGE;
            }
        } else if (type == Type.FROM_FILE) {
            if (toFile.reset(line).find()) {
                type = Type.TO_FILE;
                termStart = 4;
            } else {
                type = Type.MESSAGE;
            }
        } else if ((line.length() == 0 || line.charAt(0) == ' ')
                && (fromRemaining > 0 || toRemaining > 0)) {
            fromLine++;
            fromRemaining--;
            toLine++;
            toRemaining--;
            type = Type.CONTEXT;
            termStart = 1;
        } else if (removed.reset(line).find()) {
            fromLine++;
            fromRemaining--;
            type = Type.REMOVED;
            termStart = 1;
        } else if (added.reset(line).find()) {
            toLine++;
            toRemaining--;
            type = Type.ADDED;
            termStart = 1;
        } else if (diffCmd.reset(line).find()) {
            type = Type.DIFF_CMD;
            fromLine = 0;
            toLine = 0;
        } else if (fromFile.reset(line).find()) {
            type = Type.FROM_FILE;
            termStart = 4;
            fromLine = 0;
            toLine = 0;
        } else {
            fromLine = 0;
            toLine = 0;
            type = Type.MESSAGE;
        }
    }

    @Override
    public String toString() {
        return "UDiffParser(type: " + type
                + ", fromLine: " + fromLine
                + ", fromRemaining: " + fromRemaining
                + ", toLine: " + toLine
                + ", toRemaining: " + toRemaining + ")";
    }

}
