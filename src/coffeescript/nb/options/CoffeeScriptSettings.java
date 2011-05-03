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
        return getPreferences().getBoolean("bare", false);
    }

    public void setBare(boolean bare) {
        getPreferences().put("bare", Boolean.toString(bare));
    }
}
