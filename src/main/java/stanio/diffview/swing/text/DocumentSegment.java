/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.text;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

/**
 * A {@linkplain Segment#setPartialReturn(boolean) partial} {@code Segment}
 * that updates automatically to satisfy full document access requests without
 * intermediate content copying.
 * <p>
 * The behavior is undefined if the document content changes after this
 * segment is constructed.</p>
 *
 * @see  Document#getText(int, int, Segment)
 */
public class DocumentSegment extends Segment {

    public final Document document;

    int sequenceStart;
    int sequenceLength;

    private int documentOffset;

    public DocumentSegment(Document document) {
        super(null, 0, 0);
        super.setPartialReturn(true);
        this.document = document;
        this.sequenceLength = document.getLength();
    }

    DocumentSegment(DocumentSegment segment) {
        super(segment.array, segment.offset, segment.count);
        super.setPartialReturn(true);
        this.document = segment.document;
        this.sequenceStart = segment.sequenceStart;
        this.sequenceLength = segment.sequenceLength;
        this.documentOffset = segment.documentOffset;
    }

    boolean updateTo(int position) {
        if (position < documentOffset || position >= documentOffset + count) {
            int length = document.getLength();
            if (position >= length)
                return false;

            try {
                document.getText(position, length - position, this);
            } catch (BadLocationException e) {
                throw (IndexOutOfBoundsException)
                        new IndexOutOfBoundsException(position).initCause(e);
            }
            documentOffset = position;
            super.first();
        }
        return true;
    }

    int getChar(int pos) {
        return updateTo(pos)
                ? array[pos - documentOffset + offset]
                : -1;
    }

    int getChars(int pos, char[] dst, int dstPos, int len) {
        if (updateTo(pos)) {
            final int size = Math.min(len, documentOffset + count - pos);
            System.arraycopy(array, pos - documentOffset + offset, dst, dstPos, size);
            return size;
        }
        return -1;
    }

    public void update(int start, int end) {
        sequenceStart = start;
        sequenceLength = end - start;
    }

    @Override
    public int length() {
        return sequenceLength;
    }

    @Override
    public char charAt(int index) {
        int ch = getChar(sequenceStart + index);
        if (ch == -1) {
            throw new IndexOutOfBoundsException(index);
        }
        return (char) ch;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        DocumentSegment segment = new DocumentSegment(this);
        segment.sequenceStart = sequenceStart + start;
        segment.sequenceLength = end - start;
        segment.count = Math.min(segment.count, segment.sequenceLength);
        return segment;
    }

    public String substring(int begin, int end) {
        try {
            return document.getText(sequenceStart + begin, end - begin);
        } catch (BadLocationException e) {
            throw new IllegalArgumentException("begin: "
                    + begin + ", end: " + end, e);
        }
    }

    @Override
    public String toString() {
        try {
            return document.getText(sequenceStart, sequenceLength);
        } catch (BadLocationException e) {
            return "";
        }
    }

    @Override
    public char first() {
        if (updateTo(sequenceStart)) {
            return super.first();
        }
        return DONE;
    }

    @Override
    public char last() {
        if (updateTo(sequenceStart + sequenceLength - 1)) {
            return super.last();
        }
        return DONE;
    }

    @Override
    public char current() {
        // TODO: Does it need something else?
        return super.current();
    }

    @Override
    public char next() {
        char ch = super.next();
        if (ch == DONE && super.getIndex()
                + documentOffset < sequenceStart + sequenceLength) {
            // TODO: Handle update
        }
        return ch;
    }

    @Override
    public char previous() {
        char ch = super.previous();
        if (ch == DONE && super.getIndex() + documentOffset > sequenceStart) {
            // TODO: Handle update
        }
        return ch;
    }

    @Override
    public char setIndex(int position) {
        if (updateTo(position)) {
            return super.setIndex(position - documentOffset);
        }
        throw new IllegalArgumentException("position: " + position);
    }

    @Override
    public int getBeginIndex() {
        return sequenceStart;
    }

    @Override
    public int getEndIndex() {
        return sequenceStart + sequenceLength;
    }

    @Override
    public int getIndex() {
        return super.getIndex() + documentOffset;
    }

}
