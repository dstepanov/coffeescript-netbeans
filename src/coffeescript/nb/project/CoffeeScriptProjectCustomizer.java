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
package coffeescript.nb.project;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Denis Stepanov 
 */
public class CoffeeScriptProjectCustomizer implements CustomizerProvider {

    public static final String CUSTOMIZER_FOLDER_PATH = "Projects/coffeescript-nb-project/Customizer";
    private final CoffeeScriptProject project;

    public CoffeeScriptProjectCustomizer(CoffeeScriptProject project) {
        this.project = project;
    }

    @Override
    public void showCustomizer() {
        showCustomizer(null);
    }

    public void showCustomizer(final String preselectedCategory) {
        Mutex.EVENT.readAccess(new Runnable() {

            @Override
            public void run() {

                OptionListener optionListener = new OptionListener();
                StoreListener storeListener = new StoreListener();
                Lookup context = Lookups.fixed(project);
                Dialog dialog = ProjectCustomizer.createCustomizerDialog(CUSTOMIZER_FOLDER_PATH, context, preselectedCategory,
                        optionListener, storeListener, null);
                dialog.addWindowListener(optionListener);
                dialog.setTitle("Properties");
                dialog.setVisible(true);
            }
        });
    }

    private class StoreListener implements ActionListener {

        StoreListener() {
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    private static class OptionListener extends WindowAdapter implements ActionListener {

        OptionListener() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }

        @Override
        public void windowClosed(WindowEvent e) {
        }

        @Override
        public void windowClosing(WindowEvent e) {
        }
    }
}
