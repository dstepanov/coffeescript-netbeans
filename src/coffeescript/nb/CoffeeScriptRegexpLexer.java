package coffeescript.nb;

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptRegexpLexer extends CoffeeScriptLexerBase<CoffeeScriptRegexpTokenId> {

    private boolean inEmbedded;

    public CoffeeScriptRegexpLexer(LexerRestartInfo<CoffeeScriptRegexpTokenId> info) {
        super(info.input(), info.tokenFactory());
        inEmbedded = info.state() instanceof Boolean ? (Boolean) info.state() : false;
    }

    public Token<CoffeeScriptRegexpTokenId> nextToken() {
        if (inEmbedded) {
            try {
                if (balancedInterpolatedString("}")) {
                    if (input.readLength() > 1) {
                        input.backup(1);
                        return token(CoffeeScriptRegexpTokenId.EMBEDDED);
                    } else if (input.readLength() == 0) {
                        return null;
                    }
                }
                return token(CoffeeScriptRegexpTokenId.REGEXP);
            } finally {
                inEmbedded = false;
            }
        }
        while (true) {
            int ch = input.read();
            switch (ch) {
                case LexerInput.EOF:
                    if (input.readLength() > 0) {
                        return token(CoffeeScriptRegexpTokenId.REGEXP);
                    } else {
                        return null;
                    }
                case '#':
                    if (inputMatch("{")) {
                        inEmbedded = true;
                        return token(CoffeeScriptRegexpTokenId.REGEXP);
                    }
            }
        }
    }

    public Object state() {
        return inEmbedded;
    }

    public void release() {
    }
}
