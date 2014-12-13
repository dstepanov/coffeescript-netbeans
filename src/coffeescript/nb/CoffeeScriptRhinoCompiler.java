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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
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
public class CoffeeScriptRhinoCompiler implements CoffeeScriptCompiler {

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
    

    public CompilerResult compile(String code, boolean bare, boolean literate) {
        try {
            return new CompilerResult(compileCode(code, bare, literate));
        } catch (StoppedContextException e) {
            return null; // Canceled
        } catch (JavaScriptException e) {
            if (e.getValue() instanceof IdScriptableObject) {
                IdScriptableObject error = (IdScriptableObject) e.getValue();
                String message = (String) ScriptableObject.getProperty(error, "message");
                IdScriptableObject location = (IdScriptableObject) ScriptableObject.getProperty(error, "location");
                Double line = (Double) ScriptableObject.getProperty(location, "first_line");
                Double column;
                if(col instanceof Integer) {
                    column = ((Integer)col).doubleValue();
                } else {
                    column = (Double)col;
                }
                return new CompilerResult(new Error(line == null ? -1 : line.intValue()+1, column == null ? 0 : column.intValue()+1, message, message));
            }
            return new CompilerResult(new Error(-1, "", e.getMessage()));
        }
    }

    private String compileCode(String code, boolean bare, boolean literate) {
        Context.enter();
        Context ctx = new StoppableContext();
        try {
            ctx.setInstructionObserverThreshold(1);
            ctx.setOptimizationLevel(-1);
            Scriptable scope = ctx.newObject(ctx.initStandardObjects());
            getScriptFromClasspath("coffeescript/nb/resources/coffee-script.js").exec(ctx, scope);
            scope.put("code", scope, code);
            String options = String.format("{bare: %b, literate: %b}", bare, literate);
            String script = String.format("CoffeeScript.compile(code, %s);", options);
            return (String) getScriptFromString(script).exec(ctx, scope);

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
