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
import javax.swing.text.StyledDocument;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.text.NbDocument;

/**
 * @author Denis Stepanov
 */
public class CoffeeScriptSyntaxErrorsHighlightingTask extends ParserResultTask<CoffeeScriptParser.ParsingResult> {

    public void run(CoffeeScriptParser.ParsingResult result, SchedulerEvent event) {
        Document document = result.getSnapshot().getSource().getDocument(false);
        if ((result != null) && (result.getCompilerResult() != null) && (result.getCompilerResult().getError() != null)) {
            CoffeeScriptCompiler.Error error = result.getCompilerResult().getError();
            int line = error.getLine() == -1 ? 0 : error.getLine();

            if (!result.getSnapshot().getMimePath().getMimeType(0).equals(CoffeeScriptLanguage.MIME_TYPE)) {
                StyledDocument doc = (StyledDocument) result.getSnapshot().getSource().getDocument(true);
                int originalOffset = result.getSnapshot().getOriginalOffset(0);
                line += NbDocument.findLineNumber(doc, originalOffset);
            }

            String msg = error.getLine() == -1 ? error.getMessage() : error.getErrorName();
            ErrorDescription errorDescription = ErrorDescriptionFactory.createErrorDescription(Severity.ERROR, msg, document, line);
            HintsController.setErrors(document, CoffeeScriptLanguage.MIME_TYPE, Collections.singleton(errorDescription));
        } else {
            HintsController.setErrors(document, CoffeeScriptLanguage.MIME_TYPE, Collections.<ErrorDescription>emptyList());
        }
    }

    public int getPriority() {
        return 100;
    }

    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    public void cancel() {
    }

    public static class Factory extends TaskFactory {

        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singleton(new CoffeeScriptSyntaxErrorsHighlightingTask());
        }
    }
}
