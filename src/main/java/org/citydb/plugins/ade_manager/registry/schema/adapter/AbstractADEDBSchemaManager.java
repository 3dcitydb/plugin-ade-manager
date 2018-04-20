package org.citydb.plugins.ade_manager.registry.schema.adapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencingEntry;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.SQLScriptRunner;
import org.citydb.plugins.ade_manager.util.PathResolver;

public abstract class AbstractADEDBSchemaManager implements ADEDBSchemaManager {
	protected final Logger LOG = Logger.getInstance();
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	protected final Connection connection;
	protected final ConfigImpl config;
	
	public AbstractADEDBSchemaManager(Connection connection, ConfigImpl config) {
		this.connection = connection;
		this.config = config;
	}
	
	public void createADEDatabaseSchema() throws SQLException {
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		try {	
			String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, databaseType);	
			String createDBscriptString = new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
			SQLScriptRunner.getInstance().runScript(processScript(createDBscriptString), connection);
		} catch (SQLException | IOException e) {
			throw new SQLException("Error occurred while reading and running ADE database creation script", e);
		}
	}
	
	public void dropADEDatabaseSchema(String adeId) throws SQLException {
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);
		try {
			SQLScriptRunner.getInstance().runScript(adeMetadataManager.getDropDBScript(adeId), connection);
			dropCurrentDeleteFunctions();
		} catch (SQLException e) {		
			throw new SQLException("Error occurred while dropping the current delete functions", e);
		} 
	}
	
	protected abstract String processScript(String inputScript) throws SQLException;
	protected abstract void dropCurrentDeleteFunctions() throws SQLException;
	

	public abstract List<String> query_selfref_fk(String tableName, String schemaName) throws SQLException;
	public abstract List<MnRefEntry> query_ref_fk(String tableName, String schemaName) throws SQLException;
	public abstract List<ReferencingEntry> query_ref_tables_and_columns(String tableName, String schemaName) throws SQLException;
	public abstract String query_ref_to_parent_fk(String tableName, String schemaName) throws SQLException;
	public abstract List<ReferencedEntry> query_ref_to_fk(String tableName, String schemaName) throws SQLException;
	
}
