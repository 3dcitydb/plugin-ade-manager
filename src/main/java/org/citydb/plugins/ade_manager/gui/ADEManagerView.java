
package org.citydb.plugins.ade_manager.gui;

import java.awt.Component;

import javax.swing.Icon;

import org.citydb.plugin.extension.view.View;
import org.citydb.plugin.extension.view.ViewController;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;

public class ADEManagerView extends View {
	private final ADEManagerPanel component;
	
	public ADEManagerView(ViewController viewController, ADEManagerPlugin adeManagerPlugin) {
		component = new ADEManagerPanel(viewController, adeManagerPlugin);
	}
	
	@Override
	public String getLocalizedTitle() {
		return "ADE Manager";
	}

	@Override
	public Component getViewComponent() {
		return component;
	}

	@Override
	public String getToolTip() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return null;
	}
	
	public void loadSettings() {
		component.loadSettings();
	}
	
	public void saveSettings() {
		component.setSettings();
	}
	
	public void doTranslation() {
		component.doTranslation();
	}

}
