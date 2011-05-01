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
