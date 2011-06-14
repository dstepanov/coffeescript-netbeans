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

import org.netbeans.api.lexer.Language;
import org.netbeans.modules.csl.api.Formatter;
import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
import org.netbeans.modules.csl.spi.LanguageRegistration;
import org.netbeans.modules.parsing.spi.Parser;

/**
 *
 * @author Denis Stepanov
 */
@LanguageRegistration(mimeType = CoffeeScriptLanguage.MIME_TYPE)
public class CoffeScriptLanguageRegistration extends DefaultLanguageConfig {

    @Override
    public String getLineCommentPrefix() {
        return "#";
    }

    @Override
    public Language getLexerLanguage() {
        return CoffeeScriptLanguage.getLanguage();
    }

    @Override
    public Parser getParser() {
        return new CoffeeScriptParser();
    }

    @Override
    public boolean hasFormatter() {
        return true;
    }

    @Override
    public Formatter getFormatter() {
        return new CoffeeScriptFormatter();
    }

    @Override
    public String getDisplayName() {
        return "CoffeeScript";
    }
}
