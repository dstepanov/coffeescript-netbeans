package coffeescript.nb;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.LifecycleManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.util.Cancellable;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author Denis Stepanov
 */
@ActionID(category = "Build", id = "CoffeeScriptAction")
@ActionRegistration(displayName = "coffeescript.nb.resources.Bundle#CoffeeScriptAction")
@ActionReferences({
    @ActionReference(path = "Loaders/text/coffeescript/Actions", position = 200, separatorAfter = 201, separatorBefore = 199),
    @ActionReference(path = "Editors/text/coffeescript/Popup", position = 10001, separatorBefore = 10000)
})
public class CoffeeScriptAction extends AbstractAction implements ContextAwareAction {

    private CoffeeScriptDataObject data;

    public CoffeeScriptAction() {
    }

    public CoffeeScriptAction(CoffeeScriptDataObject data) {
        super(NbBundle.getBundle("coffeescript.nb.resources.Bundle").getString("CoffeeScriptAction"));
        this.data = data;
    }

    public void actionPerformed(ActionEvent ae) {
        final CancelAction cancelAction = new CancelAction();
        final InputOutput io = IOProvider.getDefault().getIO(data.getPrimaryFile().getNameExt(), new Action[]{
                    cancelAction});

        final ExecutorTask[] taskHolder = new ExecutorTask[1];
        Cancellable cancellable = new Cancellable() {

            public boolean cancel() {
                taskHolder[0].stop();
                cancelAction.setEnabled(false);
                return true;
            }
        };
        cancelAction.setCancellable(cancellable);
        final ProgressHandle handle = ProgressHandleFactory.createHandle("Compiling " + data.getPrimaryFile().getNameExt(), cancellable);
        taskHolder[0] = ExecutionEngine.getDefault().execute("CoffeeScriptExecutor", new Runnable() {

            public void run() {
                handle.start();
                try {
                    cancelAction.setEnabled(true);
                    LifecycleManager.getDefault().saveAll();
                    CoffeeScriptRhinoCompiler.CompilerResult result = CoffeeScriptRhinoCompiler.get().compile(data.getPrimaryFile().asText());
                    if (result == null) {
                        return; // Canceled
                    }
                    if (result.getJs() != null) {
                        io.getOut().append(result.getJs());
                    } else {
                        io.getErr().append(result.getError().getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace(io.getErr());
                } finally {
                    cancelAction.setEnabled(false);
                    handle.finish();
                }
            }
        }, io);
    }

    public Action createContextAwareInstance(Lookup actionContext) {
        CoffeeScriptDataObject data = actionContext.lookup(CoffeeScriptDataObject.class);
        return data == null ? null : new CoffeeScriptAction(data);
    }

    static class CancelAction extends AbstractAction {

        private Cancellable cancellable;

        CancelAction() {
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("coffeescript/nb/resources/stop.png", false)); //NOi18N
            putValue(Action.NAME, "Stop");
//            putValue(Action.NAME, NbBundle.getMessage(StopAction.class, "TXT_Stop_execution"));
//            putValue(Action.SHORT_DESCRIPTION, NbBundle.getMessage(StopAction.class, "TIP_Stop_Execution"));
            setEnabled(false);
        }

        public void setCancellable(Cancellable cancellable) {
            this.cancellable = cancellable;
        }

        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            RequestProcessor.getDefault().post(new Runnable() {

                public void run() {
                    cancellable.cancel();
                }
            });
        }
    }
}
