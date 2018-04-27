package org.citydb.plugins.ade_manager.registry.query.sql;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;

public class SQLBuilderFactory {
	private static SQLBuilderFactory instance;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	private SQLBuilderFactory() {}

	public static synchronized SQLBuilderFactory getInstance() {
		if (instance == null)
			instance = new SQLBuilderFactory();
		
		return instance;
	}
	
	public SQLBuilder createSQLBuilder() {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleSQLBuilder();
		case POSTGIS:
			return new PostgisSQLBuilder();
		}		
		return null;
	}
}
