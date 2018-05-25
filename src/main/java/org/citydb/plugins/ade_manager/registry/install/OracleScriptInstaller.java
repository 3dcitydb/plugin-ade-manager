package org.citydb.plugins.ade_manager.registry.install;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class OracleScriptInstaller extends DefaultDBScriptInstaller {

	public OracleScriptInstaller(Connection connection) {
		super(connection);
	}

	@Override
	protected void executeInstall(String scriptString) throws SQLException {
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			stmt.execute(processScriptString(scriptString));
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}
	
	private String processScriptString(String scriptString) {
		return scriptString.replaceAll("/$", "");
	}

}
