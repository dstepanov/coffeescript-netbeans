package coffeescript.nb.options;

/**
 *
 * @author Denis Stepanov
 */
public class CoffeeScriptOptionsPanel extends javax.swing.JPanel {

    private CoffeeScriptOptionsPanelController controller;

    public CoffeeScriptOptionsPanel(CoffeeScriptOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
    }

    public void update() {
        bareCheckbox.setSelected(getSettings().isBare());
    }

    public void applyChanges() {
        getSettings().setBare(bareCheckbox.isSelected());
    }

    private CoffeeScriptSettings getSettings() {
        return CoffeeScriptSettings.get();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bareCheckbox = new javax.swing.JCheckBox();

        bareCheckbox.setText(org.openide.util.NbBundle.getBundle("coffeescript.nb.resources.Bundle").getString("CoffeeScriptOptionsPanel.bareCheckbox.text")); // NOI18N
        bareCheckbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bareCheckboxStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(bareCheckbox)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(bareCheckbox)
                .addContainerGap(260, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void bareCheckboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bareCheckboxStateChanged
        controller.changed();
    }//GEN-LAST:event_bareCheckboxStateChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox bareCheckbox;
    // End of variables declaration//GEN-END:variables
}
