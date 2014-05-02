package org.showshortcuts.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tracker;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.showshortcuts.internal.l10n.Messages;

/**
 * Lightweight popup to a shortcut plus a description
 *
 * @author d031150
 */
public class ShortcutPopup extends Window {

	private static final int MARGIN_TOP = 80;
	private static final int MARGIN_RIGHT = 23;
	private static final String DIALOG_OFFSET_RIGHT = "offsetRight"; //$NON-NLS-1$
	private static final String DIALOG_OFFSET_TOP = "offsetTop"; //$NON-NLS-1$
	private static final String POPUP_COLOR_BG = Activator.PLUGIN_ID + ".popup.backgroundColor"; //$NON-NLS-1$
	private static final String POPUP_COLOR_FG = Activator.PLUGIN_ID + ".popup.foregroundColor"; //$NON-NLS-1$
	private static final String POPUP_FONT = Activator.PLUGIN_ID + ".popup.font"; //$NON-NLS-1$
	private static final int POPUP_FONT_SIZEFACTOR_KEY_LABEL = 2;
	private static final int POPUP_FONT_SIZEFACTOR_KEY = POPUP_FONT_SIZEFACTOR_KEY_LABEL + 1;

	private final List<Resource> resources = new ArrayList<Resource>(3);
	private final Listener moveListener = new MoveListener();
	private final int timeToClose;
	private String shortcut;
	private Label shortcutLabel;
	private String shortcutName;
	private String shortcutDescription;
	private Label shortcutNameLabel;
	private Label shortcutDescriptionLabel;
	private boolean readyToClose = true;

	public ShortcutPopup(Shell parentShell, int timeToClose) {
		super(parentShell);
		this.timeToClose = timeToClose;
		setShellStyle((SWT.NO_TRIM | SWT.ON_TOP | SWT.TOOL) & ~SWT.APPLICATION_MODAL);
	}

	public void setShortcut(String shortcut, String shortcutText, String shcortcutDescription) {
		this.shortcut = shortcut;
		this.shortcutName = shortcutText;
		this.shortcutDescription = shcortcutDescription;
	}

	@Override
	public int open() {
		scheduleClose();

		Shell shell = getShell();
		if (shell == null || shell.isDisposed()) {
			shell = null;
			// create the window
			create();
			shell = getShell();
		}

		// limit the shell size to the display size
		constrainShellSize();

		shell.setVisible(true);

		return OK;
	}

	private void scheduleClose() {
		this.readyToClose = true;
		Display.getDefault().timerExec(this.timeToClose, new Runnable() {
			@Override
			public void run() {
				if (ShortcutPopup.this.readyToClose && getShell() != null && !getShell().isDisposed()) {
					close();
				}
			}
		});
	}

	@Override
	public boolean close() {
		// If already closed, there is nothing to do.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=127505
		Shell shell = getShell();
		if (shell != null && !shell.isDisposed()) {
			saveDialogBounds(shell);
		}

		boolean closed = super.close();

		for (Resource resource : this.resources) {
			resource.dispose();
		}
		this.resources.clear();

		return closed;
	}

	private void saveDialogBounds(Shell shell) {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			int offsetRight = 0;
			int offsetTop = 0;
			Shell parent = getParentShell();
			if (parent != null) {
				Rectangle parentBounds = parent.getBounds();
				Rectangle shellBounds = shell.getBounds();
				offsetRight = parentBounds.x + parentBounds.width - (shellBounds.x + shellBounds.width);
				offsetTop = shellBounds.y - parentBounds.y;
			}
			String prefix = getClass().getName();
			settings.put(prefix + DIALOG_OFFSET_RIGHT, offsetRight);
			settings.put(prefix + DIALOG_OFFSET_TOP, offsetTop);
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		Color color = JFaceResources.getColorRegistry().get(POPUP_COLOR_BG);
		newShell.setBackground(color);
		newShell.setAlpha(170);
	}

	@Override
	protected Control createContents(Composite parent) {
		Font font = JFaceResources.getFont(POPUP_FONT);
		FontData[] defaultFontData = font.getFontData();
		Color foregroundColor = JFaceResources.getColorRegistry().get(POPUP_COLOR_FG);

		Composite contents = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(contents);
		contents.setBackground(parent.getBackground());
		hookDoubleClickListener(contents);
		hookPopupMenu(contents);

		this.shortcutLabel = new Label(contents, SWT.CENTER);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(this.shortcutLabel);
		FontData fontData = new FontData(defaultFontData[0].getName(), defaultFontData[0].getHeight() * POPUP_FONT_SIZEFACTOR_KEY, SWT.BOLD);
		Font shortcutFont = new Font(getShell().getDisplay(), fontData);
		this.resources.add(shortcutFont);
		this.shortcutLabel.setBackground(parent.getBackground());
		this.shortcutLabel.setForeground(foregroundColor);
		this.shortcutLabel.setFont(shortcutFont);
		this.shortcutLabel.setText(this.shortcut);
		hookDoubleClickListener(this.shortcutLabel);
		hookPopupMenu(this.shortcutLabel);

		this.shortcutNameLabel = new Label(contents, SWT.CENTER);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(this.shortcutNameLabel);
		fontData = new FontData(defaultFontData[0].getName(), defaultFontData[0].getHeight() * POPUP_FONT_SIZEFACTOR_KEY_LABEL, SWT.NORMAL);
		shortcutFont = new Font(getShell().getDisplay(), fontData);
		this.resources.add(shortcutFont);
		this.shortcutNameLabel.setFont(shortcutFont);
		this.shortcutNameLabel.setBackground(parent.getBackground());
		this.shortcutNameLabel.setForeground(foregroundColor);
		this.shortcutNameLabel.setText(this.shortcutName);
		hookDoubleClickListener(this.shortcutNameLabel);
		hookPopupMenu(this.shortcutNameLabel);

		if (this.shortcutDescription != null) {
			this.shortcutDescriptionLabel = new Label(contents, SWT.CENTER);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(this.shortcutDescriptionLabel);
			fontData = new FontData(defaultFontData[0].getName(), (int) (defaultFontData[0].getHeight() * 1.3), SWT.NORMAL);
			shortcutFont = new Font(getShell().getDisplay(), fontData);
			this.resources.add(shortcutFont);
			this.shortcutDescriptionLabel.setFont(shortcutFont);
			this.shortcutDescriptionLabel.setBackground(parent.getBackground());
			this.shortcutDescriptionLabel.setForeground(foregroundColor);
			this.shortcutDescriptionLabel.setText(this.shortcutDescription);
			hookDoubleClickListener(this.shortcutDescriptionLabel);
			hookPopupMenu(this.shortcutDescriptionLabel);
		}

		return contents;
	}

	private void hookPopupMenu(Control control) {
		MenuManager menuManager = new MenuManager();

		menuManager.add(new MoveAction());
		menuManager.add(new Separator());
		menuManager.add(new GoToPrefPageAction(getParentShell(), ShortcutPreferencePage.ID, ShortcutPreferencePage.ADDITIONAL_PAGES));

		Menu menu = menuManager.createContextMenu(control);
		control.setMenu(menu);
		menu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				ShortcutPopup.this.readyToClose = false;
			}

			@Override
			public void menuHidden(MenuEvent e) {
				scheduleClose();
			}
		});
	}

	private void hookDoubleClickListener(Control control) {
		control.addListener(SWT.MouseDoubleClick, this.moveListener);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		int offsetX = MARGIN_RIGHT;
		int offsetY = MARGIN_TOP;
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			try {
				offsetX = settings.getInt(getClass().getName() + DIALOG_OFFSET_RIGHT);
				offsetY = settings.getInt(getClass().getName() + DIALOG_OFFSET_TOP);
			} catch (NumberFormatException e) {
			}
		}

		Point result = getDefaultLocation(initialSize, offsetX, offsetY);
		return result;
	}

	private IDialogSettings getDialogSettings() {
		String sectionName = getClass().getName() + "_dialogOffset"; //$NON-NLS-1$
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(sectionName);
		if (section == null) {
			section = settings.addNewSection(sectionName);
		}
		return section;
	}

	private Point getDefaultLocation(Point initialSize, int offsetRight, int offsetTop) {
		Composite parent = getShell().getParent();

		Monitor monitor = getShell().getDisplay().getPrimaryMonitor();
		if (parent != null) {
			monitor = parent.getMonitor();
		}

		Rectangle monitorBounds = monitor.getClientArea();
		Point topRight;
		if (parent != null) {
			Rectangle parentBounds = parent.getBounds();
			topRight = new Point(parentBounds.x + parentBounds.width, parentBounds.y);
		} else {
			topRight = new Point(monitorBounds.x + monitorBounds.width, monitorBounds.y);
		}

		return new Point(topRight.x - (initialSize.x + offsetRight), //
				Math.max(monitorBounds.y, Math.min(topRight.y + offsetTop, monitorBounds.y + monitorBounds.height - initialSize.y)));
	}

	private final class MoveListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			new MoveAction().run();
		}
	}

	private final class MoveAction extends Action {

		MoveAction() {
			super(Messages.Popup_move_xmit, AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			ShortcutPopup.this.readyToClose = false;
			performTrackerAction(SWT.NONE);
			scheduleClose();
		}

		private void performTrackerAction(int style) {
			Shell shell = getShell();
			if (shell == null || shell.isDisposed()) {
				return;
			}

			Tracker tracker = new Tracker(shell.getDisplay(), style);
			tracker.setStippled(true);
			Rectangle[] r = new Rectangle[] { shell.getBounds() };
			tracker.setRectangles(r);

			if (tracker.open()) {
				if (!shell.isDisposed()) {
					shell.setBounds(tracker.getRectangles()[0]);
				}
			}
			tracker.dispose();
		}
	}

	private static final class GoToPrefPageAction extends Action {

		private final Shell shell;
		private final String pageId;
		private final String[] pagesToDisplay;

		GoToPrefPageAction(Shell shell, String pageId, List<String> additionalPages) {
			super(Messages.Popup_showPreferences_xmit, AS_PUSH_BUTTON);
			this.shell = shell;
			this.pageId = pageId;

			Set<String> pages = new HashSet<String>(additionalPages);
			pages.add(pageId);
			this.pagesToDisplay = pages.toArray(new String[pages.size()]);
		}

		@Override
		public void run() {
			if (this.shell == null || this.shell.isDisposed()) {
				return;
			}
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(this.shell, this.pageId, this.pagesToDisplay, null);
			dialog.open();
		}

	}

}
