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
 *
 * @see  Document#getText(int, int, Segment)
 */
class DocumentSegment extends Segment {

    public final Document document;

    private int start;

    public DocumentSegment(Document document) {
        super(null, 0, 0);
        super.setPartialReturn(true);
        this.document = document;
    }

    public DocumentSegment(DocumentSegment segment) {
        super(segment.array, segment.offset, segment.count);
        super.setPartialReturn(true);
        this.document = segment.document;
        this.start = segment.start;
    }

    private boolean updateTo(int position) {
        if (position < start || position >= start + count) {
            int length = document.getLength();
            if (position >= length)
                return false;

            try {
                document.getText(position, length - position, this);
            } catch (BadLocationException e) {
                throw (IndexOutOfBoundsException)
                        new IndexOutOfBoundsException(position).initCause(e);
            }
            start = position;
        }
        return true;
    }

    public int getChar(int pos) {
        return updateTo(pos)
                ? array[pos - start + offset]
                : -1;
    }

    public int getChars(int pos, char[] dst, int dstPos, int len) {
        if (updateTo(pos)) {
            final int size = Math.min(len, start + count - pos);
            System.arraycopy(array, pos - start + offset, dst, dstPos, size);
            return size;
        }
        return -1;
    }

}
