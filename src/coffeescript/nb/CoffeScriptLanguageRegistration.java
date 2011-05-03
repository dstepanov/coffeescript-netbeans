package coffeescript.nb;

import org.netbeans.api.lexer.Language;
import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
import org.netbeans.modules.csl.spi.LanguageRegistration;

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
    public String getDisplayName() {
        return "CoffeeScript";
    }
}
