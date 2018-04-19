package org.citydb.plugins.ade_manager.registry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.database.DatabaseController;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.version.DatabaseVersionException;
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
	private final DatabaseController databaseController = ObjectRegistry.getInstance().getDatabaseController();	
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	private ConfigImpl config;
	private Connection connection;
	
	public ADERegistrationController(ConfigImpl config) {
		this.config = config;	
    }
	
	private void initDBConneciton() throws ADERegistrationException {
		try {
			connection = DatabaseConnectionPool.getInstance().getConnection();
			// disable database auto-commit in order to enable rolling back database transactions
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to initialize a database connection (connection = NULL).");			
		}	
	}

	public boolean registerADE() throws ADERegistrationException {
		LOG.info("ADE Registration started...");
		initDBConneciton();

		// import ADE metadata from schema mapping file into database	
		LOG.info("Importing ADE metadata into database...");			
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);
		try {						
			adeMetadataManager.importADEMetadata();
		} catch (SQLException e) {				
			throw new ADERegistrationException("Error occurred while importing ADE metadata into database", e);
		}		
		
		// create database tables, FKs, indexes, and sequences etc.
		LOG.info("Creating ADE database schema...");		
		ADEDBSchemaManager adeDatabasSchemaManager = ADEDBSchemaManagerFactory.getInstance()
				.createADEDatabaseSchemaManager(connection, config);
		try {	
			adeDatabasSchemaManager.createADEDatabaseSchema();
		} catch (SQLException e) {
			throw new ADERegistrationException("Error occurred while creating ADE database schema", e);
		} 
		
		// commit database transactions
		try {
			adeMetadataManager.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to import ADE metadata into database", e);
		}
		
		try {
			adeDatabasSchemaManager.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to create ADE database schema", e);
		}		
		
		createDeleteScripts(true);
		
		// database re-connection is required for completing the ADE registration process
		LOG.info("ADE registration is completed and will take effect after reconnecting to the database.");	
		if (dbPool.isConnected()) {
			dbPool.disconnect();
			try {
				databaseController.connect(true);
			} catch (DatabaseConfigurationException | DatabaseVersionException | SQLException e) {
				throw new ADERegistrationException("Failed to reconnect to the database", e);
			}
		}	
		
		return true;
	}
	
	public boolean deregisterADE(String adeId) throws ADERegistrationException {
		LOG.info("Start deleting metadata of the selected ADE from database...");
		/* 
		 * In order to remove an ADE from a 3DCityDB instance, the following processing steps are required:
		 * 1) clean up all the ADE data from the CityGML and ADE tables by calling a cleanup_[ade_name] function. 
		 * 2) drop ADE database schema by calling the "drop_ade_db" script, which is persisted in the ADE metadata table.  
		 * 3) Re-generate the entire delete-functions.
		 * 4) Delete ADE metadata from the respective 3DCityDB's Metadata tables
		 */
		initDBConneciton();
		// TODO
		// Step 1: Cleanup ADE data content by calling the corresponding delete-functions
		
		// Step 2: Dropping ADE database schema and delete-functions
		ADEDBSchemaManager adeDatabasSchemaManager = ADEDBSchemaManagerFactory.getInstance()
				.createADEDatabaseSchemaManager(connection, config);	
		try {	
			adeDatabasSchemaManager.dropADEDatabaseSchema(adeId);
		} catch (SQLException e) {
			throw new ADERegistrationException("Error occurred while dropping ADE database schema", e);
		} 
		LOG.info("ADE database schema and all delete-functions successfully deleted.");
		
		// Step 3: cleanup ADE metadata
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);		
		try {
			adeMetadataManager.deleteADEMetadata(adeId);
		} catch (SQLException e) {	
			throw new ADERegistrationException("Error occurred while removing ADE metadata from database", e);
		} 		
		LOG.info("ADE Metadata successfully deleted");	

		// commit database transactions
		try {
			adeDatabasSchemaManager.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to drop ADE database schema", e);
		}	

		try {
			adeMetadataManager.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to remove ADE metadata from database", e);
		}
		
		// Step 4: re-create and install delete-functions
		createDeleteScripts(true);	
		
		LOG.info("ADE Deregistration is completed.");
		return true;
	}
	
	public List<ADEMetadataInfo> queryRegisteredADEs() throws ADERegistrationException {
		initDBConneciton();		
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
		initDBConneciton();
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
		initDBConneciton();

		DeleteScriptGenerator deleteScriptGenerator = DeleteScriptGeneratorFactory.getInstance().
				createDatabaseAdapter(connection, config);
		try {
			deleteScriptGenerator.installDeleteScript(scriptString);;
		} catch (SQLException e) {
			throw new ADERegistrationException("Error occurred while running the delete-script", e);
		}
		
		try {
			deleteScriptGenerator.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to install the delete-functions into the current database", e);
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
