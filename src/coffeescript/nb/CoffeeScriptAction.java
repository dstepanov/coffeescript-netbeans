package coffeescript.nb;

import coffeescript.nb.CoffeeScriptCompiler.CompilerResult;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.concurrent.Future;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.LifecycleManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.util.Cancellable;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.Presenter.Popup;
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

    public Action createContextAwareInstance(Lookup actionContext) {
        Collection<? extends CoffeeScriptDataObject> data = actionContext.lookupAll(CoffeeScriptDataObject.class);
        if (data.isEmpty()) {
            return null;
        }
        return new PopupAction(data);
    }

    public void actionPerformed(ActionEvent ae) {
    }

    private static class PopupAction extends AbstractAction implements Popup {

        private Collection<? extends CoffeeScriptDataObject> data;
        boolean switchOn = true, switchOff = true, switchMultipleValues;

        public PopupAction(Collection<? extends CoffeeScriptDataObject> data) {
            super(NbBundle.getBundle("coffeescript.nb.resources.Bundle").getString("CoffeeScriptAction"));
            this.data = data;

            for (CoffeeScriptDataObject dataObject : data) {
                boolean status = CoffeeScriptAutocompileContext.get().isEnabled(dataObject.getPrimaryFile());
                switchOn = switchOn && status;
                switchOff = switchOff && !status;
            }
            switchMultipleValues = !switchOn && !switchOff;
        }

        public JMenuItem getPopupPresenter() {
            String color = switchMultipleValues ? "gray" : (switchOn ? "green" : "black");
            String label = switchMultipleValues ? "-" : (switchOn ? "on" : "off");
            String switchActionName = String.format("<html>Autocompile: <b style=\"color: %s\">%s</b>", color, label);

            final JMenu menu = new JMenu(this);
            JMenu switchmenu = new JMenu(switchActionName);
            if (switchMultipleValues || switchOff) {
                switchmenu.add(new AbstractAction("Turn on") {

                    public void actionPerformed(ActionEvent ae) {
                        for (CoffeeScriptDataObject dataObject : data) {
                            CoffeeScriptAutocompileContext.get().enableAutocompile(dataObject.getPrimaryFile());
                        }
                        RequestProcessor processor = RequestProcessor.getDefault();
                        final Future[] futureHolder = new Future[1];
                        futureHolder[0] = processor.submit(new CompilerTask(data) {

                            public void run() {
                                try {
                                    compile();
                                } catch (Exception e) {
                                    Exceptions.printStackTrace(e);
                                }
                            }

                            public boolean cancel() {
                                futureHolder[0].cancel(true);
                                return true;
                            }
                        });
                    }
                });
            }
            if (switchMultipleValues || switchOn) {
                switchmenu.add(new AbstractAction("Turn off") {

                    public void actionPerformed(ActionEvent ae) {
                        for (CoffeeScriptDataObject dataObject : data) {
                            CoffeeScriptAutocompileContext.get().disableAutocompile(dataObject.getPrimaryFile());
                        }
                    }
                });
            }
            menu.add(switchmenu);
            menu.addSeparator();
            menu.add(new CompileAction(data));
            return menu;
        }

        public void actionPerformed(ActionEvent ae) {
        }
    }

    private static class CompileAction extends AbstractAction {

        Collection<? extends CoffeeScriptDataObject> data;

        public CompileAction(Collection<? extends CoffeeScriptDataObject> data) {
            super("Compile");
            this.data = data;
        }

        public void actionPerformed(ActionEvent ae) {
            new ConsoleOutputCompileTask(data).execute();
        }
    }

    private static class CancelAction extends AbstractAction {

        private Cancellable cancellable;

        private CancelAction(Cancellable cancellable) {
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("coffeescript/nb/resources/stop.png", false));
            putValue(Action.NAME, "Stop");
            setEnabled(false);
            this.cancellable = cancellable;
        }

        public void actionPerformed(ActionEvent e) {
            cancellable.cancel();
            setEnabled(false);
        }
    }

    private static class ConsoleOutputCompileTask extends CompilerTask {

        private final CancelAction cancelAction;
        private ExecutorTask executorTask;
        private InputOutput io;

        public ConsoleOutputCompileTask(Collection<? extends CoffeeScriptDataObject> data) {
            super(data);
            this.cancelAction = new CancelAction(this);
            this.io = IOProvider.getDefault().getIO(getName(), new Action[]{cancelAction});
        }

        public void execute() {
            executorTask = ExecutionEngine.getDefault().execute("CoffeeScriptExecutor", this, io);
        }

        @Override
        public void run() {
            try {
                cancelAction.setEnabled(true);
                super.compile();
            } catch (Exception e) {
                e.printStackTrace(io.getErr());
            } finally {
                cancelAction.setEnabled(false);
            }
        }

        @Override
        protected void handleResult(CompilerResult result) {
            if (result.getJs() != null) {
                io.getOut().append(result.getJs());
            } else {
                io.getErr().append(result.getError().getMessage());
            }
        }

        @Override
        public boolean cancel() {
            executorTask.stop();
            cancelAction.setEnabled(false);
            return true;
        }
    }

    private static abstract class CompilerTask implements Cancellable, Runnable {

        private Collection<? extends CoffeeScriptDataObject> data;
        private String taskName;

        public CompilerTask(Collection<? extends CoffeeScriptDataObject> data) {
            this.data = data;
            taskName = data.size() > 1 ? "Compiling CoffeeScript files" : "Compiling " + data.iterator().next().getPrimaryFile().getNameExt();
        }

        public void compile() throws Exception {
            LifecycleManager.getDefault().saveAll();
            for (CoffeeScriptDataObject dataObject : data) {
                ProgressHandle handle = handle = ProgressHandleFactory.createHandle("Compiling " + dataObject.getPrimaryFile().getNameExt(), this);
                try {
                    handle.start();
                    CoffeeScriptRhinoCompiler.CompilerResult result = CoffeeScriptRhinoCompiler.get().compile(dataObject.getPrimaryFile().asText());
                    if (result == null) {
                        return; // Canceled
                    }
                    if (result.getJs() != null) {
                        FileObject folder = dataObject.getFolder().getPrimaryFile();
                        FileObject file = folder.getFileObject(dataObject.getName(), "js");
                        if (file != null) {
                            file.delete();
                        }
                        file = folder.createData(dataObject.getName(), "js");
                        file.getOutputStream().write(result.getJs().getBytes());
                    } else {
                    }
                    handleResult(result);
                } finally {
                    handle.finish();
                }
            }
        }

        protected void handleResult(CoffeeScriptRhinoCompiler.CompilerResult result) {
        }

        public String getName() {
            return taskName;
        }
    }
}
