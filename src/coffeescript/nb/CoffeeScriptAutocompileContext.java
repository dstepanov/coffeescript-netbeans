// Copyright 2011 Denis Stepanov
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
