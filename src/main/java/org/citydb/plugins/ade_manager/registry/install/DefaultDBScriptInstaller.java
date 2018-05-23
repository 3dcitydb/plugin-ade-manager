package org.citydb.plugins.ade_manager.registry.install;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;

public abstract class DefaultDBScriptInstaller implements DBScriptInstaller {
	protected final Connection connection;

	public DefaultDBScriptInstaller(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void installScript(DBSQLScript databaseScript) throws SQLException{
		List<String> sqlBlocks = databaseScript.getSQLBlocks();
		for (String sqlBlock: sqlBlocks) {
			executeInstall(sqlBlock);
		}	
	}
	
	protected abstract void executeInstall(String scriptString) throws SQLException;
}