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
    REGEX(null, REGEXP_CAT),
    HEREGEX(null, REGEXP_CAT),
    NUMBER(null, NUMBER_CAT),
    STRING(null, STRING_CAT),
    SIMPLE_STRING(null, STRING_CAT),
    JSTOKEN(null, STRING_CAT),
    BOOL(null, KEYWORD_CAT),
    WHITESPACE(null, WHITESPACE_CAT),
    EOL(null, WHITESPACE_CAT),
    COMMENT(null, COMMENT_CAT),
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
    NONUNARY_OP(null, OPERATOR_CAT),
    DIV("/", OPERATOR_CAT),
    INC("++", OPERATOR_CAT),
    DEC("--", OPERATOR_CAT);
    //
    private final String fixedText;
    private final Category category;

    CoffeeScriptTokenId(String fixedText, Category category) {
        this.fixedText = fixedText;
        this.category = category;
    }

    public String fixedText() {
        return fixedText;
    }

    public String primaryCategory() {
        return category.getName();
    }

    public static enum Category {

        COMMENT_CAT("comment"),
        KEYWORD_CAT("keyword"),
        REGEXP_CAT("regexp"),
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
