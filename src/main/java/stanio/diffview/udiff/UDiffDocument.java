/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.StyleContext;

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
        TO_LINE;

        public static int getLineNo(Element elem) {
            Object lineNo = elem.getAttributes().getAttribute(FROM_LINE);
            return (lineNo instanceof Integer) ? (int) lineNo : 0;
        }

    } // enum Attributes


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
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        super.insertUpdate(chng, attr);
    }

}
