/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import java.util.prefs.Preferences;

import java.awt.Font;
import java.awt.Rectangle;

class Prefs {

    static interface Key {
        String darkTheme = "dark-theme";
        String windowBounds = "window.bounds";
        String windowState = "window.state";
        String splitPosition = "split.position";
        String textFont = "text.font";
    }

    private static final boolean DEBUG = Boolean
            .getBoolean("stanio.diffview.debug") || true;

    volatile Rectangle windowBounds;

    volatile int windowState;

    volatile boolean darkTheme;

    volatile int splitPosition;

    volatile String textFont;

    private Prefs(Preferences saved) {
        darkTheme = saved.getBoolean(Key.darkTheme, false);
        windowBounds = getBounds(saved, Key.windowBounds);
        windowState = saved.getInt(Key.windowState, -1);
        splitPosition = saved.getInt(Key.splitPosition, -1);
        textFont = saved.get(Key.textFont, Font.MONOSPACED + "-13");
    }

    public static Prefs load() {
        return new Prefs(prefsNode());
    }

    public void save() {
        Preferences store = prefsNode();
        store.putBoolean(Key.darkTheme, darkTheme);
        putBounds(store, Key.windowBounds, windowBounds);
        store.putInt(Key.windowState, windowState);
        store.putInt(Key.splitPosition, splitPosition);
        store.put(Key.textFont, textFont);
    }

    private static Preferences prefsNode() {
        return Preferences.userNodeForPackage(Prefs.class);
    }

    private static Rectangle getBounds(Preferences store, String key) {
        String str = store.get(key, null);
        if (str == null) return null;

        try {
            int[] array = Stream.of(str.split(","))
                                .mapToInt(Integer::parseInt)
                                .toArray();
            return new Rectangle(array[0], array[1], array[2], array[3]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            if (DEBUG) e.printStackTrace();
            return null;
        }
    }

    private static void putBounds(Preferences store, String key, Rectangle bounds) {
        if (bounds == null) return;

        int[] array = { bounds.x, bounds.y, bounds.width, bounds.height };
        store.put(key, String.join(",", IntStream.of(array)
                .mapToObj(Integer::toString).toArray(String[]::new)));
    }

    static <T> T getObject(Preferences store, String key, Class<T> klass) {
        byte[] byteArray = store.getByteArray(key, null);
        if (byteArray == null) return null;

        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
                ObjectInputStream objectInput = new ObjectInputStream(byteStream)) {
            Object value = objectInput.readObject();
            if (klass.isInstance(value)) {
                return klass.cast(value);
            } else if (DEBUG) {
                System.err.println("Not a " + klass.getName() + ": " + value);
            }
        } catch (ClassNotFoundException | IOException e) {
            if (DEBUG) e.printStackTrace();
        }
        return null;
    }

    static void putObject(Preferences store, String key, Object value) {
        if (value == null) return;

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(buf)) {
            objectOutput.writeObject(value);
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
            return;
        }
        store.putByteArray(key, buf.toByteArray());
    }

}
