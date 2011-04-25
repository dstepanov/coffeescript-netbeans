package coffeescript.nb;

import org.netbeans.api.lexer.TokenId;
import static coffeescript.nb.CoffeeScriptLexer.*;

/**
 * 
 * @author Denis Stepanov
 */
public enum CoffeeScriptTokenId implements TokenId {

    ERROR(null, ERROR_CAT),
    NEW("new", KEYWORD_CAT),
    IDENTIFIER(null, IDENTIFIER_CAT),
    REGEXP_LITERAL(null, REGEXP_CAT),
    FLOAT_LITERAL(null, NUMBER_CAT),
    STRING_LITERAL(null, STRING_CAT),
    WHITESPACE(null, WHITESPACE_CAT),
    EOL(null, WHITESPACE_CAT),
    LINE_COMMENT(null, COMMENT_CAT),
    BLOCK_COMMENT(null, COMMENT_CAT),
    LPAREN("(", SEPARATOR_CAT),
    RPAREN(")", SEPARATOR_CAT),
    LBRACE("{", SEPARATOR_CAT),
    RBRACE("}", SEPARATOR_CAT),
    LBRACKET("[", SEPARATOR_CAT),
    RBRACKET("]", SEPARATOR_CAT),
    STRING_BEGIN(null, STRING_CAT),
    STRING_END(null, STRING_CAT),
    REGEXP_BEGIN(null, REGEXP_CAT),
    REGEXP_END(null, REGEXP_CAT),
    ANY_KEYWORD(null, KEYWORD_CAT),
    ANY_OPERATOR(null, OPERATOR_CAT),
    DOT(null, OPERATOR_CAT),
    THIS("this", KEYWORD_CAT),
    FOR("for", KEYWORD_CAT),
    IF("if", KEYWORD_CAT),
    ELSE("else", KEYWORD_CAT),
    WHILE("while", KEYWORD_CAT),
    CASE("case", KEYWORD_CAT),
    DEFAULT("default", KEYWORD_CAT),
    BREAK("break", KEYWORD_CAT),
    SWITCH("switch", KEYWORD_CAT),
    COLON(":", OPERATOR_CAT),
    SEMI(";", OPERATOR_CAT),
    FUNCTION("function", KEYWORD_CAT),
    FIELD(null, FIELD_CAT),
    NONUNARY_OP(null, OPERATOR_CAT),
    EMBEDDED_RUBY(null, "");
    
    private final String fixedText;
    private final String primaryCategory;

    CoffeeScriptTokenId(String fixedText, String primaryCategory) {
        this.fixedText = fixedText;
        this.primaryCategory = primaryCategory;
    }

    public String fixedText() {
        return fixedText;
    }

    public String primaryCategory() {
        return primaryCategory;
    }
}
