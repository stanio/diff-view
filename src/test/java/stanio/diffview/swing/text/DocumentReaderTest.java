/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.text;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DocumentReaderTest {

    private static final String
            BASE_CONTENT = "Paneer red leicester halloumi.\nMacaroni"
                           + " cheese say cheese bavarian bergkase.";

    private DocumentReader reader;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        Document doc = new DefaultStyledDocument();
        doc.insertString(0, BASE_CONTENT, null);
        reader = new DocumentReader(doc);
    }

    @Test
    public void singleChar() throws Exception {
        int ch = reader.read();

        assertEquals((char) ch, BASE_CONTENT.charAt(0), "char");
    }

    @Test
    public void basicFunction() throws Exception {
        char[] chunk = new char[12];

        int count = reader.read(chunk);

        assertEquals(count, chunk.length, "read count");
        assertEquals(new String(chunk, 0, count),
                BASE_CONTENT.substring(0, count), "chars");
    }

    @Test(expectedExceptions = InterruptedIOException.class,
                expectedExceptionsMessageRegExp = "Interrupted")
    public void interruptReading() throws Exception {
        char[] chunk = new char[12];
        reader.read(chunk);
        Thread.currentThread().interrupt();

        reader.read(chunk);
    }

    @Test
    public void skipAndPosition() throws Exception {
        long count = reader.skip(BASE_CONTENT.length() + 10);

        assertEquals(count, BASE_CONTENT.length(), "skip count");
        assertEquals(reader.position(), count, "position");
    }

    @Test
    public void markSupported() throws Exception {
        assertTrue(reader.markSupported(), "markSupported");
    }

    @Test
    public void markAndReset() throws Exception {
        char[] chunk = new char[12];
        reader.mark(Integer.MAX_VALUE);
        reader.read(chunk);
        reader.reset();

        int count = reader.read(chunk, 0, 10);

        assertEquals(count, 10, "read count");
        assertEquals(new String(chunk, 0, count),
                BASE_CONTENT.substring(0, count), "chars");
    }

    @Test
    public void readAfterClose() throws Exception {
        char[] chunk = new char[12];
        reader.close();

        try {
            reader.read(chunk);
        } catch (IOException e) {
            fail("Read failed after close", e);
        }
    }

}
