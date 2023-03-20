/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

import static stanio.diffview.udiff.UDiffEditorKit.udiffStyles;

import java.util.HashMap;
import java.util.Map;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import stanio.diffview.swing.text.DocumentSegment;
import stanio.diffview.udiff.ParseResult.Type;

/**
 * Should ideally express the following structure:
 * <ul>
 * <li>&lt;root&gt;
 *   <ul>
 *   <li>Message/comment</li>
 *   <li>File entry #1</li>
 *   <li>File entry #N</li>
 *   </ul></li>
 * </ul>
 * <p>File entries should have an attribute (f.e. "file") specifying the target
 * file path &ndash; for use in outline trees.  Every entry would be composed of
 * lines/paragraphs:</p>
 * <ul>
 * <li>File entry (file=..., type=added|deleted|modified|renamed)
 *   <ul>
 *   <li>Index: line</li>
 *   <li>Message line(s)</li>
 *   <li>{@code diff} command line</li>
 *   <li>Message line(s)</li>
 *   <li>from-file line</li>
 *   <li>to-file line</li>
 *   <li>Message line(s)</li>
 *   <li>hunk header ()</li>
 *   <li>context line(s)</li>
 *   <li>deleted line(s)</li>
 *   <li>added line(s)</li>
 *   <li>context line(s)</li>
 *   <li>Message line(s)</li>
 *   </ul></li>
 * </ul>
 * <p>Note, the file entry {@code file} attribute would generally identify
 * the to-file path (in case of renames), unless it is {@code /dev/null} which
 * would indicate the original file is deleted so the from-file should be used.</p>
 * <p>
 * The "Index: " line as well as the "diff command line" are used to identify
 * (and group) the following lines into a new "File entry".</p>
 *
 * @see  UDiffParser
 */
@SuppressWarnings("serial")
public class UDiffDocument extends DefaultStyledDocument {


    public enum Attribute {
        FILE,
        FROM_LINE,
        TO_LINE
    }


    public final class StyleName {

        public static final String DELETED_LINE = "deleted-line";
        public static final String INSERTED_LINE = "inserted-line";
        public static final String DELETED_NUMBER = "deleted-number";
        public static final String INSERTED_NUMBER = "inserted-number";
        public static final String MESSAGE = "message-text";
        public static final String DIFF_COMMAND = "diff-command";
        public static final String FROM_FILE = "from-file";
        public static final String TO_FILE = "to-file";
        public static final String HUNK = "hunk";
        public static final String HUNK_LABEL = "hunk-label";

        private StyleName() {}

    } // class Styles


    public UDiffDocument() {
        this(DiffStyles.getDefault());
    }

    public UDiffDocument(StyleContext styles) {
        super(styles);
    }

    public UDiffDocument(Content c, StyleContext styles) {
        super(c, styles);
    }

    @Override
    protected AbstractElement createDefaultRoot() {
        return super.createDefaultRoot();
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException {
        checkParseThread();
        super.insertString(offs, str, a);
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
        checkParseThread();
        super.remove(offs, len);
    }

    @Override
    public void replace(int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        checkParseThread();
        super.replace(offset, length, text, attrs);
    }

    private void checkParseThread() {
        Thread th = parseThread;
        if (th == null || th == Thread.currentThread())
            return;

        throw new IllegalStateException("Ongoing parsing in another thread");
    }

    private volatile Thread parseThread;
    private UDiffParser readParser;

    void setReadParser(UDiffParser parser) {
        this.readParser = parser;
        if (parser == null) {
            parseThread = null;
            lineNumbers = null;
            last = null;
        } else {
            parseThread = Thread.currentThread();
            lineNumbers = new HashMap<>();
        }
    }

    @Override
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        super.insertUpdate(chng, attr);
        UDiffParser parser = readParser;
        if (parser == null) return;

        DocumentSegment text = new DocumentSegment(this);
        Element root = getDefaultRootElement();
        int end = chng.getOffset() + chng.getLength();
        for (int i = root.getElementIndex(chng.getOffset()),
                count = root.getElementCount(); i < count; i++) {
            Element paragraph = root.getElement(i);
            if (paragraph.getStartOffset() >= end)
                break;

            text.update(paragraph.getStartOffset(),
                    Math.min(paragraph.getEndOffset(), end));
            if (text.charAt(text.length() - 1) != '\n')
                break;

            updateLine(paragraph, parser, text, chng);
        }

    }

    private Map<Integer, String> lineNumbers;
    private Element last;

    private void updateLine(Element paragraph,
                            UDiffParser parser,
                            DocumentSegment line,
                            DefaultDocumentEvent change) {
        parser.update(line);

        Type lineType = parser.getType();
        setLogicalStyle(paragraph, lineType);
        if (lineType == Type.HUNK) {
            Style hunkLabel = getStyle(StyleName.HUNK_LABEL);
            int labelStart = paragraph.getStartOffset() + parser.getTermEnd();
            buffer.change(labelStart, paragraph.getEndOffset() - labelStart, change);
            MutableAttributeSet attrs = (MutableAttributeSet)
                    paragraph.getElement(paragraph.getElementIndex(labelStart));
            attrs.addAttributes(hunkLabel);
        } else if (lineType == Type.FROM_FILE) {
            String file = line.substring(parser.getTermStart(), parser.getTermEnd());
            if (!file.startsWith("/dev/null")) {
                MutableAttributeSet a = (MutableAttributeSet) paragraph.getAttributes();
                a.addAttribute(Attribute.FILE, file.replaceFirst("^a/", ""));
            }
        } else if (lineType == Type.TO_FILE) {
            String file = line.substring(parser.getTermStart(), parser.getTermEnd());
            if (!file.startsWith("/dev/null")) {
                MutableAttributeSet a = (MutableAttributeSet) last.getAttributes();
                a.addAttribute(Attribute.FILE, file.replaceFirst("^b/", ""));
            }
        } else if (lineType == Type.CONTEXT
                || lineType == Type.ADDED
                || lineType == Type.REMOVED) {
            String fromLine = lineNumbers.computeIfAbsent(parser.getFromLine(), Object::toString);
            String toLine = lineNumbers.computeIfAbsent(parser.getToLine(), Object::toString);

            MutableAttributeSet attrs = (MutableAttributeSet) paragraph.getAttributes();
            if (lineType != Type.ADDED) {
                attrs.addAttribute(Attribute.FROM_LINE, fromLine);
            }
            if (lineType != Type.REMOVED) {
                attrs.addAttribute(Attribute.TO_LINE, toLine);
            }
        }
        last = paragraph;
    }

    private void setLogicalStyle(Element paragraph, Type lineType) {
        String name = udiffStyles.get(lineType);
        Style style = (name == null) ? null : getStyle(name);
        if (style == null) style = getStyle(StyleContext.DEFAULT_STYLE);
        ((AbstractElement) paragraph).setResolveParent(style);
    }

}
