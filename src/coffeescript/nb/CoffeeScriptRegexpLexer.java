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

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import static coffeescript.nb.CoffeeScriptRegexpTokenId.*;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptRegexpLexer extends CoffeeScriptLexerBase<CoffeeScriptRegexpTokenId> {

    private boolean inEmbedded;

    public CoffeeScriptRegexpLexer(LexerRestartInfo<CoffeeScriptRegexpTokenId> info) {
        super(info.input(), info.tokenFactory());
        inEmbedded = info.state() instanceof Boolean ? (Boolean) info.state() : false;
    }

    public Token<CoffeeScriptRegexpTokenId> nextToken() {
        if (inEmbedded) {
            try {
                if (balancedInterpolatedString("}")) {
                    if (input.readLength() > 1) {
                        input.backup(1);
                        return token(EMBEDDED);
                    } else if (input.readLength() == 0) {
                        return null;
                    }
                }
                return token(REGEXP);
            } finally {
                inEmbedded = false;
            }
        }
        while (true) {
            int ch = input.read();
            switch (ch) {
                case LexerInput.EOF:
                    if (input.readLength() > 0) {
                        return token(REGEXP);
                    } else {
                        return null;
                    }
                case '#':
                    if (inputMatch("{")) {
                        inEmbedded = true;
                        return token(REGEXP);
                    } else {
                        if (input.readLength() > 1) {
                            input.backup(1);
                            return token(REGEXP);
                        }
                        while (true) {
                            int c = input.read();
                            if (c == '\n' || c == LexerInput.EOF) {
                                return token(COMMENT);
                            }
                        }
                    }
            }
        }
    }

    public Object state() {
        return inEmbedded;
    }

    public void release() {
    }
}
