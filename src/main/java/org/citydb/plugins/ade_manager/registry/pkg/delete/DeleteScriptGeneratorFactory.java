package org.citydb.plugins.ade_manager.registry.pkg.delete;

import java.sql.Connection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.oracle.OracleDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.postgis.PostgisDeleteGeneratorGenerator;

public class DeleteScriptGeneratorFactory {
	private static DeleteScriptGeneratorFactory instance;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	private DeleteScriptGeneratorFactory() {}

	public static synchronized DeleteScriptGeneratorFactory getInstance() {
		if (instance == null)
			instance = new DeleteScriptGeneratorFactory();
		
		return instance;
	}
	
	public DeleteScriptGenerator createDatabaseAdapter(Connection connection, ConfigImpl config) {
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
