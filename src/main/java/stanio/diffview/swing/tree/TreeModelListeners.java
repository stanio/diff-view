/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing.tree;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public class TreeModelListeners {

    private EventListenerList listenerList;

    public TreeModelListeners() {
        this(new EventListenerList());
    }

    public TreeModelListeners(EventListenerList listenerList) {
        this.listenerList = Objects.requireNonNull(listenerList);
    }

    public void add(TreeModelListener listener) {
        listenerList.add(TreeModelListener.class, listener);
    }

    public void remove(TreeModelListener listener) {
        listenerList.remove(TreeModelListener.class, listener);
    }

    public void notify(BiConsumer<TreeModelListener, TreeModelEvent> dispatcher,
                       Supplier<TreeModelEvent> eventSupplier) {
        notify(listenerList, dispatcher, eventSupplier);
    }

    public static void notify(EventListenerList listenerList,
            BiConsumer<TreeModelListener, TreeModelEvent> dispatcher,
            Supplier<TreeModelEvent> eventSupplier) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent event = null;

        // Process the listeners last to first, notifying those
        // that are interested in this event.
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (event == null) event = eventSupplier.get();
                dispatcher.accept((TreeModelListener) listeners[i + 1], event);
            }
        }
    }

}
