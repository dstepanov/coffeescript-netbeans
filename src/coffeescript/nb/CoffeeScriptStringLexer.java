package coffeescript.nb;

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptStringLexer extends CoffeeScriptLexerBase<CoffeeScriptStringTokenId> {

    private boolean inEmbedded;

    public CoffeeScriptStringLexer(LexerRestartInfo<CoffeeScriptStringTokenId> info) {
        super(info.input(), info.tokenFactory());
        inEmbedded = info.state() instanceof Boolean ? (Boolean) info.state() : false;
    }

    public Token<CoffeeScriptStringTokenId> nextToken() {
        if (inEmbedded) {
            try {
                if (balancedInterpolatedString("}")) {
                    if (input.readLength() > 1) {
                        input.backup(1);
                        return token(CoffeeScriptStringTokenId.EMBEDDED);
                    } else if (input.readLength() == 0) {
                        return null;
                    }
                }
                return token(CoffeeScriptStringTokenId.STRING);
            } finally {
                inEmbedded = false;
            }
        }
        while (true) {
            int ch = input.read();
            switch (ch) {
                case LexerInput.EOF:
                    if (input.readLength() > 0) {
                        return token(CoffeeScriptStringTokenId.STRING);
                    } else {
                        return null;
                    }
                case '\\':
                    input.read();
                    break;
                case '#':
                    if (inputMatch("{")) {
                        inEmbedded = true;
                        return token(CoffeeScriptStringTokenId.STRING);
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
