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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            
            /*
            
            TokenHierarchy<CoffeeScriptTokenId> th = (TokenHierarchy<CoffeeScriptTokenId>) pr.getSnapshot().getTokenHierarchy();;
            TokenSequence<CoffeeScriptTokenId> ts = th.tokenSequence(CoffeeScriptLanguage.getLanguage());
            while (ts.moveNext()) {
            // TODO: read tokens to find fold ranges
            }
            
            */
            
            Map<String, List<OffsetRange>> folds = new HashMap<String, List<OffsetRange>>();
            List<OffsetRange> ranges = new ArrayList<OffsetRange>();
            BaseDocument document = (BaseDocument) pr.getSnapshot().getSource().getDocument(true);
            ranges.add(new OffsetRange(Utilities.getRowStart(document, 50), Utilities.getRowEnd(document, 200)));
            folds.put("codeblocks", ranges);
            return folds;
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        return Collections.emptyMap();
    }

    public Configuration getConfiguration() {
        return new Configuration(false, false);
    }
}
