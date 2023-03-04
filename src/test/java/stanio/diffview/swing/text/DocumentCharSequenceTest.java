/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.text;

import static org.testng.Assert.assertEquals;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DocumentCharSequenceTest {

    private static final String
            BASE_CONTENT = "Sugar doppio that, ut, extra\n"
                           + "aged coffee aromatic bar.";

    private DocumentCharSequence charSequence;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        Document doc = new DefaultStyledDocument();
        doc.insertString(0, BASE_CONTENT, null);
        charSequence = DocumentCharSequence.of(doc);
    }

    @Test
    public void singleChar() throws Exception {
        final int index = 0;

        int ch = charSequence.charAt(index);

        assertEquals((char) ch, BASE_CONTENT.charAt(index), "char");
    }

    @Test
    public void basicFunction() throws Exception {
        final int start = 5, end = 10;

        CharSequence subSeq = charSequence.subSequence(start, end);

        assertEquals(subSeq.toString(),
                BASE_CONTENT.substring(start, end), "sub-sequence");
    }

}
