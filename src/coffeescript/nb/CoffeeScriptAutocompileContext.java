package coffeescript.nb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptAutocompileContext {

    private Map<String, Boolean> autocompile = Collections.synchronizedMap(new HashMap<String, Boolean>());
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
        Boolean result = autocompile.get(file.getPath());
        return result == null ? false : result;
    }

    public void enableAutocompile(final FileObject file) {
        final String path = file.getPath();
        autocompile.put(path, Boolean.TRUE);
        file.addFileChangeListener(new FileChangeAdapter() {

            @Override
            public void fileRenamed(FileRenameEvent fe) {
                Boolean result = autocompile.remove(path);
                if (result != null) {
                    autocompile.put(fe.getFile().getPath(), result);
                }
            }
        });
    }

    public void disableAutocompile(FileObject file) {
        autocompile.remove(file.getPath());
    }
}
