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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptNodeJSCompiler implements CoffeeScriptCompiler {

    private static Pattern ERROR_PATTERN1 = Pattern.compile("(.*) on line (\\d*)(.*)");
    private static Pattern ERROR_PATTERN2 = Pattern.compile(".*:(\\d*):(\\d*):.*: (.*)");

    private static CoffeeScriptNodeJSCompiler INSTANCE;
    private static final Logger logger = Logger.getLogger(CoffeeScriptNodeJSCompiler.class.getName());

    private CoffeeScriptNodeJSCompiler() {
    }

    public static synchronized CoffeeScriptNodeJSCompiler get() {
        if (INSTANCE == null) {
            return (INSTANCE = new CoffeeScriptNodeJSCompiler());
        }
        return INSTANCE;
    }

    private boolean isValidFile(String file) {
        File execFile = new File(file);
        if (!execFile.exists() || !execFile.isFile() || !execFile.canRead()) {
            if (Utilities.isWindows() && file.toLowerCase().contains("%appdata%")) {
                return true; // Allow to check file with appdata
            }
            return false;
        }
        return true;
    }

    public boolean isValid(String exec) {
        if (!isValidFile(exec)) {
            return false;
        }
        try {
            ExecResult result = exec(
                    Utilities.isWindows()
                    ? createValidateProcessBuilderWindows(exec)
                    : createValidateProcessBuilderNix(exec));
            String err = result.err;
            String out = result.out;
            if (!err.isEmpty()) {
                logger.log(Level.INFO, "Invalid exec\n{0}", err);
                return false;
            }
            if (!out.startsWith("CoffeeScript")) {
                logger.log(Level.INFO, "Not a coffee script ''coffee'', invalid output:\n{0}", out);
            }
            return true;
        } catch (Exception e) {
            logger.log(Level.INFO, "Invalid exec", e);
        }
        return false;
    }

    private ExecResult exec(ProcessBuilder pb) throws Exception {
        return exec(null, pb);
    }

    private ExecResult exec(String output, ProcessBuilder pb) throws Exception {
        return Utilities.isWindows() ? execWindows(output, pb) : execNix(output, pb);
    }

    private ExecResult execNix(final String output, ProcessBuilder pb) throws Exception {
        Map<String, String> environment = pb.environment();
        // Prevent "env: node: No such file or directory"
        environment.put("PATH", environment.get("PATH") + ":/usr/local/bin");
        Process p = pb.start();
        if (output != null) {
            OutputStream os = p.getOutputStream();
            os.write(output.getBytes("UTF-8"));
            os.close();
        }

        String out = getInputStreamAsString(p.getInputStream());
        String err = getInputStreamAsString(p.getErrorStream());

        p.destroy();

        ExecResult result = new ExecResult();
        result.err = err;
        result.out = out;
        return result;
    }

    private ExecResult execWindows(final String output, ProcessBuilder pb) throws Exception {

        final String[] errHolder = new String[1];
        final String[] outHolder = new String[1];

        final Process p = pb.start();

        final InputStream errStream = p.getErrorStream();
        final InputStream inputStream = p.getInputStream();

        Thread outThread = new Thread(new Runnable() {

            public void run() {
                if (output != null) {
                    try {
                        OutputStream os = p.getOutputStream();
                        os.write(output.getBytes("UTF-8"));
                        os.close();
                    } catch (Exception e) {
                        Exceptions.printStackTrace(e);
                    }
                }
            }
        });
        Thread errThread = new Thread(new Runnable() {

            public void run() {
                errHolder[0] = getInputStreamAsString(errStream);

            }
        });
        Thread inputThread = new Thread(new Runnable() {

            public void run() {
                outHolder[0] = getInputStreamAsString(inputStream);
            }
        });

        errThread.start();
        inputThread.start();
        outThread.start();
        p.waitFor();
        errThread.join();
        inputThread.join();
        outThread.join();
        p.destroy();

        ExecResult result = new ExecResult();
        result.err = errHolder[0];
        result.out = outHolder[0];
        return result;
    }

    public CompilerResult compile(final String code, boolean bare) {
        try {
            String exec = CoffeeScriptSettings.get().getCompilerExec();
            ExecResult result = exec(code,
                    Utilities.isWindows()
                    ? createCompileProcessBuilderWindows(exec, bare)
                    : createCompileProcessBuilderNix(exec, bare));
            String err = result.err;
            String out = result.out;
            if (!err.isEmpty()) {
                int i = err.indexOf('\n');
                if (i != -1) {
                    err = err.substring(0, i);
                }
                Matcher matcher = ERROR_PATTERN1.matcher(err);
                if (matcher.matches()) {
                    return new CompilerResult(new Error(Integer.valueOf(matcher.group(2)), matcher.group(1) + matcher.group(3), err));
                }
                matcher = ERROR_PATTERN2.matcher(err);
                if (matcher.matches()) {
                    return new CompilerResult(new Error(Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)), matcher.group(3), err));
                }
                return new CompilerResult(new Error(-1, "", err));
            }
            return new CompilerResult(out);
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        return null;
    }

    protected String getInputStreamAsString(InputStream is) {
        char[] buffer = new char[1024];
        Writer writer = new StringWriter();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return writer.toString();
    }

    protected ProcessBuilder createValidateProcessBuilderWindows(String exec) {
        return new ProcessBuilder("cmd", "/c", "\"\"" + exec + "\"" + " -v \"");
    }

    protected ProcessBuilder createValidateProcessBuilderNix(String exec) {
        return new ProcessBuilder(exec, "-v");
    }

    protected ProcessBuilder createCompileProcessBuilderWindows(String exec, boolean bare) {
        return new ProcessBuilder("cmd", "/c", "\"\"" + exec + "\"" + (bare ? " -scb" : " -sc") + " \"");
    }

    protected ProcessBuilder createCompileProcessBuilderNix(String exec, boolean bare) {
        return new ProcessBuilder(exec, (bare ? "-scb" : "-sc"));
    }

    private static class ExecResult {

        String out, err;
    }
}
