package org.showshortcuts.internal;

import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_MOUSE_TRIGGER_ENABLED;
import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_SHORTCUTS_ENABLED;
import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_SHOW_DESCRIPTION;
import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_TIME_TO_CLOSE;

import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin //
		implements IStartup, IExecutionListener, IPropertyChangeListener, DebugOptionsListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.showshortcuts"; //$NON-NLS-1$

	private static final String DEBUG_PATH = "/debug"; //$NON-NLS-1$
	private static final String DEBUG_PATH_FULL = PLUGIN_ID + DEBUG_PATH;

	// The shared instance
	private static Activator plugin;

	private ShortcutPopup shorcutPopup;

	private static volatile DebugTrace debugTrace;
	private static volatile boolean debug = false;

	@Override
	public void earlyStartup() {
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// need to do this later if all debug options are initialized properly
		Job job = new Job("Registering trace listener") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Dictionary<String, String> props = new Hashtable<String, String>(1, 1);
				props.put(DebugOptions.LISTENER_SYMBOLICNAME, PLUGIN_ID);
				context.registerService(DebugOptionsListener.class.getName(), Activator.this, props);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();

		getPreferenceStore().addPropertyChangeListener(plugin);

		if (isEnabled()) {
			ICommandService cmdService = (ICommandService) getWorkbench().getService(ICommandService.class);
			cmdService.addExecutionListener(plugin);
		}
	}

	private boolean isEnabled() {
		IPreferenceStore store = getPreferenceStore();
		return store.getBoolean(PREF_KEY_SHORTCUTS_ENABLED) || store.getBoolean(PREF_KEY_MOUSE_TRIGGER_ENABLED);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		getPreferenceStore().removePropertyChangeListener(plugin);

		ICommandService cmdService = (ICommandService) getWorkbench().getService(ICommandService.class);
		if (cmdService != null) {
			cmdService.removeExecutionListener(plugin);
		}

		closePopup();

		debugTrace = null;
		debug = false;
		plugin = null;

		super.stop(context);
	}

	private void closePopup() {
		if (this.shorcutPopup != null) {
			this.shorcutPopup.close();
			this.shorcutPopup = null;
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	@Override
	public void preExecute(String commandId, ExecutionEvent event) {
		Event trigger = getTrigger(commandId, event);
		if (trigger == null) {
			if (debug && debugTrace != null) {
				debugTrace.trace(DEBUG_PATH, MessageFormat.format("No trigger found for command {0} in event {1}", commandId, event)); //$NON-NLS-1$
			}
			return;
		}

		closePopup();

		String formattedShortcut = getFormattedShortcut(commandId, trigger);
		if (formattedShortcut == null) {
			return;
		}

		IWorkbench workbench = getWorkbench();
		ICommandService cmdService = (ICommandService) workbench.getService(ICommandService.class);
		Command command = cmdService.getCommand(commandId);
		int timeToClose = getPreferenceStore().getInt(PREF_KEY_TIME_TO_CLOSE);

		try {
			String name = command.getName();
			String description = null;
			if (getPreferenceStore().getBoolean(PREF_KEY_SHOW_DESCRIPTION)) {
				description = command.getDescription();
			}
			this.shorcutPopup = new ShortcutPopup(workbench.getActiveWorkbenchWindow().getShell(), timeToClose);
			this.shorcutPopup.setShortcut(formattedShortcut, name, description);
			this.shorcutPopup.open();
		} catch (NotDefinedException e) {
			if (debug && debugTrace != null) {
				debugTrace.trace(DEBUG_PATH, e.getMessage(), e);
			}
		}
	}

	private String getFormattedShortcut(String commandId, Event trigger) {
		IPreferenceStore store = getPreferenceStore();

		int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(trigger);
		KeyStroke keyStroke = SWTKeySupport.convertAcceleratorToKeyStroke(accelerator);

		if (KeyStroke.NO_KEY != keyStroke.getNaturalKey()) {
			if (store.getBoolean(PREF_KEY_SHORTCUTS_ENABLED)) {
				String formattedStroke = SWTKeySupport.getKeyFormatterForPlatform().format(keyStroke);
				if (debug && debugTrace != null) {
					debugTrace.trace(DEBUG_PATH, "Formatted stroke is: " + formattedStroke); //$NON-NLS-1$
				}
				return formattedStroke;
			} else { // keystroke found, but we're disabled
				return null;
			}
		}

		else if (store.getBoolean(PREF_KEY_MOUSE_TRIGGER_ENABLED)) {
			IBindingService bindingService = (IBindingService) getWorkbench().getService(IBindingService.class);
			return bindingService.getBestActiveBindingFormattedFor(commandId);
		}

		return null;
	}

	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {
	}

	@Override
	public void notHandled(String commandId, NotHandledException exception) {
	}

	@Override
	public void postExecuteFailure(String commandId, ExecutionException exception) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (ShortcutPreferenceInitializer.PREF_KEY_SHORTCUTS_ENABLED.equals(property)
				|| ShortcutPreferenceInitializer.PREF_KEY_MOUSE_TRIGGER_ENABLED.equals(property)) {
			ICommandService cmdService = (ICommandService) getWorkbench().getService(ICommandService.class);
			if (isEnabled()) {
				cmdService.addExecutionListener(plugin);
			} else {
				cmdService.removeExecutionListener(plugin);
			}
		}
	}

	@Override
	public void optionsChanged(DebugOptions options) {
		debugTrace = options.newDebugTrace(PLUGIN_ID);
		debug = options.getBooleanOption(DEBUG_PATH_FULL, false);
	}

	private Event getTrigger(String commandId, ExecutionEvent event) {
		Event trigger = (Event) event.getTrigger();
		if (trigger != null) {
			return trigger;
		}
		return findTriggerInE4(commandId, event);
	}

	/*
	 * Bug in Eclipse 4.3: SWT Event trigger is not passed to command listeners.
	 * Fix is to dig for it in the internals...
	 */
	private Event findTriggerInE4(String commandId, ExecutionEvent event) {
		Class<?> handlerServiceHandler = Reflection.classForName(
				"org.eclipse.e4.core.commands.internal.HandlerServiceHandler", getClass().getClassLoader()); //$NON-NLS-1$
		if (handlerServiceHandler != null) {
			Reflection handlerReflection = Reflection.forNewObject(handlerServiceHandler, new Class[] { String.class }, commandId);
			Object staticContext = handlerReflection
					.invoke("getStaticContext", new Class[] { Object.class }, event.getApplicationContext()); //$NON-NLS-1$

			Event trigger = (Event) Reflection.forObject(staticContext).invoke("getLocal", new Class[] { Class.class }, Event.class); //$NON-NLS-1$
			return trigger;
		}
		return null;
	}

	public static void log(Throwable e) {
		if (plugin != null) {
			plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
		} else {
			e.printStackTrace();
		}
	}

}
