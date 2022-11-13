/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class App extends JFrame {

    public App() {
        super("diff-view");
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initMenuBar();
        initContent();
    }

    private void initMenuBar() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(new JMenuItem("Settings..."));
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem("Exit"));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(viewMenu);
        super.setJMenuBar(menuBar);
    }

    private void initContent() {
        DiffOutlinePane treeOutline = new DiffOutlinePane();
        treeOutline.setName("File-outline");

        DiffTextPane textViewer = new DiffTextPane();
        textViewer.setName("Diff-text");

        JSplitPane splitPane = new JSplitPane(JSplitPane
                .HORIZONTAL_SPLIT, true, treeOutline, textViewer);
        //splitPane.setOneTouchExpandable(true);
        super.add(splitPane);
    }

    /**
     * The command-line entry point.
     *
     * @param   args  the command-line arguments provided by the JVM
     * @see     <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html#description"
     *                  >The java Command</a>
     * @see     <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-12.html#jls-12.1"
     *                  >Java Virtual Machine Startup</a> <i>(JLS &sect;12.1)</i>
     * @see     <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-5.html#jvms-5.2"
     *                  >Java Virtual Machine Startup</a> <i>(JVMS &sect;5.2)</i>
     */
    public static void main(String[] args) {
        // TODO: Read input either from file or STDIN, if redirected/piped.

        // TODO: Read saved java.prefs/java.util.prefs.Preferences
        SwingUtilities.invokeLater(() -> {
            com.formdev.flatlaf.FlatDarculaLaf.setup();

            App window = new App();
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

}
