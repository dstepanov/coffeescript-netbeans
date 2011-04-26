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
public class CoffeeScriptStringLanguage extends LanguageHierarchy<CoffeeScriptStringTokenId> {

    public static final String MIME_TYPE = "text/coffeescript-string"; //NOI18N
    private static final Language<CoffeeScriptStringTokenId> LANGUAGE = new CoffeeScriptStringLanguage().language();

    public static final Language<CoffeeScriptStringTokenId> getLanguage() {
        return LANGUAGE;
    }

    private CoffeeScriptStringLanguage() {
    }

    protected Collection<CoffeeScriptStringTokenId> createTokenIds() {
        return EnumSet.allOf(CoffeeScriptStringTokenId.class);
    }

    @Override
    protected Lexer<CoffeeScriptStringTokenId> createLexer(LexerRestartInfo<CoffeeScriptStringTokenId> lri) {
        return new CoffeeScriptStringLexer(lri);
    }

    @Override
    protected EmbeddingPresence embeddingPresence(CoffeeScriptStringTokenId id) {
        if (id == CoffeeScriptStringTokenId.EMBEDDED) {
            return EmbeddingPresence.ALWAYS_QUERY;
        }
        return null;
    }

    @Override
    protected LanguageEmbedding<?> embedding(Token<CoffeeScriptStringTokenId> token, LanguagePath languagePath, InputAttributes inputAttributes) {
        if (token.id() == CoffeeScriptStringTokenId.EMBEDDED) {
            return LanguageEmbedding.create(CoffeeScriptLanguage.getLanguage(), 0, 0);
        }
        return null;
    }

    @Override
    protected String mimeType() {
        return MIME_TYPE;
    }
}