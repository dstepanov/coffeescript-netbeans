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
package coffeescript.nb.options;

import coffeescript.nb.CoffeeScriptCompiler;
import coffeescript.nb.CoffeeScriptNodeJSCompiler;
import coffeescript.nb.CoffeeScriptRhinoCompiler;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptSettings {

    private static CoffeeScriptSettings INSTANCE;

    private CoffeeScriptSettings() {
    }

    public static synchronized CoffeeScriptSettings get() {
        if (INSTANCE == null) {
            INSTANCE = new CoffeeScriptSettings();
        }
        return INSTANCE;
    }

    private Preferences getPreferences() {
        return NbPreferences.forModule(CoffeeScriptSettings.class).node("settings");
    }

    public boolean isBare() {
        return getPreferences().getBoolean("bare", true);
    }

    public void setBare(boolean bare) {
        getPreferences().put("bare", Boolean.toString(bare));
    }

    public CompilerType getCompilerType() {
        return CompilerType.valueOf(getPreferences().get("compilerType", CompilerType.RHINO.name()));
    }

    public void setCompilerType(CompilerType compilerType) {
        getPreferences().put("compilerType", compilerType.name());
    }

    public String getCompilerExec() {
        return getPreferences().get("compilerExec", "");
    }

    public void setCompilerExec(String compilerExec) {
        getPreferences().put("compilerExec", compilerExec);
    }

    public static CoffeeScriptCompiler getCompiler() {
        switch (CoffeeScriptSettings.get().getCompilerType()) {
            case NODEJS:
                return CoffeeScriptNodeJSCompiler.get();
            case RHINO:
                return CoffeeScriptRhinoCompiler.get();
        }
        return null;
    }

    public enum CompilerType {

        RHINO("Rhino (JavaScript for Java)"),
        NODEJS("CoffeeScript (Node.js)");
        private final String label;

        private CompilerType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
