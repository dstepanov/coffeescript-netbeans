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

import coffeescript.nb.options.CoffeeScriptSettings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.event.ChangeListener;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.csl.spi.DefaultError;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.text.NbDocument;
import org.openide.util.RequestProcessor;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptParser extends Parser {

    private static final RequestProcessor PARSER_TASK = new RequestProcessor(CoffeeScriptParser.class.getName(), 1);
    private Future<ParsingResult> future;

    public void parse(final Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
        future = PARSER_TASK.submit(new Callable<ParsingResult>() {

            public ParsingResult call() throws Exception {
                CharSequence text = snapshot.getText();
                CoffeeScriptCompiler.CompilerResult compilerResult = CoffeeScriptSettings.getCompiler().compile(text.toString(), CoffeeScriptSettings.get().isBare());
                return new ParsingResult(snapshot, compilerResult);
            }
        });
    }

    public Result getResult(Task task) throws ParseException {
        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (Exception ex) {
        } // Ignore
        future.cancel(true);
        return null;
    }

    public void addChangeListener(ChangeListener changeListener) {
    }

    public void removeChangeListener(ChangeListener changeListener) {
    }

    public static class Factory extends ParserFactory {

        public Parser createParser(Collection<Snapshot> snapshots) {
            return new CoffeeScriptParser();
        }
    }

    public static class ParsingResult extends ParserResult {

        private CoffeeScriptRhinoCompiler.CompilerResult compilerResult;

        public ParsingResult(Snapshot snapshot, CoffeeScriptRhinoCompiler.CompilerResult compilerResult) {
            super(snapshot);
            this.compilerResult = compilerResult;
        }

        public CoffeeScriptCompiler.CompilerResult getCompilerResult() {
            return compilerResult;
        }

        @Override
        public List<? extends Error> getDiagnostics() {
            if ((compilerResult != null) && (compilerResult.getError() != null)) {
                CoffeeScriptCompiler.Error error = compilerResult.getError();
                int line = error.getLine() == -1 ? 0 : error.getLine();
                String msg = error.getLine() == -1 ? error.getMessage() : error.getErrorName();
                StyledDocument doc = (StyledDocument) getSnapshot().getSource().getDocument(true);
                if (!getSnapshot().getMimePath().getMimeType(0).equals(CoffeeScriptLanguage.MIME_TYPE)) {
                    int originalOffset = getSnapshot().getOriginalOffset(0);
                    line += NbDocument.findLineNumber(doc, originalOffset);
                }
                int offsetError = getSnapshot().getEmbeddedOffset(NbDocument.findLineOffset(doc, Math.max(0, line - 1)));
                return Collections.singletonList(DefaultError.createDefaultError(
                        "cs.key", msg, "", getSnapshot().getSource().getFileObject(),
                        offsetError, -1, true, Severity.ERROR));
            }
            return Collections.emptyList();
        }

        protected void invalidate() {
//            compilerResult = null;
        }
    }
}
