package org.citydb.plugins.ade_manager.registry.pkg;

import java.sql.Connection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.oracle.OracleDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.delete.postgis.PostgisDeleteGeneratorGenerator;

public class DBScriptGeneratorFactory {
	private static DBScriptGeneratorFactory instance;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	private DBScriptGeneratorFactory() {}

	public static synchronized DBScriptGeneratorFactory getInstance() {
		if (instance == null)
			instance = new DBScriptGeneratorFactory();
		
		return instance;
	}
	
	public DBScriptGenerator createDeleteScriptGenerator(Connection connection, ConfigImpl config) {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleDeleteScriptGenerator(connection, config);
		case POSTGIS:
			return new PostgisDeleteGeneratorGenerator(connection, config);
		}		
		return null;
	}
	
	public DBScriptGenerator createEnvelopeScriptGenerator(Connection connection, ConfigImpl config) {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleDeleteScriptGenerator(connection, config);
		case POSTGIS:
			return new PostgisDeleteGeneratorGenerator(connection, config);
		}		
		return null;
	}
}
