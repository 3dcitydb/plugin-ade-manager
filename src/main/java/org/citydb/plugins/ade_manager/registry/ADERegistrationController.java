package org.citydb.plugins.ade_manager.registry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.EventDispatcher;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.event.ScriptCreationEvent;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteScriptGeneratorFactory;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManagerFactory;
import org.citydb.registry.ObjectRegistry;

public class ADERegistrationController {
	private final Logger LOG = Logger.getInstance();
	private final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();	
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	private ConfigImpl config;
	private Connection connection;
	
	public ADERegistrationController(ConfigImpl config) {
		this.config = config;	
    }
	
	public void initDBConneciton() throws ADERegistrationException {
		try {
			connection = dbPool.getConnection();
			// disable database auto-commit in order to enable rolling back database transactions
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to initialize a database connection (connection = NULL).");			
		}	
	}

	public boolean registerADE() throws ADERegistrationException {
		LOG.info("ADE Registration started...");

		// import ADE metadata from schema mapping file into database	
		LOG.info("Importing ADE metadata into database...");			
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);
		try {						
			adeMetadataManager.importADEMetadata();
		} catch (SQLException e) {				
			throw new ADERegistrationException("Failed to import ADE metadata into database", e);
		}		
		
		// create database tables, FKs, indexes, and sequences etc. 
		LOG.info("Creating ADE database schema...");		
		ADEDBSchemaManager adeDatabasSchemaManager = ADEDBSchemaManagerFactory.getInstance()
				.createADEDatabaseSchemaManager(connection, config);
		try {	
			adeDatabasSchemaManager.createADEDatabaseSchema();
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to create ADE database schema", e);
		} 	
		
		// create and install delete-functions.
		LOG.info("Creating and installing delete-function...");
		try {	
			createDeleteScripts(true);
		} catch (ADERegistrationException e) {
			LOG.info("Failed to create and install delete-script into database. (Skipped)");
		} 	

		return true;
	}
	
	public boolean deregisterADE(String adeId) throws ADERegistrationException {		
		/* 
		 * In order to remove an ADE from a 3DCityDB instance, the following processing steps are required:
		 * 1) clean up all the ADE data from the CityGML and ADE tables by calling a cleanup_[ade_name] function. 
		 * 2) drop ADE database schema by calling the "drop_ade_db" script, which is persisted in the ADE metadata table.  
		 * 3) Re-generate the entire delete-functions.
		 * 4) Delete ADE metadata from the respective 3DCityDB's Metadata tables
		 */
		ADEDBSchemaManager adeDatabasSchemaManager = ADEDBSchemaManagerFactory.getInstance()
				.createADEDatabaseSchemaManager(connection, config);
		// Step 1: Cleanup ADE data content by calling the corresponding delete-functions
		LOG.info("Deleting ADE data content...");
		try {
			adeDatabasSchemaManager.cleanupADEData(adeId);
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to clean up ADE data", e);
		}
		
		// Step 2: Dropping ADE database schema and delete-functions	
		LOG.info("Dropping ADE database schema and all delete-functions...");
		try {	
			adeDatabasSchemaManager.dropADEDatabaseSchema(adeId);
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to drop ADE database schema", e);
		} 
		
		// Step 3: Removing ADE metadata
		LOG.info("Removing ADE Metadata");
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);		
		try {
			adeMetadataManager.deleteADEMetadata(adeId);
		} catch (SQLException e) {	
			throw new ADERegistrationException("Failed to delete ADE metadata from database", e);
		} 		

		// Step 4: re-create and install delete-functions
		LOG.info("Re-creating and installing delete-function...");
		try {	
			createDeleteScripts(true);
		} catch (ADERegistrationException e) {
			LOG.info("Failed to create and install delete-script into database. (Skipped)");
		} 

		return true;
	}
	
	public List<ADEMetadataInfo> queryRegisteredADEs() throws ADERegistrationException {	
		List<ADEMetadataInfo> adeList = new ArrayList<ADEMetadataInfo>();				
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);
		try {
			adeList = adeMetadataManager.queryADEMetadata();
		} catch (SQLException e) {		
			throw new ADERegistrationException("Failed to query those ADEs that have already been registered in the connected database",e);
		} 
		
		return adeList;
	}
	
	public void createDeleteScripts(boolean autoInstall) throws ADERegistrationException {
		LOG.info("Start creating delete-script for the current 3DCityDB instance...");
		String deleteScript = null;
		DeleteScriptGenerator deleteScriptGenerator = DeleteScriptGeneratorFactory.getInstance().
				createDatabaseAdapter(connection, config);
		try {
			deleteScript = deleteScriptGenerator.generateDeleteScript();			
			eventDispatcher.triggerEvent(new ScriptCreationEvent(deleteScript, autoInstall, this));
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to create delete-script for the current 3DCityDB instance", e);
		}		
		LOG.info("Delete-script is successfully created for the current 3DCityDB database.");
		
		if (autoInstall)
			installDeleteScript(deleteScript);
	}
	
	public void installDeleteScript(String scriptString) throws ADERegistrationException {
		LOG.info("Start installing delete-script for the current 3DCityDB instance...");
		DeleteScriptGenerator deleteScriptGenerator = DeleteScriptGeneratorFactory.getInstance().
				createDatabaseAdapter(connection, config);
		try {
			deleteScriptGenerator.installDeleteScript(scriptString);;
		} catch (SQLException e) {
			throw new ADERegistrationException("Error occurred while installing the generated delete-script", e);
		}
		
		LOG.info("Delete-script is successfully installed into the connected database.");
	}

	public void closeDBConnection() {
		try {
			if (connection != null) {
				connection.close();	
			}
						
		} catch (SQLException e) {
			LOG.error("Failed to close databse connection.");
		}
	}
	
	public void commitTransactions() throws ADERegistrationException {
		try {
			if (connection != null) {
				connection.commit();
			}  		
    	} catch (SQLException e) {
			throw new ADERegistrationException("Failed to exeute the database transaction", e);
		} 	
	}
	
	public void rollbackTransactions() {
		if (connection != null) {
			try {
				connection.rollback();
			} catch (SQLException e) {
				LOG.error("Failed to roll back database transactions.");
			}	
		}
	}
		
}
