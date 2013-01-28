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
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptOptionsPanel extends javax.swing.JPanel
{
    private CoffeeScriptOptionsPanelController controller;
    final DocumentListener documentListener = new DocumentListener()
    {
        public void changedUpdate(DocumentEvent e)
        {
            execChanged();
        }

        public void removeUpdate(DocumentEvent e)
        {
            execChanged();
        }

        public void insertUpdate(DocumentEvent e)
        {
            execChanged();
        }
    };

    public CoffeeScriptOptionsPanel(CoffeeScriptOptionsPanelController controller)
    {
        this.controller = controller;
        initComponents();
        compilerComboBox.setModel(new javax.swing.DefaultComboBoxModel(CoffeeScriptSettings.CompilerType.values()));
        compilerHelp.addHyperlinkListener(new HyperlinkListener()
        {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                {
                    try
                    {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception e1)
                    {
                    }
                }
            }
        });
    }

    public void update()
    {
        bareCheckbox.setSelected(getSettings().isBare());
        boolean isNode = getSettings().getCompilerType() == CompilerType.NODEJS;
        executablePathTextField.setVisible(isNode);
        executablePathLabel.setVisible(isNode);
        compilerComboBox.setSelectedItem(getSettings().getCompilerType());
        outputFolderTextField.setText(getSettings().getOutputFolder());
        utfCheckbox.setSelected(getSettings().isUseUTF8Encoding());
    }

    public void applyChanges()
    {
        getSettings().setBare(bareCheckbox.isSelected());
        CompilerType compilerType = (CompilerType) compilerComboBox.getSelectedItem();
        getSettings().setCompilerType(compilerType);
        if (compilerType == CompilerType.NODEJS)
        {
            getSettings().setCompilerExec(executablePathTextField.getText());
        }
        getSettings().setOutputFolder(outputFolderTextField.getText());
        getSettings().setUseUTF8Encoding(utfCheckbox.isSelected());
    }

    public boolean isSettingsValid()
    {
        CompilerType compilerType = (CompilerType) compilerComboBox.getSelectedItem();
        if (compilerType == CompilerType.NODEJS)
        {
            return CoffeeScriptNodeJSCompiler.get().isValid(executablePathTextField.getText());
        }
        return true;
    }

    private CoffeeScriptSettings getSettings()
    {
        return CoffeeScriptSettings.get();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        compilerLabel = new javax.swing.JLabel();
        compilerComboBox = new javax.swing.JComboBox();
        executablePathLabel = new javax.swing.JLabel();
        executablePathTextField = new javax.swing.JTextField();
        compilerHelpScroll = new javax.swing.JScrollPane();
        compilerHelp = new javax.swing.JEditorPane();
        compilerSettings = new javax.swing.JPanel();
        bareCheckbox = new javax.swing.JCheckBox();
        utfCheckbox = new javax.swing.JCheckBox();
        outputFolderLabel = new javax.swing.JLabel();
        outputFolderTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();

        setToolTipText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.toolTipText")); // NOI18N

        compilerLabel.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.compilerLabel.text")); // NOI18N

        compilerComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Rhino (JavaScript for Java)", "CoffeeScript (Node.js)" }));
        compilerComboBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                compilerChanged(evt);
            }
        });

        executablePathLabel.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.executablePathLabel.text")); // NOI18N

        executablePathTextField.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.executablePathTextField.text")); // NOI18N

        compilerHelp.setEditable(false);
        compilerHelp.setContentType(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.compilerHelp.contentType")); // NOI18N
        compilerHelp.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.compilerHelp.text")); // NOI18N
        compilerHelpScroll.setViewportView(compilerHelp);

        compilerSettings.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.compilerSettings.border.title"))); // NOI18N

        bareCheckbox.setText(org.openide.util.NbBundle.getBundle("coffeescript.nb.resources.Bundle").getString("CoffeeScriptOptionsPanel.bareCheckbox.text")); // NOI18N
        bareCheckbox.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                bareCheckboxStateChanged(evt);
            }
        });

        utfCheckbox.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.utfCheckbox.text")); // NOI18N

        outputFolderLabel.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.outputFolderLabel.text")); // NOI18N

        outputFolderTextField.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.outputFolderTextField.text")); // NOI18N

        browseButton.setText(org.openide.util.NbBundle.getMessage(CoffeeScriptOptionsPanel.class, "CoffeeScriptOptionsPanel.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                browseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout compilerSettingsLayout = new javax.swing.GroupLayout(compilerSettings);
        compilerSettings.setLayout(compilerSettingsLayout);
        compilerSettingsLayout.setHorizontalGroup(
            compilerSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(compilerSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(compilerSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(outputFolderLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(compilerSettingsLayout.createSequentialGroup()
                        .addGroup(compilerSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(bareCheckbox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(utfCheckbox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 9, Short.MAX_VALUE))
                    .addGroup(compilerSettingsLayout.createSequentialGroup()
                        .addComponent(outputFolderTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton)))
                .addContainerGap())
        );
        compilerSettingsLayout.setVerticalGroup(
            compilerSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(compilerSettingsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(bareCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(utfCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputFolderLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(compilerSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(compilerLabel)
                            .addComponent(executablePathLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(compilerComboBox, 0, 278, Short.MAX_VALUE)
                            .addComponent(executablePathTextField)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(compilerSettings, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(compilerHelpScroll))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(compilerLabel)
                    .addComponent(compilerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(executablePathLabel)
                    .addComponent(executablePathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(compilerHelpScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(compilerSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void bareCheckboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bareCheckboxStateChanged
        controller.changed();
    }//GEN-LAST:event_bareCheckboxStateChanged

    private void compilerChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compilerChanged
        boolean isNode = compilerComboBox.getSelectedItem() == CompilerType.NODEJS;
        executablePathTextField.setVisible(isNode);
        executablePathLabel.setVisible(isNode);
        if (isNode)
        {
            executablePathTextField.getDocument().removeDocumentListener(documentListener);
            String exec = getSettings().getCompilerExec();
            if (exec.length() == 0)
            {
                if (Utilities.isUnix() || Utilities.isMac())
                {
                    exec = "/usr/local/bin/coffee";
                }
                else if (Utilities.isWindows())
                {
                    exec = "%APPDATA%\\npm\\coffee.cmd";
                }
            }
            if (!exec.equals(executablePathTextField.getText()))
            {
                executablePathTextField.setText(exec);
            }
            executablePathTextField.getDocument().addDocumentListener(documentListener);
        }
        else
        {
            executablePathTextField.getDocument().removeDocumentListener(documentListener);
        }
        controller.changed();
        controller.valid();
    }//GEN-LAST:event_compilerChanged

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_browseButtonActionPerformed
    {//GEN-HEADEREND:event_browseButtonActionPerformed
        JFileChooser c = new JFileChooser();
        c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rVal = c.showOpenDialog(WindowManager.getDefault().getMainWindow());
        if (rVal == JFileChooser.APPROVE_OPTION)
        {
            outputFolderTextField.setText(c.getSelectedFile().getPath());
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void execChanged()
    {
        controller.changed();
        controller.valid();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox bareCheckbox;
    private javax.swing.JButton browseButton;
    private javax.swing.JComboBox compilerComboBox;
    private javax.swing.JEditorPane compilerHelp;
    private javax.swing.JScrollPane compilerHelpScroll;
    private javax.swing.JLabel compilerLabel;
    private javax.swing.JPanel compilerSettings;
    private javax.swing.JLabel executablePathLabel;
    private javax.swing.JTextField executablePathTextField;
    private javax.swing.JLabel outputFolderLabel;
    private javax.swing.JTextField outputFolderTextField;
    private javax.swing.JCheckBox utfCheckbox;
    // End of variables declaration//GEN-END:variables
}
