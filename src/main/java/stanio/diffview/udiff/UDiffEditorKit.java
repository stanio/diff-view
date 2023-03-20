/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

import stanio.diffview.swing.text.BoxBackgroundFactory;

import stanio.diffview.udiff.ParseResult.Type;
import stanio.diffview.udiff.UDiffDocument.StyleName;

@SuppressWarnings("serial")
public class UDiffEditorKit extends StyledEditorKit {

    static Map<Type, String> udiffStyles;
    static {
        Map<Type, String> styleMap = new EnumMap<>(Type.class);
        styleMap.put(Type.MESSAGE, StyleName.MESSAGE);
        styleMap.put(Type.DIFF_CMD, StyleName.DIFF_COMMAND);
        styleMap.put(Type.FROM_FILE, StyleName.FROM_FILE);
        styleMap.put(Type.TO_FILE, StyleName.TO_FILE);
        styleMap.put(Type.HUNK, StyleName.HUNK);
        styleMap.put(Type.CONTEXT, StyleContext.DEFAULT_STYLE);
        styleMap.put(Type.REMOVED, StyleName.DELETED_LINE);
        styleMap.put(Type.ADDED, StyleName.INSERTED_LINE);
        udiffStyles = styleMap;
    }

    private final ViewFactory viewFactory;

    public UDiffEditorKit() {
        this.viewFactory = new BoxBackgroundFactory(super.getViewFactory());
    }

    @Override
    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    @Override
    public Document createDefaultDocument() {
        return new UDiffDocument();
    }

    @Override
    public void read(Reader in, Document doc, int pos)
            throws IOException, BadLocationException
    {
        if (pos == 0 && doc instanceof UDiffDocument) {
            try {
                parse(in, (UDiffDocument) doc);
            } finally {
                ((UDiffDocument) doc).setReadParser(null);
            }
        } else {
            super.read(in, doc, pos);
        }
    }

    void parse(Reader in, UDiffDocument document)
            throws IOException, BadLocationException
    {
        int pos = 0;
        document.setReadParser(new UDiffParser());
        char[] buf = new char[8 * 1024];
        int bufLen;

        while ((bufLen = in.read(buf)) >= 0) {
            if (bufLen == 0) {
                Thread.yield();
                continue;
            }
            document.insertString(pos, new String(buf, 0, bufLen), null);
            pos += bufLen;
        }
    }

}
