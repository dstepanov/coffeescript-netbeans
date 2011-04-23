package coffeescript.nb;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.util.Exceptions;
import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.ImporterTopLevel;
import sun.org.mozilla.javascript.internal.Script;
import sun.org.mozilla.javascript.internal.ScriptableObject;

/**
 * Doesn't work yet. Too slow, mystery exceptions...
 * 
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptParser extends Parser {

    private final Script coffeeScript;
    private final Script checkScript;
    private ParsingResult result;

    public CoffeeScriptParser(Script coffeeScript, Script checkScript) {
        this.coffeeScript = coffeeScript;
        this.checkScript = checkScript;
    }

    public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
        Map<Integer, String> errors = new HashMap<Integer, String>();
        Context ctx = Context.enter();
        try {
            ctx.setOptimizationLevel(-1);
            ScriptableObject scope = new ImporterTopLevel(ctx);
            coffeeScript.exec(ctx, scope);
            scope.put("data", scope, snapshot.getText());
            Object r = checkScript.exec(ctx, scope);
            System.out.println(result);
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        } finally {
            Context.exit();
        }
        result = new ParsingResult(snapshot, errors);
    }

    public Result getResult(Task task) throws ParseException {
        return result;
    }

    public void addChangeListener(ChangeListener changeListener) {
    }

    public void removeChangeListener(ChangeListener changeListener) {
    }

    public static class Factory extends ParserFactory {

        private Script coffeeScript, checkScript;

        public Parser createParser(Collection<Snapshot> snapshots) {
            return new CoffeeScriptParser(getCoffeeScript(), getCheckScript());
        }

        private synchronized Script getCheckScript() {
            if (checkScript == null) {
                Context ctx = Context.enter();
                try {
                    ctx.setOptimizationLevel(-1);
                    checkScript = ctx.compileReader(new StringReader("CoffeeScript.nodes(data)"), "", 1, null);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                } finally {
                    Context.exit();
                }
            }
            return checkScript;
        }

        private synchronized Script getCoffeeScript() {
            if (coffeeScript == null) {
                Context ctx = Context.enter();
                try {
                    ctx.setOptimizationLevel(-1);
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("coffeescript/nb/coffee-script.js");
                    coffeeScript = ctx.compileReader(new InputStreamReader(inputStream, "UTF-8"), "", 1, null);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                } finally {
                    Context.exit();
                }
            }
            return coffeeScript;


        }
    }

    public static class ParsingResult extends Result {

        private Map<Integer, String> errors;

        public ParsingResult(Snapshot snapshot, Map<Integer, String> errors) {
            super(snapshot);
            this.errors = errors;
        }

        public Map<Integer, String> getErrors() {
            return errors;
        }

        protected void invalidate() {
            errors = null;
        }
    }
}
