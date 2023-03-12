/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see  <a href="https://www.gnu.org/software/diffutils/manual/html_node/Unified-Format.html"
 *              >Unified Format</a> <i>(GNU Diffutils)</i>
 * @see  <a href="https://www.gnu.org/software/diffutils/manual/html_node/Multiple-Patches.html"
 *              >Multiple Patches in a File</a> <i>(GNU Diffutils)</i>
 */
class UDiffParser implements ParseResult {

    private final Matcher indexLine = Pattern.compile("^Index: .+").matcher("");
    private final Matcher diffCmd   = Pattern.compile("^diff ").matcher("");
    private final Matcher fromFile  = Pattern.compile("^--- [^\\x00-\\x1F}]+").matcher("");
    private final Matcher toFile    = Pattern.compile("^\\+\\+\\+ [^\\x00-\\x1F}]+").matcher("");
    private final Matcher hunk      = Pattern.compile("^@@ -([0-9]+)(?:,([0-9]+))? \\+([0-9]+)(?:,([0-9]+))? @@").matcher("");
    private final Matcher removed   = Pattern.compile("^-").matcher("");
    private final Matcher added     = Pattern.compile("^\\+").matcher("");

    private Type type = Type.MESSAGE;

    private int fromLine;
    private int toLine;

    private int termStart;
    private int termEnd;

    private int fromRemaining = 0;
    private int toRemaining = 0;

    private String text;

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int getFromLine() {
        return fromLine;
    }

    @Override
    public int getToLine() {
        return toLine;
    }

    @Override
    public int getTermStart() {
        return termStart;
    }

    @Override
    public int getTermEnd() {
        return termEnd;
    }

    private final boolean find(Matcher m) {
        return m.reset(text).find();
    }

    public void update(CharSequence line) {
        text = line.toString();
        termStart = 0;
        termEnd = line.length();

        if (find(hunk)) {
            fromLine = Integer.parseInt(hunk.group(1)) - 1;
            String fromLength = hunk.group(2);
            fromRemaining = fromLength == null ? 1 : Integer.parseInt(fromLength);
            toLine = Integer.parseInt(hunk.group(3)) - 1;
            String toLength = hunk.group(4);
            toRemaining = toLength == null ? 1 : Integer.parseInt(toLength);
            type = Type.HUNK;
            termEnd = hunk.end();
        } else if (type == Type.MESSAGE) {
            if (find(diffCmd)) {
                type = Type.DIFF_CMD;
            } else if (find(indexLine)) {
                type = Type.INDEX;
                termStart = 7;
                termEnd = indexLine.end();
            } else if (find(fromFile)) {
                type = Type.FROM_FILE;
                termStart = 4;
                termEnd = fromFile.end();
            }
        } else if (type == Type.DIFF_CMD || type == Type.INDEX) {
            if (find(fromFile)) {
                type = Type.FROM_FILE;
                termStart = 4;
            } else {
                type = Type.MESSAGE;
            }
        } else if (type == Type.FROM_FILE) {
            if (find(toFile)) {
                type = Type.TO_FILE;
                termStart = 4;
                termEnd = toFile.end();
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
        } else if (find(removed)) {
            fromLine++;
            fromRemaining--;
            type = Type.REMOVED;
            termStart = 1;
        } else if (find(added)) {
            toLine++;
            toRemaining--;
            type = Type.ADDED;
            termStart = 1;
        } else if (find(indexLine)) {
            type = Type.INDEX;
            termStart = 7;
            fromLine = 0;
            toLine = 0;
        } else if (find(diffCmd)) {
            type = Type.DIFF_CMD;
            fromLine = 0;
            toLine = 0;
        } else if (find(fromFile)) {
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

    public ParseResult toParseResult() {
        return new Result(type, fromLine, toLine, termStart, termEnd);
    }

    public void reset() {
        type = Type.MESSAGE;
    }

    @Override
    public String toString() {
        return "UDiffParser(type: " + type
                + ", fromLine: " + fromLine
                + ", fromRemaining: " + fromRemaining
                + ", toLine: " + toLine
                + ", toRemaining: " + toRemaining + ")";
    }


    private static class Result implements ParseResult {

        private final Type type;
        private final int fromLine;
        private final int toLine;
        private final int termStart;
        private final int termEnd;

        Result(Type type,
                int fromLine,
                int toLine,
                int termStart,
                int termEnd) {
            this.type = type;
            this.fromLine = fromLine;
            this.toLine = toLine;
            this.termStart = termStart;
            this.termEnd = termEnd;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int getFromLine() {
            return fromLine;
        }

        @Override
        public int getToLine() {
            return toLine;
        }

        @Override
        public int getTermStart() {
            return termStart;
        }

        @Override
        public int getTermEnd() {
            return termEnd;
        }

    } // class Result


} // class UDiffParser
