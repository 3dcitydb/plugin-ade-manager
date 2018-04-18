package org.citydb.plugins.ade_manager.registry.schema;

import java.sql.Connection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.oracle.OracleADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.adapter.postgis.PostgisADEDBSchemaManager;

public class ADEDBSchemaManagerFactory {
	private static ADEDBSchemaManagerFactory instance;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	private ADEDBSchemaManagerFactory() {}

	public static synchronized ADEDBSchemaManagerFactory getInstance() {
		if (instance == null)
			instance = new ADEDBSchemaManagerFactory();
		
		return instance;
	}
	
	public ADEDBSchemaManager createADEDatabaseSchemaManager(Connection connection, ConfigImpl config) {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleADEDBSchemaManager(connection, config);
		case POSTGIS:
			return new PostgisADEDBSchemaManager(connection, config);
		}		
		return null;
	}
}
