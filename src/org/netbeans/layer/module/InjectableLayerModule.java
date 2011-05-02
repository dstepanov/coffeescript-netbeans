package org.netbeans.layer.module;

/**
 *
 * @author Denis Stepanov
 */
public abstract class InjectableLayerModule extends AbstractLayerModule {

    public Inject inject(Class<?> injectClass) {
        return null;
    }

    public static class Inject extends File {
    }
}
