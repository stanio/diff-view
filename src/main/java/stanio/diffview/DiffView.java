/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * The main {@code diff-view} class, providing the application command-line
 * entry point.
 *
 * @implNote  Implements the main window GUI.
 *
 * @author  Stanimir Stamenkov &lt;stanio&#x40;yahoo.com&gt;
 */
@SuppressWarnings("serial")
public class DiffView extends JFrame {

    Prefs prefs;

    DiffView() {
        super("diff-view");
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initAppIcon();
        initMenuBar();
        initContent();
    }

    private void initAppIcon() {
        UIManager.addPropertyChangeListener(event -> {
            if (event.getPropertyName().equals("lookAndFeel")) {
                setIconImages(Collections.singletonList(new AppIcon()));
            }
        });
        super.setIconImages(Collections.singletonList(new AppIcon()));
    }

    private void initMenuBar() {
        JMenu viewMenu = new JMenu("File");
        viewMenu.setMnemonic(KeyEvent.VK_F);
        viewMenu.add(new JMenuItem(new AbstractAction("Settings...") {
            {
                putValue(MNEMONIC_KEY, KeyEvent.VK_T);
            }
            @Override public void actionPerformed(ActionEvent event) {
                SettingsDialog.show(DiffView.this, prefs);
            }
        }));
        viewMenu.addSeparator();

        AbstractAction exitAction = new AbstractAction("Exit") {
            {
                putValue(MNEMONIC_KEY, KeyEvent.VK_X);
                putValue(ACCELERATOR_KEY, KeyStroke
                        .getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK, false));
            }
            @Override public void actionPerformed(ActionEvent event) {
                close(DiffView.this);
            }
        };
        viewMenu.add(new JMenuItem(exitAction));

        super.getRootPane().getActionMap().put("exit", exitAction);
        super.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "exit");

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(viewMenu);
        super.setJMenuBar(menuBar);
    }

    private void initContent() {
        DiffOutlinePane treeOutline = new DiffOutlinePane();
        treeOutline.setName("File outline");

        DiffTextPane textViewer = new DiffTextPane();
        textViewer.setName("Diff text");

        treeOutline.diffText = textViewer.diffPane;

        JSplitPane splitPane = new JSplitPane(JSplitPane
                .HORIZONTAL_SPLIT, true, treeOutline, textViewer);
        //splitPane.setOneTouchExpandable(true);
        super.add(splitPane);
    }

    private JSplitPane getSplitPane() {
        return (JSplitPane) getContentPane().getComponent(0);
    }

    DiffTextPane getDiffPane() {
        return (DiffTextPane) getSplitPane().getRightComponent();
    }

    DiffOutlinePane getOutlinePane() {
        return (DiffOutlinePane) getSplitPane().getLeftComponent();
    }

    void updateTitle(Input input) {
        Object file;
        if (input.file != null) {
            file = input.file;
        } else if (input.url != null) {
            file = input.url;
        } else {
            file = "<stdin>";
        }
        setTitle(file + " - " + getTitle());
    }

    void applyPrefs(Prefs prefs) {
        if (prefs.windowBounds == null) {
            setSize(640, 400);
            //pack();
            setLocationRelativeTo(null);
        } else {
            setBounds(prefs.windowBounds);
        }
        if (prefs.windowState >= 0) {
            setExtendedState(prefs.windowState);
        }
        if (prefs.splitPosition >= 0) {
            getSplitPane().setDividerLocation(prefs.splitPosition);
        }
        Font f = null;
        if (prefs.textFont != null) {
            f = Font.decode(prefs.textFont);
        }
        if (f == null || f.getFamily().equals(Font.DIALOG)
                && !f.getName().equalsIgnoreCase(Font.DIALOG)) {
            f = new Font(Font.MONOSPACED, Font.PLAIN,
                         f == null ? 13 : f.getSize());
        }
        getDiffPane().diffPane.setFont(f);
    }

    void updatePrefs(Prefs prefs) {
        prefs.windowBounds = getBounds();
        prefs.windowState = getExtendedState();
        prefs.splitPosition = getSplitPane().getDividerLocation();
        prefs.textFont = encodeFont(getDiffPane().diffPane.getFont());
    }

    private static String encodeFont(Font font) {
        String style;
        switch (font.getStyle()) {
        case Font.BOLD | Font.ITALIC:
            style = "-BoldItalic";
            break;
        case Font.BOLD:
            style = "-Bold";
            break;
        case Font.ITALIC:
            style = "-Italic";
            break;
        default:
            style = "";
        }
        return font.getName() + style + '-' + font.getSize();
    }

    static void close(Window window) {
        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        window.dispose();
    }

    /**
     * The {@code diff-view} command-line entry point.
     * <p>
     * <i>Usage:</i></p>
     * <pre>
     * diff-view example.diff
     *
     * git diff ... | diff-view</pre>
     *
     * @param   args  the command-line arguments provided by the JVM
     *
     * @see     <a href="https://docs.oracle.com/javase/tutorial/getStarted/application/index.html#MAIN"
     *                  >The <code>main</code> Method</a> <i>(Getting Started &ndash; The Java Tutorials)</i>
     * @see     <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html#description"
     *                  >The java Command</a> <i>(JDK Tool Specifications)</i>
     * @see     <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-12.html#jls-12.1"
     *                  >Java Virtual Machine Startup</a> <i>(JLS &sect;12.1)</i>
     * @see     <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-5.html#jvms-5.2"
     *                  >Java Virtual Machine Startup</a> <i>(JVMS &sect;5.2)</i>
     */
    public static void main(String[] args) {
        Prefs prefs = Prefs.load();

        SwingUtilities.invokeLater(() -> {
            initLookAndFeel(prefs);

            DiffView window = new DiffView();
            window.prefs = prefs;
            window.setDefaultCloseOperation(EXIT_ON_CLOSE);

            Input input = resolveInput(args);
            window.updateTitle(input);
            window.getDiffPane().load(input);

            window.applyPrefs(prefs);
            window.setVisible(true);

            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    window.updatePrefs(prefs);
                    prefs.save();
                }
            });
         });
    }

    static void initLookAndFeel(Prefs prefs) {
        String lookAndFeel = "com.formdev.flatlaf.FlatIntelliJLaf";
        if (prefs.darkTheme) {
            lookAndFeel = "com.formdev.flatlaf.FlatDarculaLaf";
        }

        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            System.err.println(e);
        }

        // https://www.formdev.com/flatlaf/customizing/#arrow_type
        UIManager.put("Component.arrowType", "chevron");

        // https://www.formdev.com/flatlaf/components/tree/
        //UIManager.put("Tree.icon.leafColor", new Color(.5f, .5f, .5f, .5f));
        //Color folderColor = new Color(205, 175, 0);
        //UIManager.put("Tree.icon.closedColor", folderColor);
        //UIManager.put("Tree.icon.openColor", folderColor);
        UIManager.put("Tree.showDefaultIcons", true);

        // https://www.formdev.com/flatlaf/typography/#change_default_font
        Font defaultFont = UIManager.getFont("defaultFont");
        if (defaultFont != null && defaultFont.getSize() < 13) {
            UIManager.put("defaultFont",
                    new FontUIResource(defaultFont.deriveFont(13f)));
        }
    }

    static Input resolveInput(String[] args) {
        if (args.length == 0 && System.console() == null) {
            return new Input(fileEncoding(), new BufferedReader(
                    new InputStreamReader(System.in, fileEncoding())));
            //try {
            //    return new InputStreamReader(DiffView.class
            //            .getResource("sample.diff").openStream(), fileEncoding());
            //} catch (IOException e) {
            //    e.printStackTrace();
            //    System.exit(1);
            //}
        } else if (args.length == 1) {
            try {
                return openStream(args[0]);
            } catch (InvalidPathException | IOException e) {
                JOptionPane.showMessageDialog(null,
                        e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        JOptionPane.showMessageDialog(null, new Object[] {
                "diff-view <source>", "", "diff ... | diff-view" },
                "Usage", JOptionPane.INFORMATION_MESSAGE);
        System.exit(2);
        throw new AssertionError("Execution past System.exit()");
    }

    @SuppressWarnings("resource")
    private static Input openStream(String source)
            throws InvalidPathException, IOException {
        try {
            URL url = new URI(source).toURL();
            URLConnection con = url.openConnection();
            Charset charset = getContentCharset(con);
            return new Input(url, charset, new BufferedReader(
                    new InputStreamReader(con.getInputStream(), charset)));
        } catch (URISyntaxException | IllegalArgumentException | MalformedURLException e) {
            Path file = Paths.get(source);
            return new Input(file, fileEncoding(),
                    Files.newBufferedReader(file, fileEncoding()));
        }
    }

    private static Charset getContentCharset(URLConnection con) {
        String encoding = con.getContentEncoding();
        try {
            return encoding == null ? StandardCharsets.UTF_8
                                    : Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            return StandardCharsets.UTF_8;
        }
    }

    private static Charset fileEncoding() {
        return StandardCharsets.UTF_8;
    }


    static class Input {

        Path file;
        URL url;
        Charset charset;
        Reader stream;

        Input(Charset charset, Reader stream) {
            this.charset = charset;
            this.stream = stream;
        }

        Input(Path file, Charset charset, Reader stream) {
            this(charset, stream);
            this.file = file;
        }

        Input(URL url, Charset charset, Reader stream) {
            this(charset, stream);
            this.url = url;
        }

    }


}
