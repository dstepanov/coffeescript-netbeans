package org.netbeans.layer.module;

/**
 * 
 * @author Denis Stepanov
 */
public abstract class AbstractLayerModule implements LayerModule {

    public Folder folder(String name) {
        return null;
    }

    public static class File {
    }

    public static class Folder {

        public void files(File... newfiles) {
        }
    }
}
