package coffeescript.nb;

import java.util.Collection;
import java.util.EnumSet;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.EmbeddingPresence;
import org.netbeans.spi.lexer.LanguageEmbedding;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptRegexpLanguage extends LanguageHierarchy<CoffeeScriptRegexpTokenId> {

    public static final String MIME_TYPE = "text/coffeescript-regexp"; //NOI18N
    private static final Language<CoffeeScriptRegexpTokenId> LANGUAGE = new CoffeeScriptRegexpLanguage().language();

    public static final Language<CoffeeScriptRegexpTokenId> getLanguage() {
        return LANGUAGE;
    }

    private CoffeeScriptRegexpLanguage() {
    }

    protected Collection<CoffeeScriptRegexpTokenId> createTokenIds() {
        return EnumSet.allOf(CoffeeScriptRegexpTokenId.class);
    }

    @Override
    protected Lexer<CoffeeScriptRegexpTokenId> createLexer(LexerRestartInfo<CoffeeScriptRegexpTokenId> lri) {
        return new CoffeeScriptRegexpLexer(lri);
    }

    @Override
    protected EmbeddingPresence embeddingPresence(CoffeeScriptRegexpTokenId id) {
        if (id == CoffeeScriptRegexpTokenId.EMBEDDED) {
            return EmbeddingPresence.ALWAYS_QUERY;
        }
        return null;
    }

    @Override
    protected LanguageEmbedding<?> embedding(Token<CoffeeScriptRegexpTokenId> token, LanguagePath languagePath, InputAttributes inputAttributes) {
        if (token.id() == CoffeeScriptRegexpTokenId.EMBEDDED) {
            return LanguageEmbedding.create(CoffeeScriptLanguage.getLanguage(), 0, 0);
        }
        return null;
    }

    @Override
    protected String mimeType() {
        return MIME_TYPE;
    }
}