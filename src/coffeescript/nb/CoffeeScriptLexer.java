package coffeescript.nb;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import org.mozilla.javascript.Kit;
import org.mozilla.nb.javascript.CompilerEnvirons;
import org.mozilla.nb.javascript.ContextFactory;
import org.mozilla.nb.javascript.ErrorReporter;
import org.mozilla.nb.javascript.EvaluatorException;
import org.mozilla.nb.javascript.Parser;
import org.mozilla.nb.javascript.Token;
import org.mozilla.nb.javascript.TokenStream;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;
import org.openide.ErrorManager;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptLexer implements Lexer<CoffeeScriptTokenId> {

    public static final String COMMENT_CAT = "comment";
    public static final String KEYWORD_CAT = "keyword"; // NOI18N
    public static final String REGEXP_CAT = "mod-regexp"; // NOI18N
    public static final String STRING_CAT = "string"; // NOI18N
    public static final String WHITESPACE_CAT = "whitespace"; // NOI18N
    public static final String OPERATOR_CAT = "operator"; // NOI18N
    public static final String SEPARATOR_CAT = "separator"; // NOI18N
    public static final String ERROR_CAT = "error"; // NOI18N
    public static final String NUMBER_CAT = "number"; // NOI18N
    public static final String IDENTIFIER_CAT = "identifier"; // NOI18N
    public static final String FIELD_CAT = "field"; // NOI18N
    /** This is still not working; I wonder if release() is called correctly at all times...*/
    private static final boolean REUSE_LEXERS = false;
    private LexerInput input;
    private TokenFactory<CoffeeScriptTokenId> tokenFactory;
    private Parser parser;
    private TokenStream tokenStream;
    private static CoffeeScriptLexer cachedLexer;
    private final static Set<String> COFFEE_KEYWORDS = new HashSet<String>(Arrays.asList("undefined", "then", "unless", "until", "loop", "of", "by", "'when"));
    private final static Set<String> COFFEE_ALIASES = new HashSet<String>(Arrays.asList("and", "or", "is", "isnt", "not", "yes", "no", "on", "off"));

    private CoffeeScriptLexer(LexerRestartInfo<CoffeeScriptTokenId> info) {
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

//        final int targetVersion = SupportedBrowsers.getInstance().getLanguageVersion();
//        compilerEnv.setLanguageVersion(targetVersion);

//        if (targetVersion >= Context.VERSION_1_7) {
//         Let's try E4X... why not?
        compilerEnv.setXmlAvailable(true);
//        }
        // XXX What do I set here: compilerEnv.setReservedKeywordAsIdentifier();

        // The parser is NOT used for parsing here, but the Rhino scanner
        // calls into the parser for error messages. So we register our own error
        // handler for the parser and pass it into the tokenizer to handle errors.
        parser = new Parser(compilerEnv, errorReporter);

        tokenStream = new TokenStream(parser, null, null, "", 0);
    }

    public static synchronized CoffeeScriptLexer create(LexerRestartInfo<CoffeeScriptTokenId> info) {
        CoffeeScriptLexer lexer = cachedLexer;

        if (lexer == null) {
            lexer = new CoffeeScriptLexer(info);
        }

        lexer.restart(info);

        return lexer;
    }

    void restart(LexerRestartInfo<CoffeeScriptTokenId> info) {
        input = info.input();
        tokenFactory = info.tokenFactory();
        tokenStream.setInput(info.input());
        Object state = info.state();
        tokenStream.fromState(state);

        // Ensure that the parser instance is pointing to the same tokenstream instance
        // such that its error handler etc. is synchronized
        parser.setTokenStream(tokenStream);
    }

    public void release() {
        if (REUSE_LEXERS) {
            // Possibly reset the structures that could cause memory leaks
            synchronized (CoffeeScriptLexer.class) {
                cachedLexer = this;
            }
        }
    }

    public Object state() {
        return tokenStream.toState();
    }

    private org.netbeans.api.lexer.Token<CoffeeScriptTokenId> token(CoffeeScriptTokenId id, int length) {
        String fixedText = id.fixedText();
        return (fixedText != null) ? tokenFactory.getFlyweightToken(id, fixedText)
                : tokenFactory.createToken(id, length);
    }

    public org.netbeans.api.lexer.Token<CoffeeScriptTokenId> nextToken() {
        int c = input.read();
        switch (c) {
            case '"': {
                if (inputMatch("\"\"")) {
                    if (balancedInterpolatedString("\"\"\"")) {
                        return tokenFactory.createToken(CoffeeScriptTokenId.STRING_LITERAL);
                    } else {
                        return tokenFactory.createToken(CoffeeScriptTokenId.ERROR);
                    }
                } else {
                    if (balancedInterpolatedString("\"")) {
                        return tokenFactory.createToken(CoffeeScriptTokenId.STRING_LITERAL);
                    } else {
                        return tokenFactory.createToken(CoffeeScriptTokenId.ERROR);
                    }
                }
            }
            case '\'': {
                if (inputMatch("''")) {
                    if (balancedInterpolatedString("'''")) {
                        return tokenFactory.createToken(CoffeeScriptTokenId.STRING_LITERAL);
                    } else {
                        return tokenFactory.createToken(CoffeeScriptTokenId.ERROR);
                    }
                } else {
                    if (balancedString("'")) {
                        return tokenFactory.createToken(CoffeeScriptTokenId.SIMPLE_STRING_LITERAL);
                    } else {
                        return tokenFactory.createToken(CoffeeScriptTokenId.ERROR);
                    }
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
                        return tokenFactory.createToken(CoffeeScriptTokenId.HEREGEX);
                    } else {
                        return tokenFactory.createToken(CoffeeScriptTokenId.ERROR);
                    }
                }
                break;
            }
            case '#': {
                if (inputNotMatch("###") && inputMatch("##")) {
                    if (balancedString("###")) {
                        return tokenFactory.createToken(CoffeeScriptTokenId.BLOCK_COMMENT);
                    } else {
                        return tokenFactory.createToken(CoffeeScriptTokenId.ERROR);
                    }
                } else {
                    while (true) {
                        c = input.read();
                        if (c == '\n' || c == LexerInput.EOF) {
                            return tokenFactory.createToken(CoffeeScriptTokenId.BLOCK_COMMENT);
                        }
                    }
                }
            }
        }
        input.backup(1);
        int token = readToken();
        CoffeeScriptTokenId tokenType = getTokenId(token);
        int tokenLength = input.readLength();
        if (tokenLength < 1) {
            if (token == Token.EOF) {
                return null;
            }
        }
        return token(tokenType, tokenLength);
    }

    private boolean balancedString(String last) {
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

    private boolean balancedInterpolatedString(String last) {
        Deque<Character> stack = new LinkedList<Character>();
        while (true) {
            if (stack.isEmpty() && inputMatch(last)) {
                return true;
            }
            int c = input.read();
            if (!stack.isEmpty() && stack.element() == c) {
                stack.poll();
            } else if (c == '#' && inputMatch("{")) {
                stack.push('}');
            } else if (c == '\\') {
                c = input.read();
            } else if (c == LexerInput.EOF) {
                return false;
            }
        }
    }

    private boolean inputNotMatch(String string) {
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

    private boolean inputMatch(String string) {
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

    private int readToken() {
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
            return CoffeeScriptTokenId.ANY_KEYWORD;
        }
        if (COFFEE_ALIASES.contains(text)) {
            return CoffeeScriptTokenId.ANY_KEYWORD;
        }
        if (text.equals("@")) {
            int nextToken = readToken();
            CoffeeScriptTokenId convertToken = convertToken(nextToken);
            if (convertToken == CoffeeScriptTokenId.IDENTIFIER) {
                return CoffeeScriptTokenId.FIELD;
            }
            return convertToken;
        }
        return convertToken(token);
    }

    private CoffeeScriptTokenId convertToken(int token) {
        // If you add any new token types here, remember to update #getRelevantTokenTypes below
        switch (token) {
            case 65535: // SIGN ERRORS! Why does this happen?
                return CoffeeScriptTokenId.IDENTIFIER; // Dont show errors
            case Token.ERROR://          = -1, // well-known as the only code < EOF
                return CoffeeScriptTokenId.ERROR;
            case Token.LINE_COMMENT:
                return CoffeeScriptTokenId.LINE_COMMENT;
            case Token.BLOCK_COMMENT:
                return CoffeeScriptTokenId.BLOCK_COMMENT;
            case Token.NEW:
                return CoffeeScriptTokenId.NEW;
            case Token.DOT:
                return CoffeeScriptTokenId.DOT;
            case Token.WHITESPACE://     = 153,
            case Token.EOF://            = 0,  // end of file token - (not EOF_CHAR)
                return CoffeeScriptTokenId.WHITESPACE;
            case Token.EOL://            = 1,  // end of line
                return CoffeeScriptTokenId.EOL;
            case Token.FUNCTION:
                return CoffeeScriptTokenId.FUNCTION;
            case Token.THIS:
                return CoffeeScriptTokenId.THIS;
            case Token.FOR:
                return CoffeeScriptTokenId.FOR;
            case Token.IF:
                return CoffeeScriptTokenId.IF;
            case Token.WHILE:
                return CoffeeScriptTokenId.WHILE;
            case Token.ELSE:
                return CoffeeScriptTokenId.ELSE;
            case Token.CASE:
                return CoffeeScriptTokenId.CASE;
            case Token.DEFAULT:
                return CoffeeScriptTokenId.DEFAULT;
            case Token.BREAK:
                return CoffeeScriptTokenId.BREAK;
            case Token.SWITCH:
                return CoffeeScriptTokenId.SWITCH;
            case Token.DO:
            case Token.WITH:
            case Token.CATCH:
            case Token.CONST:
            case Token.CONTINUE:
            case Token.DELPROP:
            case Token.EXPORT:
            case Token.FALSE:
            case Token.FINALLY:
            case Token.IMPORT:
            case Token.IN:
            case Token.INSTANCEOF:
            case Token.NULL:
            case Token.RESERVED:
            case Token.RETURN:
            case Token.THROW:
            case Token.TRUE:
            case Token.TRY:
            case Token.TYPEOF:
            case Token.UNDEFINED:
            case Token.VAR:
            case Token.VOID:
            case Token.GOTO:
            case Token.YIELD:
            case Token.LET:
            case Token.DEBUGGER:
                return CoffeeScriptTokenId.ANY_KEYWORD;
            case Token.NUMBER:
                return CoffeeScriptTokenId.FLOAT_LITERAL;
            case Token.STRING_BEGIN:
                return CoffeeScriptTokenId.STRING_BEGIN;
            case Token.STRING:
                return CoffeeScriptTokenId.STRING_LITERAL;
            case Token.STRING_END:
                return CoffeeScriptTokenId.STRING_END;
            case Token.DIV:
                return CoffeeScriptTokenId.NONUNARY_OP;
            case Token.ASSIGN_DIV:
                return CoffeeScriptTokenId.ANY_OPERATOR;
            case Token.REGEXP_BEGIN:
                return CoffeeScriptTokenId.REGEXP_BEGIN;
            case Token.REGEXP:
                return CoffeeScriptTokenId.REGEXP_LITERAL;
            case Token.REGEXP_END:
                return CoffeeScriptTokenId.REGEXP_END;
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
                return CoffeeScriptTokenId.NONUNARY_OP;
            case Token.COLON://          = 99,
                return CoffeeScriptTokenId.COLON;
            // I don't want to treat it as a nonunary operator since formatting doesn't
            // handle it well yet
            case Token.COMMA://          = 85,  // comma operator
                return CoffeeScriptTokenId.ANY_OPERATOR;

            case Token.NAME://           = 38,
                return CoffeeScriptTokenId.IDENTIFIER;
            case Token.NEG://            = 29,
            case Token.INC://            = 102, // increment/decrement (++ --)
            case Token.DEC://            = 103,
                return CoffeeScriptTokenId.ANY_OPERATOR;
            case Token.ARRAYLIT://       = 63, // array literal
            case Token.OBJECTLIT://      = 64, // object literal
                // XXX What do I do about these?
                return CoffeeScriptTokenId.IDENTIFIER;
            case Token.SEMI:
                return CoffeeScriptTokenId.SEMI;
            case Token.LB:
                return CoffeeScriptTokenId.LBRACKET;
            case Token.RB:
                return CoffeeScriptTokenId.RBRACKET;
            case Token.LC:
                return CoffeeScriptTokenId.LBRACE;
            case Token.RC:
                return CoffeeScriptTokenId.RBRACE;
            case Token.LP:
                return CoffeeScriptTokenId.LPAREN;
            case Token.RP:
                return CoffeeScriptTokenId.RPAREN;
            default:
                return CoffeeScriptTokenId.IDENTIFIER;
        }
    }

    private static final class RhinoContext extends org.mozilla.nb.javascript.Context {

        public RhinoContext() {
            super(ContextFactory.getGlobal());
        }
    }
}
