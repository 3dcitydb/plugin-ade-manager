package org.citydb.plugins.ade_manager.registry.pkg.delete;


import java.sql.Connection;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.oracle.OracleDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.postgis.PostgisDeleteGeneratorGenerator;

public class DeleteScriptGeneratorFactory {
	private Connection conneciton;
	private ConfigImpl config;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	public DeleteScriptGeneratorFactory (Connection connection, ConfigImpl config) {
		this.conneciton = connection;
		this.config = config;
	}
	
	public DeleteScriptGenerator createDatabaseAdapter() {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleDeleteScriptGenerator(conneciton, config);
		case POSTGIS:
			return new PostgisDeleteGeneratorGenerator(conneciton, config);
		}		
		return null;
	}
}
