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
 * Doesn't work because of http://netbeans.org/bugzilla/show_bug.cgi?id=162990 ???
 * @author Denis Stepanov
 */
public final class CoffeeScriptEmbeddingProvidersFactory extends TaskFactory {

    public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
        if (!snapshot.getSource().getMimeType().equals("text/xhtml")) {
            if (snapshot.getMimeType().equals("text/html") && snapshot.getMimePath().size() > 1) { //NOI18N
                return null;
            }
        }
        if (snapshot.getMimeType().equals("text/html") && snapshot.getMimePath().size() > 1) { //NOI18N
            return null;
        }
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
            TokenHierarchy<CharSequence> th = TokenHierarchy.create(snapshot.getText(), HTMLTokenId.language());
            TokenSequence<? extends TokenId> tokenSequence = th.tokenSequence(HTMLTokenId.language());
            if (tokenSequence != null) {
                @SuppressWarnings("unchecked")
                TokenSequence<? extends HTMLTokenId> htmlTokenSequence = (TokenSequence<? extends HTMLTokenId>) tokenSequence;
                extractJavaScriptFromHtml(snapshot, htmlTokenSequence, embeddings);
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
                if (token.id().name().equals("T_INLINE_HTML")) { // NOI18N
                    TokenSequence<? extends HTMLTokenId> ts = tokenSequence.embeddedJoined(HTMLTokenId.language());
                    if (ts == null) {
                        continue;
                    }
                    extractJavaScriptFromHtml(snapshot, ts, embeddings);
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

    private void extractJavaScriptFromHtml(Snapshot snapshot, TokenSequence<? extends HTMLTokenId> ts, List<Embedding> embeddings) {
        boolean inCoffeeScript = false;
        ts.moveStart();
        while (ts.moveNext()) {
            Token<? extends HTMLTokenId> htmlToken = ts.token();
            HTMLTokenId htmlId = htmlToken.id();
            if (htmlId == HTMLTokenId.SCRIPT) {
                int sourceStart = ts.offset();
                String text = htmlToken.text().toString();
                // Make sure it doesn't start with <!--, if it does, remove it
                // (this is a mechanism used in files to gracefully handle older browsers)
                int start = 0;
                for (; start < text.length(); start++) {
                    char c = text.charAt(start);
                    if (!Character.isWhitespace(c)) {
                        break;
                    }
                }
                if (start < text.length() && text.startsWith("<!--", start)) {
                    int lineEnd = text.indexOf('\n', start);
                    if (lineEnd != -1) {
                        lineEnd++; //skip the \n
                        sourceStart += lineEnd;
                        text = text.substring(lineEnd);
                    }
                }
//                embeddings.add(snapshot.create(sourceStart, text.length(), CoffeeScriptLanguage.MIME_TYPE));
            } else if (htmlId == HTMLTokenId.TAG_OPEN) {
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
//                    embeddings.add(snapshot.create("\n", CoffeeScriptLanguage.MIME_TYPE));
                inCoffeeScript = false;
            } else {
            }
        }
    }
}
