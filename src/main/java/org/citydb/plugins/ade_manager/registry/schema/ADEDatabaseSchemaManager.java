package org.citydb.plugins.ade_manager.registry.schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.ADERegistrationImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.schema.helper.SQLScriptRunner;
import org.citydb.plugins.ade_manager.util.PathResolver;

public class ADEDatabaseSchemaManager extends ADERegistrationImpl {
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	
	public ADEDatabaseSchemaManager(Connection connection, ConfigImpl config) {
		this.connection = connection;
		this.config = config;
	}
	
	public void createADEDatabaseSchema() throws SQLException {
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();		
		SQLScriptRunner adeSQLRunner = new SQLScriptRunner(connection);			
		try {	
			String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, databaseType);	
			String createDBscriptString = new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
			int srid = dbPool.getActiveDatabaseAdapter().getUtil().getDatabaseInfo().getReferenceSystem().getSrid();
			adeSQLRunner.runScript(createDBscriptString, srid);
		} catch (SQLException | IOException e) {
			throw new SQLException("Error occurred while reading and running ADE database creation script", e);
		} 
	}

	public void dropADEDatabaseSchema(String adeId) throws SQLException {
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);
		SQLScriptRunner adeSQLRunner = new SQLScriptRunner(connection);		
		try {			
			adeSQLRunner.runScript(adeMetadataManager.getDropDBScript(adeId));
		} catch (SQLException e) {		
			throw new SQLException("Error occurred while reading and running ADE database drop script", e);
		} 
	}
	
	
}
