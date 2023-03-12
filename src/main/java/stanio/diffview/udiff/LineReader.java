/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * Unlike {@code java.io.BufferedReader} this returns the newline character(s)
 * as part of the line.
 */
class LineReader {

    private Reader in;

    private boolean eof;

    private char[] buffer;

    private int pos;

    private int end;

    LineReader(Reader in) {
        this.in = in;
        this.buffer = new char[16 * 1024];
    }

    private void readMore() throws IOException {
        if (eof) return;

        if (pos > 0 && pos < end) {
            //System.err.println("Move: " + (end - pos));
            System.arraycopy(buffer, pos, buffer, 0, end - pos);
            end -= pos;
            pos = 0;
        }

        if (buffer.length < end + 1024) { // Expand buffer (just a little bit)
            //System.err.println("Expand: " + (buffer.length + 1024));
            buffer = Arrays.copyOf(buffer, buffer.length + 1024);
        }

        int count = in.read(buffer, end, buffer.length - end);
        //System.err.println("Read: " + (buffer.length - end) + " / " + count);
        if (count < 0) {
            eof = true;
        } else {
            end += count;
        }
    }

    String nextLine() throws IOException {
        if (eof) return null;

        int eol = -1;
        char[] buf = buffer;

    line_end:
        while (!eof) {
            for (int i = pos, e = end; i < e; i++) {
                if (buf[i] == '\n') {
                    eol = i + 1;
                    break line_end;
                }
            }
            readMore();
            buf = buffer;
        }

        if (eol < 0) {
            eol = end;
        }

        try {
            return new String(buf, pos, eol - pos);
        } finally {
            pos = eol;
        }
    }

}
