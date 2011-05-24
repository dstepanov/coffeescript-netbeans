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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.netbeans.api.html.lexer.HTMLTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.EmbeddingProvider;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

/**
 * @author Denis Stepanov
 */
public final class CoffeeScriptEmbeddingProvidersFactory extends TaskFactory {

    public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
        List<SchedulerTask> ems = new ArrayList<SchedulerTask>();
        if (snapshot.getSource().getMimeType().equals("text/html")) {
            ems.add(new HTMLEmbeddingProvider());
        }
        if (snapshot.getSource().getMimeType().equals("text/x-php5")) {
            ems.add(new PHPEmbeddingProvider());
        }
        return ems;
    }

    public class HTMLEmbeddingProvider extends EmbeddingProvider {

        @Override
        public List<Embedding> getEmbeddings(Snapshot snapshot) {
            List<Embedding> embeddings = new ArrayList<Embedding>();
            TokenHierarchy<?> th = snapshot.getTokenHierarchy();
            TokenSequence<? extends TokenId> tokenSequence = th.tokenSequence(HTMLTokenId.language());
            if (tokenSequence != null) {
                @SuppressWarnings("unchecked")
                TokenSequence<? extends HTMLTokenId> htmlTokenSequence = (TokenSequence<? extends HTMLTokenId>) tokenSequence;
                extractCoffeeScriptFromHTML(snapshot, htmlTokenSequence, embeddings);
            }
            if (embeddings.isEmpty()) {
                return Collections.<Embedding>emptyList();
            }
            return Collections.singletonList(Embedding.create(embeddings));
        }

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void cancel() {
        }
    }

    public class PHPEmbeddingProvider extends EmbeddingProvider {

        @Override
        public List<Embedding> getEmbeddings(Snapshot snapshot) {
            TokenHierarchy<?> th = snapshot.getTokenHierarchy();
            if (th == null) {
                return Collections.emptyList();
            }

            TokenSequence<? extends TokenId> tokenSequence = th.tokenSequence();
            List<Embedding> embeddings = new ArrayList<Embedding>();
            while (tokenSequence.moveNext()) {
                Token<? extends TokenId> token = tokenSequence.token();
                if (token.id().name().equals("T_INLINE_HTML")) {
                    TokenSequence<? extends HTMLTokenId> ts = tokenSequence.embeddedJoined(HTMLTokenId.language());
                    if (ts == null) {
                        continue;
                    }
                    extractCoffeeScriptFromHTML(snapshot, ts, embeddings);
                    break;
                }
            }
            return embeddings;
        }

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void cancel() {
        }
    }

    private void extractCoffeeScriptFromHTML(Snapshot snapshot, TokenSequence<? extends HTMLTokenId> ts, List<Embedding> embeddings) {
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
                embeddings.add(snapshot.create(ts.offset(), htmlToken.length(), CoffeeScriptLanguage.MIME_TYPE));
            } else if (htmlId == HTMLTokenId.TAG_CLOSE && "script".equals(htmlToken.toString())) {
                inCoffeeScript = false;
            } else {
            }
        }
    }
}
