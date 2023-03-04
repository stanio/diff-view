/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.util.EnumMap;
import java.util.Map;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import stanio.diffview.DiffStyles.Name;
import stanio.diffview.UDiffParser.Type;
import stanio.diffview.swing.text.DocumentCharSequence;

class DiffLines {

    public static void initNumbersAndOutline(JTextComponent textComponent,
                                             JTextPane fromRuler, JTextPane toRuler,
                                             FileTreeModel fileTree) {
        StyledDocument source = (StyledDocument) textComponent.getDocument();
        StyledDocument fromNumbers = fromRuler.getStyledDocument();
        StyledDocument toNumbers = toRuler.getStyledDocument();

        parse(source, fromNumbers, toNumbers, fileTree);
    }

    private static Map<Type, String> rulerStyles;
    private static Map<Type, String> textStyles;
    static {
        rulerStyles = new EnumMap<>(Type.class);
        rulerStyles.put(Type.ADDED, Name.INSERTED_NUMBER);
        rulerStyles.put(Type.REMOVED, Name.DELETED_NUMBER);

        textStyles = new EnumMap<>(Type.class);
        textStyles.put(Type.MESSAGE, Name.MESSAGE);
        textStyles.put(Type.DIFF_CMD, Name.DIFF_COMMAND);
        textStyles.put(Type.FROM_FILE, Name.FROM_FILE);
        textStyles.put(Type.TO_FILE, Name.TO_FILE);
        textStyles.put(Type.HUNK, Name.HUNK);
        textStyles.put(Type.CONTEXT, StyleContext.DEFAULT_STYLE);
        textStyles.put(Type.REMOVED, Name.DELETED_LINE);
        textStyles.put(Type.ADDED, Name.INSERTED_LINE);
    }

    private static void parse(StyledDocument diffText,
                              StyledDocument fromRuler,
                              StyledDocument toRuler,
                              FileTreeModel fileTree) {
        fileTree.startLoading();
        try {
            parseUnsafe(diffText, fromRuler, toRuler, fileTree);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } finally {
            fileTree.doneLoading();
        }
    }

    private static void parseUnsafe(StyledDocument diffText,
                                    StyledDocument fromRuler,
                                    StyledDocument toRuler,
                                    FileTreeModel fileTree)
            throws BadLocationException {
        fromRuler.remove(0, fromRuler.getLength());
        toRuler.remove(0, toRuler.getLength());

        UDiffParser diffParser = new UDiffParser();

        Element last = null;
        Element paragraph = diffText.getParagraphElement(0);
        while (paragraph != last) {
            CharSequence line = getText(paragraph);
            diffParser.update(line);

            setLogicalStyle(paragraph, diffParser, textStyles);
            if (diffParser.type == Type.HUNK) {
                Style hunkLabel = diffText.getStyle(Name.HUNK_LABEL);
                int labelStart = paragraph.getStartOffset() + diffParser.termEnd;
                diffText.setCharacterAttributes(labelStart,
                        paragraph.getEndOffset() - labelStart, hunkLabel, true);
            } else if (diffParser.type == Type.FROM_FILE) {
                String file = line.subSequence(diffParser.termStart, diffParser.termEnd).toString();
                if (!file.startsWith("/dev/null")) {
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    a.addAttribute("file", file.replaceFirst("^a/", ""));
                    diffText.setParagraphAttributes(paragraph.getStartOffset(),
                            paragraph.getEndOffset() - paragraph.getStartOffset(),
                            a, false);
                }
            } else if (diffParser.type == Type.TO_FILE) {
                String file = line.subSequence(diffParser.termStart, diffParser.termEnd).toString();
                if (!file.startsWith("/dev/null")) {
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    a.addAttribute("file", file.replaceFirst("^b/", ""));
                    diffText.setParagraphAttributes(last.getStartOffset(),
                            last.getEndOffset() - last.getStartOffset(),
                            a, false);
                }
                fileTree.addPath((String) last
                        .getAttributes().getAttribute("file"));
            }

            setLogicalStyle(fromRuler, diffParser, rulerStyles);

            if (diffParser.fromLine > 0
                    && diffParser.type != Type.HUNK
                    && diffParser.type != Type.ADDED) {
                fromRuler.insertString(fromRuler.getLength(),
                        Integer.toString(diffParser.fromLine), null);
            }
            fromRuler.insertString(fromRuler.getLength(), "\n", null);

            setLogicalStyle(toRuler, diffParser, rulerStyles);

            int p = toRuler.getLength();
            if (diffParser.toLine > 0
                    && diffParser.type != Type.HUNK
                    && diffParser.type != Type.REMOVED) {
                toRuler.insertString(p,
                        Integer.toString(diffParser.toLine), null);
            }
            toRuler.insertString(toRuler.getLength(), "\n", null);

            last = paragraph;
            paragraph = diffText.getParagraphElement(last.getEndOffset());
        }
    }

    private static CharSequence getText(Element elem) {
        int end = Math.min(elem.getEndOffset(), elem.getDocument().getLength());
        return DocumentCharSequence.of(elem.getDocument(),
                elem.getStartOffset(), end - elem.getStartOffset());
    }

    private static void setLogicalStyle(StyledDocument doc,
            UDiffParser parseResult, Map<Type, String> availableStyles) {
        String name = availableStyles.get(parseResult.type);
        Style s = (name == null) ? null : doc.getStyle(name);
        if (s == null) s = doc.getStyle(StyleContext.DEFAULT_STYLE);
        doc.setLogicalStyle(doc.getLength(), s);
    }

    private static void setLogicalStyle(Element elem,
            UDiffParser parseResult, Map<Type, String> availableStyles) {
        StyledDocument doc = (StyledDocument) elem.getDocument();
        String name = availableStyles.get(parseResult.type);
        Style s = (name == null) ? null : doc.getStyle(name);
        if (s == null) s = doc.getStyle(StyleContext.DEFAULT_STYLE);
        doc.setLogicalStyle(elem.getStartOffset(), s);
    }


    //static class DiffHunk {
    //
    //    enum Type { HUNK, CONTEXT, ADDED, REMOVED, OUTSIDE }
    //
    //    // @@ -9,3 +8,6 @@
    //    private final Matcher start = Pattern.compile("^@@ -([0-9]+)(?:,([0-9]+))? \\+([0-9]+)(?:,([0-9]+))? @@").matcher("");
    //    private final Matcher removed = Pattern.compile("^(?!--- )-").matcher("");
    //    private final Matcher added   = Pattern.compile("^(?!\\+\\+\\+ )\\+").matcher("");
    //
    //    int first;
    //    int second;
    //
    //    private int firstRemaining = 0;
    //    private int secondRemaining = 0;
    //
    //    Type type = Type.OUTSIDE;
    //
    //    void update(CharSequence line) {
    //        if (start.reset(line).find()) {
    //            first = Integer.parseInt(start.group(1)) - 1;
    //            String fl = start.group(2);
    //            firstRemaining = fl == null ? 1 : Integer.parseInt(fl);
    //            second = Integer.parseInt(start.group(3)) - 1;
    //            String sl = start.group(4);
    //            secondRemaining = sl == null ? 1 : Integer.parseInt(sl);
    //            type = Type.HUNK;
    //        } else if (type == Type.OUTSIDE) {
    //            // skip line
    //        } else if ((line.length() == 0 || line.charAt(0) == ' ')
    //                && (firstRemaining > 0 || secondRemaining > 0)) {
    //            first++;
    //            firstRemaining--;
    //            second++;
    //            secondRemaining--;
    //            type = Type.CONTEXT;
    //        } else if (removed.reset(line).find()) {
    //            first++;
    //            firstRemaining--;
    //            type = Type.REMOVED;
    //        } else if (added.reset(line).find()) {
    //            second++;
    //            secondRemaining--;
    //            type = Type.ADDED;
    //        } else {
    //            first = 0;
    //            second = 0;
    //            type = Type.OUTSIDE;
    //        }
    //    }
    //
    //    @Override
    //    public String toString() {
    //        return "DiffHunk(first=" + first + ", second=" + second
    //                + ", firstRemaining=" + firstRemaining
    //                + ", secondRemaining=" + secondRemaining
    //                + ", type=" + type + ")";
    //    }
    //
    //}


}
