package coffeescript.nb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.mozilla.nb.javascript.CompilerEnvirons;
import org.mozilla.nb.javascript.ContextFactory;
import org.mozilla.nb.javascript.ErrorReporter;
import org.mozilla.nb.javascript.EvaluatorException;
import org.mozilla.nb.javascript.Parser;
import org.mozilla.nb.javascript.Token;
import org.mozilla.nb.javascript.TokenStream;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.openide.ErrorManager;
import static coffeescript.nb.CoffeeScriptTokenId.*;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptLexer extends CoffeeScriptLexerBase<CoffeeScriptTokenId> {

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
    private Parser parser;
    private TokenStream tokenStream;
    private final static Set<String> COFFEE_KEYWORDS = new HashSet<String>(Arrays.asList("undefined", "then", "unless", "until", "loop", "of", "by", "'when"));
    private final static Set<String> COFFEE_ALIASES = new HashSet<String>(Arrays.asList("and", "or", "is", "isnt", "not", "yes", "no", "on", "off"));

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
        tokenStream.fromState(info.state());

        // Ensure that the parser instance is pointing to the same tokenstream instance
        // such that its error handler etc. is synchronized
        parser.setTokenStream(tokenStream);
    }

    public void release() {
    }

    public Object state() {
        return tokenStream.toState();
    }

    protected org.netbeans.api.lexer.Token<CoffeeScriptTokenId> token(CoffeeScriptTokenId id) {
        String fixedText = id.fixedText();
        return (fixedText != null) ? tokenFactory.getFlyweightToken(id, fixedText) : super.token(id);
    }

    public org.netbeans.api.lexer.Token<CoffeeScriptTokenId> nextToken() {
        int c = input.read();
        switch (c) {
            case '"': {
                if (inputMatch("\"\"")) {
                    return balancedInterpolatedString("\"\"\"") ? token(STRING_LITERAL) : token(ERROR);
                } else {
                    return balancedInterpolatedString("\"") ? token(STRING_LITERAL) : token(ERROR);
                }
            }
            case '\'': {
                if (inputMatch("''")) {
                    return balancedString("'''") ? token(SIMPLE_STRING_LITERAL) : token(ERROR);
                } else {
                    return balancedString("'") ? token(SIMPLE_STRING_LITERAL) : token(ERROR);
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
                }
                break;
            }
            case '#': {
                if (inputNotMatch("###") && inputMatch("##")) {
                    return balancedString("###") ? token(BLOCK_COMMENT) : token(ERROR);
                } else {
                    while (true) {
                        c = input.read();
                        if (c == '\n' || c == LexerInput.EOF) {
                            return token(BLOCK_COMMENT);
                        }
                    }
                }
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
                return IDENTIFIER; // Dont show errors
            case Token.ERROR://          = -1, // well-known as the only code < EOF
                return ERROR;
            case Token.LINE_COMMENT:
                return LINE_COMMENT;
            case Token.BLOCK_COMMENT:
                return BLOCK_COMMENT;
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
                return FUNCTION;
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
                return ANY_KEYWORD;
            case Token.NUMBER:
                return FLOAT_LITERAL;
            case Token.STRING_BEGIN:
                return STRING_BEGIN;
            case Token.STRING:
                return STRING_LITERAL;
            case Token.STRING_END:
                return STRING_END;
            case Token.DIV:
                return NONUNARY_OP;
            case Token.ASSIGN_DIV:
                return ANY_OPERATOR;
            case Token.REGEXP_BEGIN:
                return REGEXP_BEGIN;
            case Token.REGEXP:
                return REGEXP_LITERAL;
            case Token.REGEXP_END:
                return REGEXP_END;
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
            case Token.DEC://            = 103,
                return ANY_OPERATOR;
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

    private static final class RhinoContext extends org.mozilla.nb.javascript.Context {

        public RhinoContext() {
            super(ContextFactory.getGlobal());
        }
    }
}
