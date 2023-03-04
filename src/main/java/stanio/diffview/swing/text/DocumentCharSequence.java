/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.text;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Implements {@code CharSequence} over {@code Document}'s content.  Avoids
 * copying the entire content for the purpose of scanning the text.  May be
 * used as input to regular expression matching, for example.
 * <p>
 * The behavior is undefined if the document content changes after this
 * sequence is constructed.</p>
 */
public class DocumentCharSequence implements CharSequence {

    private final DocumentSegment segment;

    private final int offset;

    private final int length;

    private DocumentCharSequence(DocumentSegment segment, int offset, int length) {
        this.segment = segment;
        this.offset = offset;
        this.length = length;
    }

    public static DocumentCharSequence of(Document document, int offset, int length) {
        return new DocumentCharSequence(new DocumentSegment(document), offset, length);
    }

    public static DocumentCharSequence of(Document document) {
        return of(document, 0, document.getLength());
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        int ch = segment.getChar(offset + index);
        if (ch == -1) {
            throw new IndexOutOfBoundsException(index);
        }
        return (char) ch;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new DocumentCharSequence(new DocumentSegment(segment), offset + start, end - start);
    }

    @Override
    public String toString() {
        try {
            return segment.document.getText(offset, length);
        } catch (BadLocationException e) {/* fall back */}

        int currentLength = segment.document.getLength();
        try {
            return (offset < currentLength)
                    ? segment.document.getText(offset, currentLength - offset)
                    : "";
        } catch (BadLocationException e) {
            return "";
        }
    }

}
