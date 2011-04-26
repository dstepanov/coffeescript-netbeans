package coffeescript.nb;

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptStringLexer implements Lexer<CoffeeScriptStringTokenId> {
    
    private LexerInput input;
    private TokenFactory<CoffeeScriptStringTokenId> tokenFactory;
    private boolean inEmbedded;
    
    public CoffeeScriptStringLexer(LexerRestartInfo<CoffeeScriptStringTokenId> info) {
        this.input = info.input();
        this.tokenFactory = info.tokenFactory();
    }
    
    public Token<CoffeeScriptStringTokenId> nextToken() {
        if (inEmbedded) {
            while (true) {
                int c = input.read();
                if ((c == LexerInput.EOF) || (c == '}')) {
                    if (input.readLength() > 1) {
                        input.backup(1);
                        return tokenFactory.createToken(CoffeeScriptStringTokenId.EMBEDDED);
                    } else {
                        break;
                    }
                }
            }
        }
        while (true) {
            int ch = input.read();
            switch (ch) {
                case LexerInput.EOF:
                    if (input.readLength() > 0) {
                        return tokenFactory.createToken(CoffeeScriptStringTokenId.STRING);
                    } else {
                        return null;
                    }
                case '#':
                    if (input.read() == '{') {
                        inEmbedded = true;
                        return tokenFactory.createToken(CoffeeScriptStringTokenId.STRING);
                    }
            }
        }
    }
    
    public Object state() {
        return null;
    }
    
    public void release() {
    }
}
