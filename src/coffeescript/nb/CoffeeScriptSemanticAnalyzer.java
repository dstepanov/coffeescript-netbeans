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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.ColoringAttributes;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.SemanticAnalyzer;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;

public class CoffeeScriptSemanticAnalyzer extends SemanticAnalyzer {

    private Map<OffsetRange, Set<ColoringAttributes>> highlights;

    @Override
    public Map<OffsetRange, Set<ColoringAttributes>> getHighlights() {
        return highlights;
    }

    @Override
    public void run(Parser.Result pr, SchedulerEvent se) {
        Map<OffsetRange, Set<ColoringAttributes>> newHighlights = new HashMap<OffsetRange, Set<ColoringAttributes>>();
        TokenHierarchy<CoffeeScriptTokenId> th = (TokenHierarchy<CoffeeScriptTokenId>) pr.getSnapshot().getTokenHierarchy();
        TokenSequence<CoffeeScriptTokenId> ts = th.tokenSequence(CoffeeScriptLanguage.getLanguage());
        int start = 0;
        while (ts.moveNext()) {
            Token<CoffeeScriptTokenId> token = ts.token();
            switch (token.id()) {
                case LBRACKET:
                    start =  ts.offset();
                    break;
                case RBRACKET:
                    ts.moveNext();
                    newHighlights.put(new OffsetRange(start, ts.offset()), ColoringAttributes.METHOD_SET);
                    ts.movePrevious();
                    break;
            }
        }
        highlights = newHighlights.isEmpty() ? null : newHighlights;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }
}
