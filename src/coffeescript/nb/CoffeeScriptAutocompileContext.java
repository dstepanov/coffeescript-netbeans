package coffeescript.nb;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.openide.filesystems.FileObject;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptAutocompileContext {

    private Map<FileObject, Boolean> autocompile = Collections.synchronizedMap(new WeakHashMap<FileObject, Boolean>());
    private static CoffeeScriptAutocompileContext INSTANCE;

    private CoffeeScriptAutocompileContext() {
    }

    public static synchronized CoffeeScriptAutocompileContext get() {
        if (INSTANCE == null) {
            INSTANCE = new CoffeeScriptAutocompileContext();
        }
        return INSTANCE;
    }

    public boolean isEnabled(FileObject file) {
        Boolean result = autocompile.get(file);
        return result == null ? false : result;
    }

    public void enableAutocompile(FileObject file) {
        autocompile.put(file, Boolean.TRUE);
    }

    public void disableAutocompile(FileObject file) {
        autocompile.remove(file);
    }
}
