package coffeescript.nb;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.openide.util.Exceptions;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptRhinoCompiler {

    private final Map<String, Script> scriptCacheMap = new HashMap<String, Script>(1);
    private static CoffeeScriptRhinoCompiler INSTANCE;

    private CoffeeScriptRhinoCompiler() {
    }

    public static synchronized CoffeeScriptRhinoCompiler get() {
        if (INSTANCE == null) {
            return (INSTANCE = new CoffeeScriptRhinoCompiler());
        }
        return INSTANCE;
    }

    public CompilerResult compile(String code) {
        try {
            return new CompilerResult(compileCode(code));
        } catch (StoppedContextException e) {
            return null; // Canceled
        } catch (JavaScriptException e) {
            if (e.getValue() instanceof IdScriptableObject) {
                IdScriptableObject error = (IdScriptableObject) e.getValue();
                String message = (String) ScriptableObject.getProperty(error, "message");
                Pattern pattern = Pattern.compile("(.*) on line (\\d*)(.*)");
                Matcher matcher = pattern.matcher(message);
                if (matcher.matches()) {
                    return new CompilerResult(new Error(Integer.valueOf(matcher.group(2)), matcher.group(1) + matcher.group(3), message));
                }
                return new CompilerResult(new Error(-1, "", message));
            }
            return new CompilerResult(new Error(-1, "", e.getMessage()));
        }
    }

    private String compileCode(String code) {
        Context.enter();
        Context ctx = new StoppableContext();
        try {
            ctx.setInstructionObserverThreshold(1);
            ctx.setOptimizationLevel(-1);
            Scriptable scope = ctx.newObject(ctx.initStandardObjects());
            getScriptFromClasspath("coffeescript/nb/resources/coffee-script.js").exec(ctx, scope);
            scope.put("code", scope, code);
            return (String) getScriptFromString("CoffeeScript.compile(code);").exec(ctx, scope);

        } finally {
            Context.exit();
        }
    }

    private Script getScriptFromClasspath(String url) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(url);
            return getScriptFromReader(url, new InputStreamReader(inputStream, "UTF-8"));
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        return null;
    }

    private Script getScriptFromReader(String key, Reader reader) {
        synchronized (scriptCacheMap) {
            Script script = scriptCacheMap.get(key);
            if (script == null) {
                Context ctx = Context.enter();
                try {
                    ctx.setOptimizationLevel(-1);
                    script = ctx.compileReader(reader, "", 0, null);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                } finally {
                    Context.exit();
                }
                scriptCacheMap.put(key, script);
            }
            return script;
        }
    }

    private Script getScriptFromString(String string) {
        synchronized (scriptCacheMap) {
            Script script = scriptCacheMap.get(string);
            if (script == null) {
                Context ctx = Context.enter();
                try {
                    ctx.setOptimizationLevel(-1);
                    script = ctx.compileString(string, "", 0, null);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                } finally {
                    Context.exit();
                }
                scriptCacheMap.put(string, script);
            }
            return script;
        }
    }

    public static class CompilerResult {

        private String js;
        private Error error;

        public CompilerResult(String js) {
            this.js = js;
        }

        public CompilerResult(Error error) {
            this.error = error;
        }

        public String getJs() {
            return js;
        }

        public Error getError() {
            return error;
        }
    }

    public static class Error {

        private final int line;
        private final String errorName, message;

        public Error(int line, String errorName, String message) {
            this.line = line;
            this.errorName = errorName;
            this.message = message;
        }

        public int getLine() {
            return line;
        }

        public String getErrorName() {
            return errorName;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class StoppableContext extends Context {

        @Override
        protected void observeInstructionCount(int instructionCount) {
            if (Thread.interrupted()) {
                throw new StoppedContextException();
            }
        }
    }

    public static class StoppedContextException extends RuntimeException {
    }
}
