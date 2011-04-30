package coffeescript.nb;

import org.netbeans.api.lexer.TokenId;
import static coffeescript.nb.CoffeeScriptTokenId.Category.*;

/**
 * 
 * @author Denis Stepanov
 */
public enum CoffeeScriptTokenId implements TokenId {

    ERROR(null, ERROR_CAT),
    NEW("new", KEYWORD_CAT),
    IDENTIFIER(null, IDENTIFIER_CAT),
    REGEXP(null, REGEXP_CAT),
    HEREGEX(null, REGEXP_CAT),
    FLOAT_LITERAL(null, NUMBER_CAT),
    STRING_LITERAL(null, STRING_CAT),
    SIMPLE_STRING_LITERAL(null, STRING_CAT),
    BOOL(null, STRING_CAT),
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
    FIELD(null, FIELD_CAT),
    NONUNARY_OP(null, OPERATOR_CAT);
    //
    private final String fixedText;
    private final String primaryCategory;

    CoffeeScriptTokenId(String fixedText, Category primaryCategory) {
        this.fixedText = fixedText;
        this.primaryCategory = primaryCategory.getName();
    }

    public String fixedText() {
        return fixedText;
    }

    public String primaryCategory() {
        return primaryCategory;
    }

    public static enum Category {

        COMMENT_CAT("comment"),
        KEYWORD_CAT("keyword"),
        REGEXP_CAT("mod-regexp"),
        STRING_CAT("string"),
        WHITESPACE_CAT("whitespace"),
        OPERATOR_CAT("operator"),
        SEPARATOR_CAT("separator"),
        ERROR_CAT("error"),
        NUMBER_CAT("number"),
        IDENTIFIER_CAT("identifier"),
        FIELD_CAT("field");
        private String name;

        private Category(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
