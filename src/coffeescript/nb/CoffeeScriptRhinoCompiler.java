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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mozilla.javascript.*;
import org.openide.util.Exceptions;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptRhinoCompiler implements CoffeeScriptCompiler {

    private static CoffeeScriptRhinoCompiler INSTANCE;
    private Script coffeeScriptJs;
    private Pattern ERROR_PATTERN = Pattern.compile("(.*) on line (\\d*)(.*)");
    private int RHINO_OPTIMALIZATION_LEVEL = 9;

    private CoffeeScriptRhinoCompiler() {
    }

    public static synchronized CoffeeScriptRhinoCompiler get() {
        if (INSTANCE == null) {
            return (INSTANCE = new CoffeeScriptRhinoCompiler());
        }
        return INSTANCE;
    }

    public CompilerResult compile(String code, boolean bare) {
        try {
            return new CompilerResult(compileCode(code, bare));
        } catch (StoppedContextException e) {
            return null; // Canceled
        } catch (JavaScriptException e) {
            if (e.getValue() instanceof IdScriptableObject) {
                IdScriptableObject error = (IdScriptableObject) e.getValue();
                String message = (String) ScriptableObject.getProperty(error, "message");
                Matcher matcher = ERROR_PATTERN.matcher(message);
                if (matcher.matches()) {
                    return new CompilerResult(new Error(Integer.valueOf(matcher.group(2)), matcher.group(1) + matcher.group(3), message));
                }
                return new CompilerResult(new Error(-1, "", message));
            }
            return new CompilerResult(new Error(-1, "", e.getMessage()));
        }
    }

    private String compileCode(String code, boolean bare) {
        Context ctx = Context.enter();
        try {
            //ctx.setInstructionObserverThreshold(1);
            ctx.setOptimizationLevel(RHINO_OPTIMALIZATION_LEVEL);
            Scriptable scope = ctx.newObject(ctx.initStandardObjects());
            scope.put("code", scope, code);
            getCoffeeScriptJS().exec(ctx, scope);
            String options = String.format("{bare: %b}", bare);
            String script = String.format("CoffeeScript.compile(code, %s);", options);
            return (String) getScriptFromString(script).exec(ctx, scope);
        } finally {
            Context.exit();
        }
    }

    private synchronized Script getCoffeeScriptJS() {
        if (coffeeScriptJs == null) {
            coffeeScriptJs = getScriptFromClasspath("coffeescript/nb/resources/coffee-script.js");
        }
        return coffeeScriptJs;
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
        Context ctx = Context.enter();
        try {
            ctx.setOptimizationLevel(RHINO_OPTIMALIZATION_LEVEL);
            return ctx.compileReader(reader, "", 0, null);
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        } finally {
            Context.exit();
        }
        return null;
    }

    private Script getScriptFromString(String string) {
        Context ctx = Context.enter();
        try {
            ctx.setOptimizationLevel(RHINO_OPTIMALIZATION_LEVEL);
            return ctx.compileString(string, "", 0, null);
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        } finally {
            Context.exit();
        }
        return null;
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
