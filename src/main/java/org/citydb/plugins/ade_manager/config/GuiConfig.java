package org.citydb.plugins.ade_manager.config;

import org.citydb.config.gui.components.SQLExportFilterComponent;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "GuiConfigType", propOrder = {
		"showRemoveADEWarning"
})
public class GuiConfig {
	private boolean showRemoveADEWarning = true;

	public boolean isShowRemoveADEWarning() {
		return showRemoveADEWarning;
	}

	public void setShowRemoveADEWarning(boolean showRemoveADEWarning) {
		this.showRemoveADEWarning = showRemoveADEWarning;
	}
}
