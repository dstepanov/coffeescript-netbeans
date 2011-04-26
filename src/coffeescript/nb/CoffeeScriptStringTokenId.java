package coffeescript.nb;

import org.netbeans.api.lexer.TokenId;

/**
 *
 * @author Denis Stepanov
 */
public enum CoffeeScriptStringTokenId implements TokenId {

    STRING("string"),
    EMBEDDED("embedded");
    //
    private String category;

    private CoffeeScriptStringTokenId(String category) {
        this.category = category;
    }

    public String primaryCategory() {
        return category;
    }
}
