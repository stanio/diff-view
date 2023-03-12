/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.udiff;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

import stanio.diffview.swing.text.BoxBackgroundFactory;

import stanio.diffview.udiff.ParseResult.Type;
import stanio.diffview.udiff.UDiffDocument.Attribute;
import stanio.diffview.udiff.UDiffDocument.StyleName;

@SuppressWarnings("serial")
public class UDiffEditorKit extends StyledEditorKit {

    private static Map<Type, String> udiffStyles;
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
        if (doc instanceof StyledDocument) {
            parse(in, (StyledDocument) doc, pos);
        } else {
            super.read(in, doc, pos);
        }
    }

    void parse(Reader in, StyledDocument document, int pos)
            throws IOException, BadLocationException
    {
        UDiffParser parser = new UDiffParser();
        LineReader reader = new LineReader(in);
        String line;
        while ((line = reader.nextLine()) != null) {
            parser.update(line);

            insert(line, parser, document, pos);

            pos += line.length();
        }
        last = null;
        lineNumbers = null;
    }

    private Map<Integer, String> lineNumbers = new HashMap<>();
    private Element last;

    private void insert(String line, ParseResult result,
                        StyledDocument document, int pos)
            throws BadLocationException {
        document.insertString(pos, line, null);

        Type lineType = result.getType();
        Element paragraph = setLogicalStyle(document, pos, lineType);
        if (lineType == Type.HUNK) {
            Style hunkLabel = document.getStyle(StyleName.HUNK_LABEL);
            int labelStart = paragraph.getStartOffset() + result.getTermEnd();
            document.setCharacterAttributes(labelStart,
                    paragraph.getEndOffset() - labelStart, hunkLabel, true);
        } else if (lineType == Type.FROM_FILE) {
            String file = line.substring(result.getTermStart(), result.getTermEnd());
            if (!file.startsWith("/dev/null")) {
                SimpleAttributeSet a = new SimpleAttributeSet();
                a.addAttribute(Attribute.FILE, file.replaceFirst("^a/", ""));
                document.setParagraphAttributes(paragraph.getStartOffset(),
                        paragraph.getEndOffset() - paragraph.getStartOffset(),
                        a, false);
            }
        } else if (lineType == Type.TO_FILE) {
            String file = line.substring(result.getTermStart(), result.getTermEnd());
            if (!file.startsWith("/dev/null")) {
                SimpleAttributeSet a = new SimpleAttributeSet();
                a.addAttribute(Attribute.FILE, file.replaceFirst("^b/", ""));
                document.setParagraphAttributes(last.getStartOffset(),
                        last.getEndOffset() - last.getStartOffset(),
                        a, false);
            }
        } else if (lineType == Type.CONTEXT
                || lineType == Type.ADDED
                || lineType == Type.REMOVED) {
            String fromLine = lineNumbers.computeIfAbsent(result.getFromLine(), Object::toString);
            String toLine = lineNumbers.computeIfAbsent(result.getToLine(), Object::toString);

            SimpleAttributeSet attrs = new SimpleAttributeSet();
            if (lineType != Type.ADDED) {
                attrs.addAttribute(Attribute.FROM_LINE, fromLine);
            }
            if (lineType != Type.REMOVED) {
                attrs.addAttribute(Attribute.TO_LINE, toLine);
            }
            document.setParagraphAttributes(paragraph.getStartOffset(),
                    paragraph.getEndOffset() - paragraph.getStartOffset(),
                    attrs, false);
        }
        last = paragraph;
    }

    private static
    Element setLogicalStyle(StyledDocument document, int pos, Type lineType) {
        Element paragraph = document.getParagraphElement(pos);
        String name = udiffStyles.get(lineType);
        Style style = (name == null) ? null : document.getStyle(name);
        if (style == null) style = document.getStyle(StyleContext.DEFAULT_STYLE);
        document.setLogicalStyle(paragraph.getStartOffset(), style);
        return paragraph;
    }

}
