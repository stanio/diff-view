/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

public class LineReaderTest {

    @Test
    public void completeLines() throws Exception {
        Reader source = new StringReader("Foo\nBar\r\nBaz\rQux");
        List<String> lines = readAllLines(source);
        assertThat("text lines", lines, contains("Foo\n", "Bar\r\n", "Baz\rQux"));
    }

    private List<String> readAllLines(Reader source) throws IOException {
        LineReader reader = new LineReader(source);
        List<String> list = new ArrayList<>();
        String line;
        while ((line = reader.nextLine()) != null) {
            list.add(line);
        }
        return list;
    }

}
