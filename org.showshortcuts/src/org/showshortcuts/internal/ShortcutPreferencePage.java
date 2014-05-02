package org.showshortcuts.internal;

import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_MOUSE_TRIGGER_ENABLED;
import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_SHORTCUTS_ENABLED;
import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_SHOW_DESCRIPTION;
import static org.showshortcuts.internal.ShortcutPreferenceInitializer.PREF_KEY_TIME_TO_CLOSE;
import static org.showshortcuts.internal.ShortcutPreferenceInitializer.THEME_CATEGORY;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.showshortcuts.internal.l10n.Messages;

/**
 * Preference page to change shortcut visualizer settings
 *
 * @author d031150
 */
public class ShortcutPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String PAGE_ID_KEYS = "org.eclipse.ui.preferencePages.Keys"; //$NON-NLS-1$
	private static final String PAGE_ID_COLORS_AND_FONTS = "org.eclipse.ui.preferencePages.ColorsAndFonts"; //$NON-NLS-1$

	static final String ID = Activator.PLUGIN_ID + ".preferencePage"; //$NON-NLS-1$
	static final List<String> ADDITIONAL_PAGES = Arrays.asList(PAGE_ID_KEYS, PAGE_ID_COLORS_AND_FONTS);

	public ShortcutPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected void createFieldEditors() {
		Composite editorParent = getFieldEditorParent();

		{
			Label triggeredBy = new Label(editorParent, SWT.NONE);
			triggeredBy.setText(Messages.PrefPage_showCommandsTriggeredBy_xfld);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(triggeredBy);
			{
				BooleanFieldEditor editor = new BooleanFieldEditor(//
						PREF_KEY_SHORTCUTS_ENABLED, Messages.PrefPage_shortcuts_enabled_xckl, editorParent);
				GridDataFactory.fillDefaults().indent(convertHorizontalDLUsToPixels(10), SWT.DEFAULT)
						.applyTo(editor.getDescriptionControl(editorParent));
				addField(editor);
			}
			{
				BooleanFieldEditor editor = new BooleanFieldEditor(//
						PREF_KEY_MOUSE_TRIGGER_ENABLED, Messages.PrefPage_mouse_enabled_xckl, editorParent);
				GridDataFactory.fillDefaults().indent(convertHorizontalDLUsToPixels(10), SWT.DEFAULT)
						.applyTo(editor.getDescriptionControl(editorParent));
				addField(editor);
			}
		}

		{
			Label separator = new Label(editorParent, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(separator);
		}

		{
			BooleanFieldEditor editor = new BooleanFieldEditor(//
					PREF_KEY_SHOW_DESCRIPTION, Messages.PrefPage_showCommandDescription_xckl, editorParent);
			addField(editor);
		}
		{
			IntegerFieldEditor editor = new IntegerFieldEditor(//
					PREF_KEY_TIME_TO_CLOSE, Messages.PrefPage_timeToClose_xfld, editorParent);
			editor.setValidRange(100, 60 * 1000);
			Text textControl = editor.getTextControl(editorParent);
			textControl.setToolTipText(Messages.PrefPage_timeToClose_xtol);
			editor.getLabelControl(editorParent).setToolTipText(textControl.getToolTipText());
			addField(editor);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER)
					.hint(convertWidthInCharsToPixels(textControl.getTextLimit() + 1), -1).applyTo(textControl);
		}

		{
			Label separator = new Label(editorParent, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(separator);
		}

		{
			PreferenceLinkArea linkArea = new PreferenceLinkArea(editorParent, SWT.NONE, PAGE_ID_COLORS_AND_FONTS,
					Messages.PrefPage_configureVisuals_xlnk, //
					(IWorkbenchPreferenceContainer) getContainer(), null);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(linkArea.getControl());

			// set preference strings for our category so that pref page shows up pre-expanded
			IPreferenceStore store = JFacePreferences.getPreferenceStore();
			store.setValue("ColorsAndFontsPreferencePage.selectedElement", //$NON-NLS-1$
					toCategoryPreferenceString(THEME_CATEGORY));
			store.setValue("ColorsAndFontsPreferencePage.expandedCategories", //$NON-NLS-1$
					toCategoryPreferenceString(THEME_CATEGORY));
		}
		{
			PreferenceLinkArea linkArea = new PreferenceLinkArea(editorParent, SWT.NONE, PAGE_ID_KEYS,
					Messages.PrefPage_configureKeys_xlnk, //
					(IWorkbenchPreferenceContainer) getContainer(), null);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(linkArea.getControl());
		}
	}

	private static String toCategoryPreferenceString(String categoryId) {
		return "T" + categoryId; //$NON-NLS-1$
	}

}
