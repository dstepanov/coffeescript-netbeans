package coffeescript.nb;

/**
 *
 * @author Denis Stepanov
 */
public interface CoffeeScriptCompiler {

    public CompilerResult compile(String code);

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
}
