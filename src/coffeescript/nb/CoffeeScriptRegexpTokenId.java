package coffeescript.nb;

import org.netbeans.api.lexer.TokenId;

/**
 *
 * @author Denis Stepanov
 */
public enum CoffeeScriptRegexpTokenId implements TokenId {

    REGEXP("mod-regexp"),
    EMBEDDED("embedded");
    //
    private String category;

    private CoffeeScriptRegexpTokenId(String category) {
        this.category = category;
    }

    public String primaryCategory() {
        return category;
    }
}
