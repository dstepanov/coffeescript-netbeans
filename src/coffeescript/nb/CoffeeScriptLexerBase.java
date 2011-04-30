package coffeescript.nb;

import java.util.Deque;
import java.util.LinkedList;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.TokenFactory;

/**
 *
 * @author Denis Stepanov
 */
public abstract class CoffeeScriptLexerBase<T extends TokenId> implements Lexer<T> {

    protected LexerInput input;
    protected TokenFactory<T> tokenFactory;

    public CoffeeScriptLexerBase(LexerInput input, TokenFactory<T> tokenFactory) {
        this.input = input;
        this.tokenFactory = tokenFactory;
    }

    protected boolean balancedString(String last) {
        while (true) {
            if (inputMatch(last)) {
                return true;
            }
            int c = input.read();
            if (c == '\\') {
                c = input.read();
            } else if (c == LexerInput.EOF) {
                return false;
            }
        }
    }

    protected boolean balancedInterpolatedString(String last) {
        Deque<Character> stack = new LinkedList<Character>();
        while (true) {
            if (stack.isEmpty() && inputMatch(last)) {
                return true;
            }
            boolean canBeInterpolated = stack.isEmpty() || !stack.isEmpty() && stack.element() == '"';
            boolean inInterpolation = stack.isEmpty() && last.endsWith("}") || !stack.isEmpty() && stack.element() == '}';
            int c = input.read();
            if (!stack.isEmpty() && stack.element() == c) {
                stack.poll();
            } else if (canBeInterpolated && c == '#' && inputMatch("{")) {
                stack.push('}');
            } else if (inInterpolation && (c == '"' || c == '\'' || c == '{')) {
                stack.push(c == '{' ? '}' : (char) c);
            } else if (c == '\\') {
                c = input.read();
            } else if (c == LexerInput.EOF) {
                return false;
            }
        }
    }

    protected boolean balancedRegex() {
        Deque<Character> stack = new LinkedList<Character>();
        while (true) {
            int c = input.read();
            if (stack.isEmpty() && c == '/') {
                return true;
            }
            if (!stack.isEmpty() && stack.element() == c) {
                stack.poll();
            } else if (c == '[') {
                stack.push(']');
            } else if (stack.isEmpty() && c == '\\') {
                // We don't need to escape things in square braces
                c = input.read();
            } else if (c == '\n' || c == LexerInput.EOF) {
                return false;
            }
        }
    }

    protected boolean balancedJSToken() {
        Deque<Character> stack = new LinkedList<Character>();
        while (true) {
            int c = input.read();
            if (stack.isEmpty() && c == '`') {
                return true;
            }
            if (!stack.isEmpty() && stack.element() == c) {
                stack.poll();
            } else if (c == '"' || c == '\'') {
                stack.push((char) c);
            } else if (c == '\\') {
                c = input.read();
            } else if (c == LexerInput.EOF) {
                return false;
            }
        }
    }

    protected Token<T> token(T token) {
        return tokenFactory.createToken(token);
    }

    protected boolean inputNotMatch(String string) {
        int readChars = 0;
        for (char c : string.toCharArray()) {
            readChars++;
            if (input.read() != c) {
                input.backup(readChars);
                return true;
            }
        }
        input.backup(readChars);
        return false;
    }

    protected boolean inputMatch(String string) {
        int readChars = 0;
        for (char c : string.toCharArray()) {
            readChars++;
            if (input.read() != c) {
                input.backup(readChars);
                return false;
            }
        }
        return true;
    }
}
