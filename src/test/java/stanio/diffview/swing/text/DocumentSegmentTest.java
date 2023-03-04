/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.text;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import org.mockito.InOrder;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DocumentSegmentTest {

    private static final String BASE_CONTENT = "Foo bar\nBaz qux";

    private DocumentSegment segment;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        Document doc = spy(DefaultStyledDocument.class);
        doc.insertString(0, BASE_CONTENT, null);
        segment = new DocumentSegment(doc);
    }

    @Test
    public void singleChar() {
        final int pos = 12;

        int ch = segment.getChar(pos);

        assertEquals((char) ch, BASE_CONTENT.charAt(pos), "single char");
    }

    @Test
    public void fullChunk() {
        final int pos = 4;
        char[] chunk = new char[7]; // pos + length <= BASE_CONTENT.length()

        int count = segment.getChars(pos, chunk, 0, chunk.length);

        assertEquals(count, chunk.length, "read count");
        assertEquals(new String(chunk),
                BASE_CONTENT.substring(pos, pos + chunk.length), "chars");
    }

    @Test
    public void partialChunk() {
        final int pos = 8;
        char[] chunk = new char[12]; // pos + length > BASE_CONTENT.length()

        int count = segment.getChars(pos, chunk, 0, chunk.length);

        assertEquals(count, BASE_CONTENT.length() - pos, "read count");
        assertEquals(new String(chunk, 0, count),
                BASE_CONTENT.substring(pos, pos + count), "chars");
    }

    @Test
    public void singleCharOutOfBounds() {
        int ch = segment.getChar(segment.document.getLength());

        assertEquals(ch, -1, "single char");
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class,
            expectedExceptionsMessageRegExp = ".*-1")
    public void singleCharNegativePosition() {
        segment.getChar(-1);
    }

    @Test
    public void chunkOutOfBounds() {
        char[] chunk = new char[12];
        int count = segment.getChars(segment.document.getLength() + 1, chunk, 0, chunk.length);

        assertEquals(count, -1, "read count");
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class,
            expectedExceptionsMessageRegExp = ".*-2")
    public void chunkNegativePosition() {
        segment.getChars(-2, new char[10], 0, 10);
    }

    @Test
    public void updateFragmentNext() throws Exception {
        // Given
        int firstChunkEnd = segment.document.getLength();
        segment.getChar(0); // Ensure existing content
        segment.document.insertString(firstChunkEnd, "Hello World!", null);
        int secondChunkLength = segment.document.getLength() - firstChunkEnd;

        // When
        char[] chunk = new char[11];
        int count = segment.getChars(firstChunkEnd, chunk, 0, chunk.length);

        // Then
        assertEquals(count, chunk.length, "read count");
        assertEquals(new String(chunk), "Hello World", "chars");

        InOrder inOrder = inOrder(segment.document);
        inOrder.verify(segment.document).getText(0, firstChunkEnd, segment);
        inOrder.verify(segment.document).getText(firstChunkEnd, secondChunkLength, segment);
    }

    @Test
    public void updateFragmentRandomAccess() throws Exception {
        // Given
        int firstChunkEnd = segment.document.getLength();
        segment.getChar(0);
        segment.document.insertString(firstChunkEnd, "Hello World!", null);
        int secondChunkLength = segment.document.getLength() - firstChunkEnd;
        segment.getChar(firstChunkEnd);

        // When
        int ch = segment.getChar(0);

        // Then
        assertEquals((char) ch, 'F', "single char");

        InOrder inOrder = inOrder(segment.document);
        inOrder.verify(segment.document).getText(0, firstChunkEnd, segment);
        inOrder.verify(segment.document).getText(firstChunkEnd, secondChunkLength, segment);
        inOrder.verify(segment.document).getText(0, segment.document.getLength(), segment);
    }

    @Test
    public void segmentCopy() throws Exception {
        DocumentSegment copy = new DocumentSegment(segment);
        final int pos = 6;

        int ch = copy.getChar(pos);

        assertEquals((char) ch, BASE_CONTENT.charAt(pos), "single char");
    }

}
