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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.csl.api.StructureScanner;
import org.netbeans.modules.csl.spi.ParserResult;
import org.openide.util.Exceptions;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptStructureScanner implements StructureScanner {

    public List<? extends StructureItem> scan(ParserResult pr) {
        return Collections.emptyList();
    }

    public Map<String, List<OffsetRange>> folds(ParserResult pr) {
        if (pr == null) {
            return Collections.emptyMap();
        }
        try {
            Map<String, List<OffsetRange>> folds = new HashMap<String, List<OffsetRange>>();
            BaseDocument document = (BaseDocument) pr.getSnapshot().getSource().getDocument(true);
            TokenHierarchy<CoffeeScriptTokenId> th = (TokenHierarchy<CoffeeScriptTokenId>) pr.getSnapshot().getTokenHierarchy();
            TokenSequence<CoffeeScriptTokenId> ts = th.tokenSequence(CoffeeScriptLanguage.getLanguage());
            List<OffsetRange> ranges = new ArrayList<OffsetRange>();
            Deque<IdentRegion> indents = new ArrayDeque<IdentRegion>();
            while (ts.moveNext()) {
                Token<CoffeeScriptTokenId> token = ts.token();
                switch (token.id()) {
//                    case COMMENT:
//                        TokenSequence<CoffeeScriptTokenId> commentTS = ts.subSequence(ts.offset());
//                        int start = token.offset(th);
//                        int end = start + token.length();
//                        while (commentTS.moveNext()) {
//                            Token<CoffeeScriptTokenId> commentNextToken = commentTS.token();
//                            if (commentNextToken.id() == CoffeeScriptTokenId.COMMENT) {
//                                end = commentNextToken.offset(th) + commentNextToken.length();
//                                continue;
//                            }
//                            if (commentNextToken.id().getCategory() == CoffeeScriptTokenId.Category.WHITESPACE_CAT) {
//                                continue;
//                            }
//                            break;
//                        }
//                        addIndent(document, ranges, start, end);
//                        break;
                    case INDENT:
                        Integer indent = (Integer) token.getProperty("indent");
                        indents.push(new IdentRegion(token.offset(th), indent));
                        break;
                    case OUTDENT:
                        Integer outdent = (Integer) token.getProperty("indent");
                        int to = token.offset(th) + token.length();
                        addIndent(document, ranges, indents, outdent, to);
                        break;
                }
            }
            addIndent(document, ranges, indents, -1, document.getLength());
            folds.put("codeblocks", ranges);
            return folds;
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        return Collections.emptyMap();
    }

    private void addIndent(BaseDocument document, List<OffsetRange> ranges, Deque<IdentRegion> indents, Integer outdent, int end) throws BadLocationException {
        while (!indents.isEmpty() && (indents.peek().indent > outdent)) {
            IdentRegion identRegion = indents.pop();
            int from = Utilities.getFirstNonWhiteFwd(document, identRegion.start);
            int to = Utilities.getFirstNonWhiteBwd(document, end) + 1;
            addIndent(document, ranges, from, to);
        }
    }

    private void addIndent(BaseDocument document, List<OffsetRange> ranges, int start, int end) throws BadLocationException {
        if (Utilities.getRowCount(document, start, end) > 1) {
            ranges.add(new OffsetRange(start, end));
        }
    }

    public Configuration getConfiguration() {
        return new Configuration(false, false);
    }

    private static class IdentRegion {

        int start;
        int indent;

        public IdentRegion(int start, int indent) {
            this.start = start;
            this.indent = indent;
        }
    }
}
