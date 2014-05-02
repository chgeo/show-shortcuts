package org.showshortcuts.internal.l10n;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.showshortcuts.internal.l10n.messages"; //$NON-NLS-1$

	public static String Popup_move_xmit;

	public static String Popup_showPreferences_xmit;

	public static String PrefPage_configureKeys_xlnk;
	public static String PrefPage_configureVisuals_xlnk;
	public static String PrefPage_shortcuts_enabled_xckl;

	public static String PrefPage_showCommandDescription_xckl;

	public static String PrefPage_showCommandsTriggeredBy_xfld;
	public static String PrefPage_mouse_enabled_xckl;
	public static String PrefPage_timeToClose_xfld;
	public static String PrefPage_timeToClose_xtol;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
