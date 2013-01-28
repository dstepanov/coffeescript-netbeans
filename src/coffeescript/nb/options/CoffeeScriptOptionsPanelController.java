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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 *
 * @author Denis Stepanov
 */
@OptionsPanelController.SubRegistration(id = "CoffeeScript",
displayName = "coffeescript.nb.resources.Bundle#CoffeeScriptOptions.displayName",
keywords = "coffeescript.nb.resources.Bundle#KW_CoffeeScriptOptions",
keywordsCategory = "Advanced/CoffeeScript")
public class CoffeeScriptOptionsPanelController extends OptionsPanelController {

    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
    private CoffeeScriptOptionsPanel panel;
    private boolean changed;

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getComponent();
    }

    @Override
    public void update() {
        panel.update();
    }

    @Override
    public void applyChanges() {
        panel.applyChanges();
        changed = false;
    }

    @Override
    public void cancel() {
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public boolean isValid() {
        return getComponent().isSettingsValid();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertySupport.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertySupport.removePropertyChangeListener(l);
    }

    void changed() {
        if (!changed) {
            changed = true;
            propertySupport.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
    }

    void valid() {
        propertySupport.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

    private synchronized CoffeeScriptOptionsPanel getComponent() {
        if (panel == null) {
            panel = new CoffeeScriptOptionsPanel(this);
        }
        return panel;
    }
}
