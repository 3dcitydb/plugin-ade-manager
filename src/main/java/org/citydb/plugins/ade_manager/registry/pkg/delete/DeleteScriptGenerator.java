package org.citydb.plugins.ade_manager.registry.pkg.delete;

import java.sql.SQLException;

import org.citydb.plugins.ade_manager.registry.ADERegistrationProcessor;

public interface DeleteScriptGenerator extends ADERegistrationProcessor {
	public void doProcess(boolean immediateInstall) throws SQLException;
}
