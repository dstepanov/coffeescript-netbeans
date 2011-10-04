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

import coffeescript.nb.CoffeeScriptNodeJSCompiler;
import coffeescript.nb.options.CoffeeScriptSettings.CompilerType;
import java.awt.Desktop;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.openide.util.Utilities;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptOptionsPanel extends javax.swing.JPanel {

    private CoffeeScriptOptionsPanelController controller;
    final DocumentListener documentListener = new DocumentListener() {

        public void changedUpdate(DocumentEvent e) {
            execChanged();
        }

        public void removeUpdate(DocumentEvent e) {
            execChanged();
        }

        public void insertUpdate(DocumentEvent e) {
            execChanged();
        }
    };

    public CoffeeScriptOptionsPanel(CoffeeScriptOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        compilerComboBox.setModel(new javax.swing.DefaultComboBoxModel(CoffeeScriptSettings.CompilerType.values()));
        compilerHelp.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception e1) {
                    }
                }
            }
        });


    }

    public void update() {
        bareCheckbox.setSelected(getSettings().isBare());
        boolean isNode = getSettings().getCompilerType() == CompilerType.NODEJS;
        executablePathTextField.setVisible(isNode);
        executablePathLabel.setVisible(isNode);
        compilerComboBox.setSelectedItem(getSettings().getCompilerType());
    }

    public void applyChanges() {
        getSettings().setBare(bareCheckbox.isSelected());
        CompilerType compilerType = (CompilerType) compilerComboBox.getSelectedItem();
        getSettings().setCompilerType(compilerType);
        if (compilerType == CompilerType.NODEJS) {
            getSettings().setCompilerExec(executablePathTextField.getText());
        }
    }

    public boolean isSettingsValid() {
        CompilerType compilerType = (CompilerType) compilerComboBox.getSelectedItem();
        if (compilerType == CompilerType.NODEJS) {
            return CoffeeScriptNodeJSCompiler.get().isValid(executablePathTextField.getText());
        }
        return true;
    }

    private CoffeeScriptSettings getSettings() {
        return CoffeeScriptSettings.get();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bareCheckbox = new javax.swing.JCheckBox();
        compilerLabel = new javax.swing.JLabel();
        compilerComboBox = new javax.swing.JComboBox();
        executablePathLabel = new javax.swing.JLabel();
        executablePathTextField = new javax.swing.JTextField();
        compilerHelpScroll = new javax.swing.JScrollPane();
        compilerHelp = new javax.swing.JEditorPane();

        bareCheckbox.setText(org.openide.util.NbBundle.getBundle("coffeescript.nb.resources.Bundle").getString("CoffeeScriptOptionsPanel.bareCheckbox.text")); // NOI18N
        bareCheckbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bareCheckboxStateChanged(evt);
            }
        });

        compilerLabel.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.compilerLabel.text")); // NOI18N

        compilerComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Rhino (JavaScript for Java)", "CoffeeScript (Node.js)" }));
        compilerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compilerChanged(evt);
            }
        });

        executablePathLabel.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.executablePathLabel.text")); // NOI18N

        executablePathTextField.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.executablePathTextField.text")); // NOI18N

        compilerHelp.setContentType(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.compilerHelp.contentType")); // NOI18N
        compilerHelp.setEditable(false);
        compilerHelp.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.compilerHelp.text")); // NOI18N
        compilerHelpScroll.setViewportView(compilerHelp);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(compilerHelpScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(compilerLabel)
                            .add(executablePathLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE))
                        .add(29, 29, 29)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(compilerComboBox, 0, 308, Short.MAX_VALUE)
                            .add(executablePathTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)))
                    .add(bareCheckbox))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(bareCheckbox)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(compilerLabel)
                    .add(compilerComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(executablePathLabel)
                    .add(executablePathTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(compilerHelpScroll, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 89, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void bareCheckboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bareCheckboxStateChanged
        controller.changed();
    }//GEN-LAST:event_bareCheckboxStateChanged

    private void compilerChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compilerChanged
        boolean isNode = compilerComboBox.getSelectedItem() == CompilerType.NODEJS;
        executablePathTextField.setVisible(isNode);
        executablePathLabel.setVisible(isNode);
        if (isNode) {
            executablePathTextField.getDocument().removeDocumentListener(documentListener);
            String exec = getSettings().getCompilerExec();
            if (exec.length() == 0) {
                if (Utilities.isUnix() || Utilities.isMac()) {
                    exec = "/usr/local/bin/coffee";
                }
            }
            if (!exec.equals(executablePathTextField.getText())) {
                executablePathTextField.setText(exec);
            }
            executablePathTextField.getDocument().addDocumentListener(documentListener);
        } else {
            executablePathTextField.getDocument().removeDocumentListener(documentListener);
        }
        controller.changed();
        controller.valid();
    }//GEN-LAST:event_compilerChanged

    private void execChanged() {
        controller.changed();
        controller.valid();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox bareCheckbox;
    private javax.swing.JComboBox compilerComboBox;
    private javax.swing.JEditorPane compilerHelp;
    private javax.swing.JScrollPane compilerHelpScroll;
    private javax.swing.JLabel compilerLabel;
    private javax.swing.JLabel executablePathLabel;
    private javax.swing.JTextField executablePathTextField;
    // End of variables declaration//GEN-END:variables
}
