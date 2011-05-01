package coffeescript.nb.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import javax.swing.JPanel;
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
        return true;
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
        propertySupport.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

    private synchronized JPanel getComponent() {
        if (panel == null) {
            panel = new CoffeeScriptOptionsPanel(this);
        }
        return panel;
    }
}
