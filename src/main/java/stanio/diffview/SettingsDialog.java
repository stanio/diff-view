/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

class SettingsDialog {

    static void show(Component parent, Prefs prefs) {
        JCheckBox darkTheme = new JCheckBox("Dark theme", prefs.darkTheme);
        darkTheme.addActionListener(event -> {
            prefs.darkTheme = darkTheme.isSelected();

            String lookAndFeel = "com.formdev.flatlaf.FlatIntelliJLaf";
            if (prefs.darkTheme) {
                lookAndFeel = "com.formdev.flatlaf.FlatDarculaLaf";
            }
            try {
                UIManager.setLookAndFeel(lookAndFeel);
                SwingUtilities.updateComponentTreeUI(parent instanceof Window
                        ? parent : SwingUtilities.getWindowAncestor(parent));
                SwingUtilities.updateComponentTreeUI(SwingUtilities
                        .getWindowAncestor((Component) event.getSource()));
                ((Component) event.getSource()).requestFocusInWindow();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(darkTheme);
        JOptionPane.showOptionDialog(parent, pane, "Settings",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[] { "Close" }, "Close");
    }

}
