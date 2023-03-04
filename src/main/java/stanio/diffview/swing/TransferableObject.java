/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview.swing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * {@code Transferable} of a JVM-local object reference.
 *
 * @see  DataFlavor#javaJVMLocalObjectMimeType
 * @see  <a href="https://docs.oracle.com/javase/tutorial/uiswing/dnd/index.html"
 *              >Drag and Drop and Data Transfer</a> <i>(The Java&trade; Tutorials)</i>
 */
public class TransferableObject implements Transferable {

    private static Map<Class<?>, DataFlavor> flavorCache = new ConcurrentHashMap<>();

    protected final Object value;

    protected final DataFlavor flavor;

    public TransferableObject(Object value) {
        this(value, value.getClass());
    }

    public TransferableObject(Object value, Class<?> repClass) {
        this(value, flavorFor(repClass));
    }

    public TransferableObject(Object value, DataFlavor flavor) {
        this.value = value;
        this.flavor = flavor;
    }

    public static DataFlavor flavorFor(Class<?> klass) {
        return flavorCache.computeIfAbsent(klass, k -> {
            try {
                return new DataFlavor(DataFlavor
                        .javaJVMLocalObjectMimeType + "; class=" + k.getName(),
                        null, k.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new InternalError(e);
            }
        });
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { flavor };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return this.flavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (isDataFlavorSupported(flavor)) {
            return value;
        }
        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public String toString() {
        return "TransferableObject(value=" + value + ", flavor=" + flavor + ")";
    }

}