package coffeescript.nb;

import java.util.Collection;
import java.util.EnumSet;
import org.netbeans.api.lexer.Language;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptLanguage extends LanguageHierarchy<CoffeeScriptTokenId> {

    public static final String MIME_TYPE = "text/coffeescript"; //NOI18N
    private static final Language<CoffeeScriptTokenId> LANGUAGE = new CoffeeScriptLanguage().language();

    public static final Language<CoffeeScriptTokenId> getLanguage() {
        return LANGUAGE;
    }

    private CoffeeScriptLanguage() {
    }

    protected Collection<CoffeeScriptTokenId> createTokenIds() {
        return EnumSet.allOf(CoffeeScriptTokenId.class);
    }

    @Override
    protected Lexer<CoffeeScriptTokenId> createLexer(LexerRestartInfo<CoffeeScriptTokenId> lri) {
        return CoffeeScriptLexer.create(lri);
    }

    @Override
    protected String mimeType() {
        return MIME_TYPE;
    }
}