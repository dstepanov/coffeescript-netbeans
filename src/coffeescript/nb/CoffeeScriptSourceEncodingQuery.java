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

import coffeescript.nb.options.CoffeeScriptSettings;
import java.nio.charset.Charset;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Denis Stepanov
 */
public final class CoffeeScriptSourceEncodingQuery extends FileEncodingQueryImplementation
{
    private static final Charset UTF8 = Charset.forName("UTF8");

    @Override
    public Charset getEncoding(FileObject file)
    {
        return CoffeeScriptSettings.get().isUseUTF8Encoding() ? UTF8 : null;
    }
    
}
