package org.citydb.plugins.ade_manager.registry.install;

import java.sql.SQLException;

import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;

public interface DBScriptInstaller {
	public void installScript(DBSQLScript databaseScript) throws SQLException;
}
