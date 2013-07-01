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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenPropertyProvider;
import static coffeescript.nb.CoffeeScriptTokenId.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptLexer extends CoffeeScriptLexerBase<CoffeeScriptTokenId> {

    private final static Set<String> COFFEE_ALIASES = new HashSet<String>(Arrays.asList("and", "or", "is", "isnt", "not", "yes", "no", "on", "off"));
    private final static Set<CoffeeScriptTokenId> NOT_REGEX = EnumSet.of(NUMBER, REGEX, BOOL, INC, DEC, RBRACKET);
    private final static Set<CoffeeScriptTokenId> NOT_SPACED_REGEX = EnumSet.of(RPAREN, RBRACE, THIS, IDENTIFIER, STRING);
    private final static Map<String, CoffeeScriptTokenId> TEXTID_TO_TOKEN = new HashMap<String, CoffeeScriptTokenId>();

    static {
        for (CoffeeScriptTokenId token : CoffeeScriptTokenId.values()) {
            if (token.getCategory() == Category.KEYWORD_CAT && token.fixedText() != null) {
                TEXTID_TO_TOKEN.put(token.fixedText(), token);
            }
        }
        for (String jsKeyword : Arrays.asList("true", "false", "null", "this", "new", "delete", "typeof", "in", "instanceof",
                "return", "throw", "break", "continue", "debugger", "if", "else", "switch", "for", "while", "do", "try", "catch", "finally",
                "extends", "super")) {
            TEXTID_TO_TOKEN.put(jsKeyword, ANY_KEYWORD);
        }
        TEXTID_TO_TOKEN.put("class", CLASS);
        for (String coffeeKeyword : Arrays.asList("undefined", "then", "unless", "until", "loop", "of", "by", "when")) {
            TEXTID_TO_TOKEN.put(coffeeKeyword, ANY_KEYWORD);
        }
        for (String coffeeAlias : COFFEE_ALIASES) {
            TEXTID_TO_TOKEN.put(coffeeAlias, ANY_KEYWORD);
        }
        TEXTID_TO_TOKEN.put("true", BOOL);
        TEXTID_TO_TOKEN.put("false", BOOL);
    }
    //
    private final static Pattern REGEX_MATCH = Pattern.compile("^\\/(?![\\s=])[^\\/\\n\\\\]*(?:(?:\\\\[\\s\\S]|\\[[^\\]\\n\\\\]*(?:\\\\[\\s\\S][^\\]\\n\\\\]*)*])[^\\/\\n\\\\]*)*\\/[imgy]{0,4}(?!\\w)");

    static {
        NOT_SPACED_REGEX.addAll(NOT_REGEX);
    }
    //
    private CoffeeScriptTokenId prevToken;
    private boolean func_defined = false;
    private boolean prevSpaced;
    private int indent;

    public CoffeeScriptLexer(LexerRestartInfo<CoffeeScriptTokenId> info) {
        super(info.input(), info.tokenFactory());
        if (info.state() != null) {
            State state = (State) info.state();
            prevToken = state.getPrevToken();
            prevSpaced = state.isPrevSpaced();
            indent = state.getIndent();
        }
    }

    public void release() {
    }

    public Object state() {
        return new State(prevToken, prevSpaced, indent);
    }

    protected org.netbeans.api.lexer.Token<CoffeeScriptTokenId> token(CoffeeScriptTokenId id) {
        if (id == WHITESPACE) {
            prevSpaced = true;
        } else {
            prevToken = id;
            prevSpaced = false;
        }

        switch (id) {
            case INDENT:
                return tokenFactory.createPropertyToken(id, input.readLength(), new IndentTokenProperty(indent));
            case OUTDENT:
                return tokenFactory.createPropertyToken(id, input.readLength(), new IndentTokenProperty(indent));
        }

        String fixedText = id.fixedText();
        return (fixedText != null) ? tokenFactory.getFlyweightToken(id, fixedText) : super.token(id);
    }

    public org.netbeans.api.lexer.Token<CoffeeScriptTokenId> nextToken() {
        int c;
        int lineAt = -1;
        while (true) {
            c = input.read();
            if (c == -1) {
                if (input.readLength() > 0) {
                    input.backup(1);
                    return indentToken(0);
                }
                return null;
            } else if (c == '\n') {
                lineAt = input.readLength();
            } else if (!isSpaceCharacter(c)) {
                if (input.readLength() > 1) {
                    input.backup(1);
                    if (lineAt == -1) {
                        return token(WHITESPACE);
                    }
                    return indentToken(input.readLength() - lineAt);
                }
                break;
            }
        }

        if (isDigit(c) || (c == '.' && isDigit(peek()))) {

            StringBuilder buffer = new StringBuilder();

            int base = 10;

            if (c == '0') {
                c = input.read();
                if (c == 'x' || c == 'X') {
                    base = 16;
                    c = input.read();
                } else if (isDigit(c)) {
                    base = 8;
                } else {
                    buffer.append('0');
                }
            }

            if (base == 16) {
                while (0 <= xDigitToInt(c, 0)) {
                    buffer.append((char) c);
                    c = input.read();
                }
            } else {
                while ('0' <= c && c <= '9') {
                    /*
                     * We permit 08 and 09 as decimal numbers, which
                     * makes our behavior a superset of the ECMA
                     * numeric grammar.  We might not always be so
                     * permissive, so we warn about it.
                     */
                    if (base == 8 && c >= '8') {
                        base = 10;
                    }
                    buffer.append((char) c);
                    c = input.read();
                }
            }

            boolean isInteger = true;

            if (base == 10 && (c == '.' || c == 'e' || c == 'E')) {
                isInteger = false;
                if (c == '.') {
                    do {
                        buffer.append((char) c);
                        c = input.read();
                    } while (isDigit(c));
                }
                if (c == 'e' || c == 'E') {
                    buffer.append((char) c);
                    c = input.read();
                    if (c == '+' || c == '-') {
                        buffer.append((char) c);
                        c = input.read();
                    }
                    if (!isDigit(c)) {
                        return token(ERROR);
                    }
                    do {
                        buffer.append((char) c);
                        c = input.read();
                    } while (isDigit(c));
                }
            }

            input.backup(1);

            String numString = buffer.toString();
            if (base == 10 && !isInteger) {
                try {
                    Double.valueOf(numString).doubleValue();
                } catch (NumberFormatException ex) {
                    return token(ERROR);
                }
            }
            return token(NUMBER);
        }

        boolean startsWithAt = false;
        boolean identifierStart;
        boolean isUnicodeEscapeStart = false;
        if (c == '\\') {
            c = input.read();
            if (c == 'u') {
                identifierStart = true;
                isUnicodeEscapeStart = true;
            } else {
                identifierStart = false;
                input.backup(1);
                c = '\\';
            }
        } else {
            if (c == '@') {
                c = input.read();
                startsWithAt = true;
            }
            identifierStart = Character.isJavaIdentifierStart((char) c);
        }
        if (startsWithAt && !identifierStart) {
            return token(AT);
        }
        if (identifierStart) {
            StringBuilder buffer = new StringBuilder();
            if (!isUnicodeEscapeStart) {
                buffer.append((char) c);
            }
            boolean containsEscape = isUnicodeEscapeStart;
            while (true) {
                if (isUnicodeEscapeStart) {
                    // strictly speaking we should probably push-back
                    // all the bad characters if the <backslash>uXXXX
                    // sequence is malformed. But since there isn't a
                    // correct context(is there?) for a bad Unicode
                    // escape sequence in an identifier, we can report
                    // an error here.
                    int escapeVal = 0;
                    for (int i = 0; i != 4; ++i) {
                        c = input.read();
                        escapeVal = xDigitToInt(c, escapeVal);
                        // Next check takes care about c < 0 and bad escape
                        if (escapeVal < 0) {
                            break;
                        }
                    }
                    if (escapeVal < 0) {
                        return token(ERROR);
                    }
                    buffer.append((char) escapeVal);

                    isUnicodeEscapeStart = false;
                } else {
                    c = input.read();
                    if (c == '\\') {
                        c = input.read();
                        if (c == 'u') {
                            isUnicodeEscapeStart = true;
                            containsEscape = true;
                        } else {
                            return token(ERROR);
                        }
                    } else {
                        if (c == LexerInput.EOF || !Character.isJavaIdentifierPart((char) c)) {
                            break;
                        }
                        buffer.append((char) c);
                    }
                }
            }

            input.backup(1);

            if (startsWithAt) {
                return token(FIELD);
            }

            if (EnumSet.of(DOT, QDOT, DOUBLE_COLON).contains(prevToken)) {
                return token(IDENTIFIER);
            }

            int reads = 0;

            c = input.read();
            reads++;
            while(isSpaceCharacter(c)) {
              c = input.read();
              reads++;
            }
            if(c == ':' || c == '=') {
                while(true) {
                    c = input.read();
                    reads++;
                    if(c == '-' || c == '=') {
                        c = input.read();
                        reads++;
                        if(c == '>') {
                          input.backup(reads);
                          func_defined = true;
                          return token(FUNCTION);
                        }
                    }
                    if(c == '\n' || c == ':' || c == '=') {
                      break;
                    }
                }
            }
            input.backup(reads);

            String text = buffer.toString();
            if (!containsEscape) {
                CoffeeScriptTokenId token = TEXTID_TO_TOKEN.get(text);
                if (token != null) {
                    return token(token);
                }
                if ("own".equals(text) && prevToken == CoffeeScriptTokenId.FOR) {
                    return token(ANY_KEYWORD);
                }
            }
            return token(IDENTIFIER);
        }

        switch (c) {
            case ';':
                return token(SEMI);
            case '(':
                if(func_defined) {
                  return token (FLPAREN);
                } else {
                  return token(LPAREN);
                }
            case ')':
                if(func_defined) {
                  return token (FRPAREN);
                } else {
                  return token(RPAREN);
                }
            case '{':
                return token(LBRACE);
            case '}':
                return token(RBRACE);
            case '[':
                return token(LBRACKET);
            case ']':
                return token(RBRACKET);
            case '\\':
                return token(ANY_OPERATOR);
            case '"': {
                if (inputMatch("\"\"")) {
                    return balancedInterpolatedString("\"\"\"") ? token(STRING) : token(ERROR);
                } else {
                    return balancedInterpolatedString("\"") ? token(STRING) : token(ERROR);
                }
            }
            case '\'': {
                if (inputMatch("''")) {
                    return balancedString("'''") ? token(SIMPLE_STRING) : token(ERROR);
                } else {
                    return balancedString("'") ? token(SIMPLE_STRING) : token(ERROR);
                }
            }
            case '/': {
                if (inputMatch("//")) {
                    if (balancedInterpolatedString("///")) {
                        while (true) {
                            c = input.read();
                            if (c == 'i' || c == 'm' || c == 'g' || c == 'y') {
                                continue;
                            } else {
                                input.backup(1);
                                break;
                            }
                        }
                        return token(HEREGEX);
                    } else {
                        return token(ERROR);
                    }
                } else if (prevToken != null) {
                    Set<CoffeeScriptTokenId> notRegex = prevSpaced ? NOT_REGEX : NOT_SPACED_REGEX;
                    if (!notRegex.contains(prevToken)) {
                        if (balancedRegex()) {
                            while (true) {
                                c = input.read();
                                if (c == 'i' || c == 'm' || c == 'g' || c == 'y') {
                                    continue;
                                } else {
                                    input.backup(1);
                                    break;
                                }
                            }
                            if (REGEX_MATCH.matcher(input.readText()).matches()) {
                                return token(REGEX);
                            }
                        }
                        input.backup(input.readLength() - 1);
                    }

                }
                if (inputMatch('=')) {
                    return token(ANY_OPERATOR);
                }
                return token(DIV);
            }
            case '#': {
                if (inputNotMatch("###") && inputMatch("##")) {
                    return balancedString("###") ? token(COMMENT) : token(ERROR);
                } else {
                    while (true) {
                        c = input.read();
                        if (c == '\n') {
                            input.backup(1);
                            return token(COMMENT);
                        }
                        if (c == LexerInput.EOF) {
                            return token(COMMENT);
                        }
                    }
                }
            }
            case '`': {
                return balancedJSToken() ? token(JSTOKEN) : token(ERROR);
            }
            case '.': {
                return token(DOT);
            }
            case '?': {
                return inputMatch('.') ? token(QDOT) : token(QM);
            }
            case ':': {
                return inputMatch(':') ? token(DOUBLE_COLON) : token(COLON);
            }
            case '+': {
                return inputMatch('+') ? token(INC) : token(ANY_OPERATOR);
            }
            case '-': {
                if(peek() == '>') {
                  input.read();
                  func_defined = false;
                  return token(ARROW);
                }
                return inputMatch('-') ? token(DEC) : token(ANY_OPERATOR);
            }
            case '=': {
              if(peek() == '>') {
                input.read();
                func_defined = false;
                return token(ARROW);
              }
            }
        }
        return token(ANY_OPERATOR);
    }

    private org.netbeans.api.lexer.Token<CoffeeScriptTokenId> indentToken(int lineIndent) {
        if (lineIndent < indent) {
            indent = lineIndent;
            return token(OUTDENT);
        } else if (lineIndent > indent) {
            indent = lineIndent;
            return token(INDENT);
        }
        return token(WHITESPACE);
    }

    /**
     * If character <tt>c</tt> is a hexadecimal digit, return
     * <tt>accumulator</tt> * 16 plus corresponding number. Otherise return -1.
     */
    public static int xDigitToInt(int c, int accumulator) {
        check:
        {
            // Use 0..9 < A..Z < a..z
            if (c <= '9') {
                c -= '0';
                if (0 <= c) {
                    break check;
                }
            } else if (c <= 'F') {
                if ('A' <= c) {
                    c -= ('A' - 10);
                    break check;
                }
            } else if (c <= 'f') {
                if ('a' <= c) {
                    c -= ('a' - 10);
                    break check;
                }
            }
            return -1;
        }
        return (accumulator << 4) | c;
    }

    static boolean isDigit(int c) {
        return '0' <= c && c <= '9';
    }

    private boolean isSpaceCharacter(int c) {
        if (c <= 127) {
            return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
        } else {
            return c == 0xA0 || Character.getType((char) c) == Character.SPACE_SEPARATOR;
        }
    }

    private static class State {

        final CoffeeScriptTokenId prevToken;
        final boolean prevSpaced;
        int indent;

        public State(CoffeeScriptTokenId prevToken, boolean prevSpaced, int indent) {
            this.prevToken = prevToken;
            this.prevSpaced = prevSpaced;
            this.indent = indent;
        }

        public CoffeeScriptTokenId getPrevToken() {
            return prevToken;
        }

        public boolean isPrevSpaced() {
            return prevSpaced;
        }

        public int getIndent() {
            return indent;
        }
    }

    private static class IndentTokenProperty implements TokenPropertyProvider<CoffeeScriptTokenId> {

        private final int indent;

        public IndentTokenProperty(int indent) {
            this.indent = indent;
        }

        public Object getValue(org.netbeans.api.lexer.Token<CoffeeScriptTokenId> token, Object key) {
            if ("indent".equals(key)) {
                return indent;
            }
            return null;
        }
    }
}
