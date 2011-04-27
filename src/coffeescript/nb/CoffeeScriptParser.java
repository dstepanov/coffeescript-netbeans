package coffeescript.nb;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
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
                CoffeeScriptRhinoCompiler.CompilerResult compilerResult = CoffeeScriptRhinoCompiler.get().compile(text.toString());
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

    public static class ParsingResult extends Result {

        private CoffeeScriptRhinoCompiler.CompilerResult compilerResult;

        public ParsingResult(Snapshot snapshot, CoffeeScriptRhinoCompiler.CompilerResult compilerResult) {
            super(snapshot);
            this.compilerResult = compilerResult;
        }

        public CoffeeScriptCompiler.CompilerResult getCompilerResult() {
            return compilerResult;
        }

        protected void invalidate() {
//            compilerResult = null;
        }
    }
}
