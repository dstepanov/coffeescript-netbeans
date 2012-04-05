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

import java.io.OutputStream;
import java.nio.charset.Charset;
import org.netbeans.api.queries.FileEncodingQuery;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptUtils {

    public static void writeJS(final String js, String name, FileObject folder, Charset encoding) {
        try {
            FileObject file = folder.getFileObject(name, "js");
            if (file == null) {
                file = folder.createData(name, "js");
            }
            if (!file.asText().equals(js)) {
                OutputStream out = file.getOutputStream();
                try {
                    out.write(js.getBytes(encoding));
                    out.flush();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }

    public static void writeJSForCoffeeScriptFile(final String js, FileObject coffeeFile) {
        writeJS(js, coffeeFile.getName(), coffeeFile.getParent(), FileEncodingQuery.getEncoding(coffeeFile));
    }
}
