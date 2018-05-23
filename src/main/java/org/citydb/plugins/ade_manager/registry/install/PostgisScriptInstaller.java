package org.citydb.plugins.ade_manager.registry.install;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class PostgisScriptInstaller extends DefaultDBScriptInstaller {

	public PostgisScriptInstaller(Connection connection) {
		super(connection);
	}

	@Override
	protected void executeInstall(String scriptString) throws SQLException {
		CallableStatement cs = null;
		try {
			cs = connection.prepareCall(scriptString);
			cs.execute();
		}
		finally {
			if (cs != null)
				cs.close();
		}
	}



}
