package org.citydb.plugins.ade_manager.registry.install;

import java.sql.Connection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;

public class DBScriptInstallerFactory {
	private static DBScriptInstallerFactory instance;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	private DBScriptInstallerFactory() {}

	public static synchronized DBScriptInstallerFactory getInstance() {
		if (instance == null)
			instance = new DBScriptInstallerFactory();
		
		return instance;
	}
	
	public DBScriptInstaller createScriptInstaller(Connection connection) {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleScriptInstaller(connection);
		case POSTGIS:
			return new PostgisScriptInstaller(connection);
		}		
		return null;
	}
	
}
