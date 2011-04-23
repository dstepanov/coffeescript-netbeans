package coffeescript.nb;

import coffeescript.nb.CoffeeScriptParser.ParsingResult;
import java.util.Collection;
import java.util.Collections;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

/**
 * TODO
 * 
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptSyntaxErrorsHighlightingTask extends ParserResultTask<CoffeeScriptParser.ParsingResult> {

    public void run(ParsingResult result, SchedulerEvent event) {
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
