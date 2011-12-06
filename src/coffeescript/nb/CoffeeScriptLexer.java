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
import org.mozilla.nb.javascript.CompilerEnvirons;
import org.mozilla.nb.javascript.ContextFactory;
import org.mozilla.nb.javascript.ErrorReporter;
import org.mozilla.nb.javascript.EvaluatorException;
import org.mozilla.nb.javascript.Parser;
import org.mozilla.nb.javascript.Token;
import org.mozilla.nb.javascript.TokenStream;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenPropertyProvider;
import org.openide.ErrorManager;
import static coffeescript.nb.CoffeeScriptTokenId.*;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptLexer extends CoffeeScriptLexerBase<CoffeeScriptTokenId> {

    private Parser parser;
    private TokenStream tokenStream;
    //
    private final static Set<String> COFFEE_KEYWORDS = new HashSet<String>(Arrays.asList("undefined", "then", "unless", "until", "loop", "of", "by", "when"));
    private final static Set<String> COFFEE_ALIASES = new HashSet<String>(Arrays.asList("and", "or", "is", "isnt", "not", "yes", "no", "on", "off"));
    private final static Set<CoffeeScriptTokenId> NOT_REGEX = EnumSet.of(NUMBER, REGEX, BOOL, INC, DEC, RBRACKET);
    private final static Set<CoffeeScriptTokenId> NOT_SPACED_REGEX = EnumSet.of(RPAREN, RBRACE, THIS, IDENTIFIER, STRING);
    //
    private final static Pattern REGEX_MATCH = Pattern.compile("^\\/(?![\\s=])[^\\/\\n\\\\]*(?:(?:\\\\[\\s\\S]|\\[[^\\]\\n\\\\]*(?:\\\\[\\s\\S][^\\]\\n\\\\]*)*])[^\\/\\n\\\\]*)*\\/[imgy]{0,4}(?!\\w)");

    static {
        NOT_SPACED_REGEX.addAll(NOT_REGEX);
    }
    // 
    private CoffeeScriptTokenId prevToken;
    private boolean prevSpaced;
    private boolean newLine;
    private int indent;

    public CoffeeScriptLexer(LexerRestartInfo<CoffeeScriptTokenId> info) {
        super(info.input(), info.tokenFactory());
        // TODO Use Rhino's scanner and TokenStream classes.
        // Unfortunately, they don't provide access... I'll need a hacked version of
        // Rhino!
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        ErrorReporter errorReporter =
                new ErrorReporter() {

                    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset, String id, Object params) {
                    }

                    public void error(String message, String sourceName, int line, String lineSource, int lineOffset, String id, Object params) {
                    }

                    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
                        return null;
                    }
                };

        RhinoContext ctx = new RhinoContext();
        compilerEnv.initFromContext(ctx);

        compilerEnv.setErrorReporter(errorReporter);
        compilerEnv.setGeneratingSource(false);
        compilerEnv.setGenerateDebugInfo(false);

        compilerEnv.setXmlAvailable(true);

        // The parser is NOT used for parsing here, but the Rhino scanner
        // calls into the parser for error messages. So we register our own error
        // handler for the parser and pass it into the tokenizer to handle errors.

        parser = new Parser(compilerEnv, errorReporter);
        tokenStream = new TokenStream(parser, null, null, "", 0);
        parser.setTokenStream(tokenStream);
        tokenStream.setInput(info.input());

        if (info.state() != null) {
            State state = (State) info.state();
            tokenStream.fromState(state.getTokenStreamState());
            prevToken = state.getPrevToken();
            prevSpaced = state.isPrevSpaced();
            newLine = state.isNewLine();
            indent = state.getIndent();
        }

        // Ensure that the parser instance is pointing to the same tokenstream instance
        // such that its error handler etc. is synchronized
        parser.setTokenStream(tokenStream);
    }

    public void release() {
    }

    public Object state() {
        return new State(tokenStream.toState(), prevToken, prevSpaced, newLine, indent);
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
        switch (c) {
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
                if (inputMatch("=")) {
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
        }
        input.backup(1);
        int token = nextRhinoToken();
        CoffeeScriptTokenId tokenType = getTokenId(token);
        if (input.readLength() < 1) {
            if (token == Token.EOF) {
                return null;
            }
        }
        return token(tokenType);
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

    private int nextRhinoToken() {
        try {
            return tokenStream.getToken() & Parser.CLEAR_TI_MASK;
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
        } catch (AssertionError ex) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
        }
        return org.mozilla.nb.javascript.Token.ERROR;
    }

    private CoffeeScriptTokenId getTokenId(int token) {
        String text = input.readText().toString();
        if (COFFEE_KEYWORDS.contains(text)) {
            return ANY_KEYWORD;
        }
        if (COFFEE_ALIASES.contains(text)) {
            return ANY_KEYWORD;
        }
        if (text.equals("@")) {
            int nextToken = nextRhinoToken();
            CoffeeScriptTokenId convertToken = convertToken(nextToken);
            if (convertToken == IDENTIFIER) {
                return FIELD;
            }
            return convertToken;
        }
        return convertToken(token);
    }

    private CoffeeScriptTokenId convertToken(int token) {
        switch (token) {
            case 65535: // SIGN ERRORS! Why does this happen?
                return ERROR; // Dont show errors
            case Token.ERROR://          = -1, // well-known as the only code < EOF
                return ERROR;
            case Token.LINE_COMMENT:
            case Token.BLOCK_COMMENT:
                return COMMENT;
            case Token.NEW:
                return NEW;
            case Token.DOT:
                return DOT;
            case Token.WHITESPACE://     = 153,
            case Token.EOF://            = 0,  // end of file token - (not EOF_CHAR)
                return WHITESPACE;
            case Token.EOL://            = 1,  // end of line
                return EOL;
            case Token.FUNCTION:
                return IDENTIFIER;
            case Token.THIS:
                return THIS;
            case Token.FOR:
                return FOR;
            case Token.IF:
                return IF;
            case Token.WHILE:
                return WHILE;
            case Token.ELSE:
                return ELSE;
            case Token.CASE:
                return CASE;
            case Token.DEFAULT:
                return DEFAULT;
            case Token.BREAK:
                return BREAK;
            case Token.SWITCH:
                return SWITCH;
            case Token.TRUE:
            case Token.FALSE:
                return BOOL;
            case Token.DO:
            case Token.WITH:
            case Token.CATCH:
            case Token.CONST:
            case Token.CONTINUE:
            case Token.DELPROP:
            case Token.EXPORT:

            case Token.FINALLY:
            case Token.IMPORT:
            case Token.IN:
            case Token.INSTANCEOF:
            case Token.NULL:
            case Token.RESERVED:
            case Token.RETURN:
            case Token.THROW:

            case Token.TRY:
            case Token.TYPEOF:
            case Token.UNDEFINED:
            case Token.VAR:
            case Token.VOID:
            case Token.GOTO:
            case Token.YIELD:
            case Token.LET:
            case Token.DEBUGGER:
                return ANY_KEYWORD;
            case Token.NUMBER:
                return NUMBER;
            case Token.STRING_BEGIN:
            case Token.STRING:
            case Token.STRING_END:
                return STRING;
            case Token.DIV:
                return DIV;
            case Token.ASSIGN_DIV:
                return ANY_OPERATOR;
            case Token.REGEXP_BEGIN:
            case Token.REGEXP:
            case Token.REGEXP_END:
                return REGEX;
            case Token.IFEQ://           = 6,
            case Token.IFNE://           = 7,
            case Token.BITOR://          = 9,
            case Token.BITXOR://         = 10,
            case Token.BITAND://         = 11,
            case Token.EQ://             = 12,
            case Token.NE://             = 13,
            case Token.LT://             = 14,
            case Token.LE://             = 15,
            case Token.GT://             = 16,
            case Token.GE://             = 17,
            case Token.LSH://            = 18,
            case Token.RSH://            = 19,
            case Token.URSH://           = 20,
            case Token.ADD://            = 21,
            case Token.SUB://            = 22,
            case Token.MUL://            = 23,
            case Token.MOD://            = 25,
            case Token.NOT://            = 26,
            case Token.BITNOT://         = 27,
            case Token.POS://            = 28,
            case Token.SHEQ://           = 45,   // shallow equality (===)
            case Token.SHNE://           = 46,   // shallow inequality (!==)
            case Token.ASSIGN://         = 86,  // simple assignment  (=)
            case Token.ASSIGN_BITOR://   = 87,  // |=
            case Token.ASSIGN_BITXOR://  = 88,  // ^=
            case Token.ASSIGN_BITAND://  = 89,  // |=
            case Token.ASSIGN_LSH://     = 90,  // <<=
            case Token.ASSIGN_RSH://     = 91,  // >>=
            case Token.ASSIGN_URSH://    = 92,  // >>>=
            case Token.ASSIGN_ADD://     = 93,  // +=
            case Token.ASSIGN_SUB://     = 94,  // -=
            case Token.ASSIGN_MUL://     = 95,  // *=
            case Token.ASSIGN_MOD://     = 97;  // %=
            case Token.OR://             = 100, // logical or (||)
            case Token.AND://            = 101, // logical and (&&)
            case Token.HOOK://           = 98, // conditional (?:)
                return NONUNARY_OP;
            case Token.COLON://          = 99,
                return COLON;
            // I don't want to treat it as a nonunary operator since formatting doesn't
            // handle it well yet
            case Token.COMMA://          = 85,  // comma operator
                return ANY_OPERATOR;

            case Token.NAME://           = 38,
                return IDENTIFIER;
            case Token.NEG://            = 29,
            case Token.INC://            = 102, // increment/decrement (++ --)
                return INC;
            case Token.DEC://            = 103,
                return DEC;
            case Token.ARRAYLIT://       = 63, // array literal
            case Token.OBJECTLIT://      = 64, // object literal
                // XXX What do I do about these?
                return IDENTIFIER;
            case Token.SEMI:
                return SEMI;
            case Token.LB:
                return LBRACKET;
            case Token.RB:
                return RBRACKET;
            case Token.LC:
                return LBRACE;
            case Token.RC:
                return RBRACE;
            case Token.LP:
                return LPAREN;
            case Token.RP:
                return RPAREN;
            default:
                return IDENTIFIER;
        }
    }

    private boolean isSpaceCharacter(int c) {
        if (c <= 127) {
            return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
        } else {
            return c == 0xA0 || Character.getType((char) c) == Character.SPACE_SEPARATOR;
        }
    }

    private static class State {

        final Object tokenStreamState;
        final CoffeeScriptTokenId prevToken;
        final boolean prevSpaced;
        boolean newLine;
        int indent;

        public State(Object tokenStreamState, CoffeeScriptTokenId prevToken, boolean prevSpaced, boolean newLine, int indent) {
            this.tokenStreamState = tokenStreamState;
            this.prevToken = prevToken;
            this.prevSpaced = prevSpaced;
            this.newLine = newLine;
            this.indent = indent;
        }

        public Object getTokenStreamState() {
            return tokenStreamState;
        }

        public CoffeeScriptTokenId getPrevToken() {
            return prevToken;
        }

        public boolean isPrevSpaced() {
            return prevSpaced;
        }

        public boolean isNewLine() {
            return newLine;
        }

        public int getIndent() {
            return indent;
        }
    }

    private static final class RhinoContext extends org.mozilla.nb.javascript.Context {

        public RhinoContext() {
            super(ContextFactory.getGlobal());
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
