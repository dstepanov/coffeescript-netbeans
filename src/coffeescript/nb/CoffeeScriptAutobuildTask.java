package coffeescript.nb;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 * @author Denis Stepanov
 */
public class CoffeeScriptAutobuildTask extends ParserResultTask<CoffeeScriptParser.ParsingResult> {

    public void run(final CoffeeScriptParser.ParsingResult result, SchedulerEvent event) {
        if ((result != null) && (result.getCompilerResult() != null) && (result.getCompilerResult().getJs() != null)) {
            final FileObject coffeeFile = result.getSnapshot().getSource().getFileObject();
            final String js = result.getCompilerResult().getJs();
            try {
                FileObject folder = coffeeFile.getParent();
                FileObject file = folder.getFileObject(coffeeFile.getName(), "js");
                if (file == null) {
                    file = folder.createData(coffeeFile.getName(), "js");
                }
                if (!file.asText().equals(js)) {
                    OutputStream out = file.getOutputStream();
                    out.write(js.getBytes());
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
        }
    }

    public int getPriority() {
        return 10000;
    }

    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    public void cancel() {
    }

    public static class Factory extends TaskFactory {

        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singleton(new CoffeeScriptAutobuildTask());
        }
    }
}
