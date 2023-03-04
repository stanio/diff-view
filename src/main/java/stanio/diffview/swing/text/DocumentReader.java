/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.text;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;

import javax.swing.text.Document;

/**
 * A {@code Reader} over {@code Document}'s content.
 * <p>
 * The reader is not safe for use by multiple threads.  Synchronization should
 * be ensured by means of {@link javax.swing.text.AbstractDocument#readLock()} /
 * {@code readUnlock()}, for example.  The behavior is undefined if the document
 * content changes after this reader is constructed.</p>
 * <p>
 * This reader's methods don't generally throw {@code IOException} unless the
 * reading thread is interrupted &ndash; then an {@code InterruptedIOException}
 * is thrown.  This could be used to implement background parsing (off the EDT),
 * and interrupt the parsing when/if the EDT needs to read the document to
 * perform immediate painting.  The {@code InterruptedIOException} would be
 * handled specifically then.</p>
 */
public final class DocumentReader extends Reader {

    private final DocumentSegment segment;

    private int position;

    private int mark;

    public DocumentReader(Document doc) {
        this(doc, 0);
    }

    public DocumentReader(Document doc, int start) {
        this.segment = new DocumentSegment(doc);
        this.position = start;
        this.mark = start;
    }

    /**
     * The current document position.
     *
     * @return  The starting position for the next {@code read()} operation
     */
    public int position() {
        return position;
    }

    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Interrupted");
        }
    }

    @Override
    public int read() throws IOException {
        checkInterrupted();
        return segment.getChar(position++);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        checkInterrupted();
        if (len == 0) return 0;

        int read = 0, count;
        while (read < len && (count =
                segment.getChars(position, cbuf, off + read, len - read)) != -1) {
            read += count;
        }
        position += read;
        return (read == 0) ? -1 : read;
    }

    @Override
    public long skip(long n) throws IOException {
        checkInterrupted();
        long n1 = Math.min(n, segment.document.getLength() - position);
        position += n1;
        return n1;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        mark = position;
    }

    @Override
    public void reset() throws IOException {
        position = mark;
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }

}
