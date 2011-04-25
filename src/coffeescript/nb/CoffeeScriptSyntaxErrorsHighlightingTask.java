package coffeescript.nb;

import coffeescript.nb.CoffeeScriptRhinoCompiler.Error;
import java.util.Collection;
import java.util.Collections;
import javax.swing.text.Document;
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

/**
 * @author Denis Stepanov
 */
public class CoffeeScriptSyntaxErrorsHighlightingTask extends ParserResultTask<CoffeeScriptParser.ParsingResult> {

    public void run(CoffeeScriptParser.ParsingResult result, SchedulerEvent event) {
        Document document = result.getSnapshot().getSource().getDocument(false);
        if ((result != null) && (result.getCompilerResult() != null) && (result.getCompilerResult().getError() != null)) {
            Error error = result.getCompilerResult().getError();
            int line = error.getLine() == -1 ? 0 : error.getLine();
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
