package coffeescript.nb.layer;

import coffeescript.nb.CoffeeScriptParser;
import org.netbeans.layer.module.InjectableLayerModule;
import org.netbeans.layer.module.annotations.GenerateLayer;

/**
 *
 * @author Denis Stepanov
 */
@GenerateLayer
public class CoffeeScriptLayerModule extends InjectableLayerModule {

    public void configure() {
        folder("Editors/text/coffeescript").files(inject(CoffeeScriptParser.Factory.class));
    }
}
