package org.citydb.plugins.ade_manager.registry.pkg;

import java.sql.SQLException;

import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;

public interface DBScriptGenerator {
	public DBSQLScript generateDBScript() throws SQLException;
}
