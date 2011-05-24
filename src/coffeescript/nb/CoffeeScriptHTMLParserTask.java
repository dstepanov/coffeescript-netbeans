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

import java.util.Collection;
import java.util.Collections;
import javax.swing.text.Document;
import org.netbeans.api.html.lexer.HTMLTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptHTMLParserTask extends ParserResultTask<Result> {

    @Override
    public void run(Result result, SchedulerEvent event) {
        TokenHierarchy<Document> th = TokenHierarchy.get(result.getSnapshot().getSource().getDocument(true));
        extractCoffeeScriptFromHtml(th.tokenSequence(HTMLTokenId.language()));
    }

    private void extractCoffeeScriptFromHtml(TokenSequence<? extends HTMLTokenId> ts) {
        boolean inCoffeeScript = false;
        ts.moveStart();
        while (ts.moveNext()) {
            Token<? extends HTMLTokenId> htmlToken = ts.token();
            HTMLTokenId htmlId = htmlToken.id();
            if (htmlId == HTMLTokenId.TAG_OPEN) {
                String text = htmlToken.text().toString();
                if ("script".equals(text)) {
                    TokenSequence<? extends HTMLTokenId> ets = ts.subSequence(ts.offset());
                    ets.moveStart();
                    boolean foundSrc = false;
                    boolean foundType = false;
                    String type = null;
                    String src = null;
                    while (ets.moveNext()) {
                        Token<? extends HTMLTokenId> t = ets.token();
                        HTMLTokenId id = t.id();
                        if (id == HTMLTokenId.TAG_CLOSE_SYMBOL) {
                            break;
                        } else if (foundSrc || foundType) {
                            if (id == HTMLTokenId.ARGUMENT) {
                                break;
                            } else if (id == HTMLTokenId.VALUE) {
                                if (foundSrc) {
                                    src = t.toString();
                                } else {
                                    assert foundType;
                                    type = t.toString();
                                }
                                foundSrc = false;
                                foundType = false;
                            }
                        } else if (id == HTMLTokenId.ARGUMENT) {
                            String val = t.toString();
                            if ("src".equals(val)) {
                                foundSrc = true;
                            } else if ("type".equals(val)) {
                                foundType = true;
                            }
                        }
                    }
                    if ((src == null) && (type != null) && type.contains(CoffeeScriptLanguage.MIME_TYPE)) {
                        inCoffeeScript = true;
                    }
                }
            } else if (inCoffeeScript && htmlId == HTMLTokenId.TEXT) {
                ts.createEmbedding(CoffeeScriptLanguage.getLanguage(), 0, 0);
                System.out.println("*** " + ts.token().text());
//                    embeddings.add(snapshot.create("\n", CoffeeScriptLanguage.MIME_TYPE));
                inCoffeeScript = false;
            }
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }

    public static class Factory extends TaskFactory {

        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singleton(new CoffeeScriptHTMLParserTask());
        }
    }
}
