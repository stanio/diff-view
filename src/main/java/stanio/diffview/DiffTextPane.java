/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

@SuppressWarnings("serial")
class DiffTextPane extends JScrollPane {

    private JEditorPane textPane;

    private JTextPane lineRuler;

    public DiffTextPane() {
        super(VERTICAL_SCROLLBAR_ALWAYS,
                HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.textPane = new JTextPane();
        super.setViewportView(textPane);
        setUpTextPane();
        createNumbers();
    }

    private void setUpTextPane() {
        JEditorPane text = textPane;
        text.setEditable(false);
        text.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        // Sample text content
        text.setContentType("text/html");
        text.setText("<div>Fooooo <big>Barrrr</big> <small>Bazzzz</small></div>"
                + "<div>Fooooo</div>"
                + "<div><big>Barrrr</big></div>"
                + "<div>Fooooo<br><big>Barrrr</big></div>"
                + "<div><big>Fooooo</big><br>Barrrr</div>"
                + "<div>Fooooo<big><br>Barrrr</big></div>");
    }

    private void createNumbers() {
        StyledDocument ruler = new DefaultStyledDocument();
        StyleConstants.setAlignment(ruler.getStyle(StyleContext.DEFAULT_STYLE),
                                    StyleConstants.ALIGN_RIGHT);
        lineRuler = new JTextPane(ruler);
        lineRuler.setEditable(false);
        lineRuler.setEnabled(false);

        invokeMuchLater(() -> {
            // Sample ruler content
            try {
                String numbers = IntStream.rangeClosed(1, 17)
                                          .mapToObj(Integer::toString)
                                          .collect(Collectors.joining("\n"));
                ruler.insertString(0, numbers, null);
            } catch (BadLocationException e) {
                throw new IllegalStateException(e);
            }
        });

        super.setRowHeaderView(lineRuler);
    }

    /**
     * Defers the execution of the given task to after the next full event
     * processing cycle.
     *
     * @param   task  the task to execute
     * @see     SwingUtilities#invokeLater(Runnable)
     */
    static void invokeMuchLater(Runnable task) {
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(task));
    }

}
