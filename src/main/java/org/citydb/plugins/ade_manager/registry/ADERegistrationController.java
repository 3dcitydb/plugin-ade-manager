package org.citydb.plugins.ade_manager.registry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.config.project.database.DatabaseConfigurationException;
import org.citydb.database.DatabaseController;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.version.DatabaseVersionException;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataEntity;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.sqlrunner.ADEsqlScriptRunner;
import org.citydb.registry.ObjectRegistry;

public class ADERegistrationController implements EventHandler {
	private final Logger LOG = Logger.getInstance();
	private final DatabaseController databaseController;
	private final DatabaseConnectionPool dbPool;
	private ConfigImpl config;
	private Connection connection;
	
	public ADERegistrationController(ConfigImpl config) {
		this.config = config;
		this.databaseController = ObjectRegistry.getInstance().getDatabaseController();		
		this.dbPool = DatabaseConnectionPool.getInstance();		
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
		LOG.info("Importing metadata into database...");
		String adeSchemaMappingPath = config.getSchemaMappingPath();
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection);
		try {						
			adeMetadataManager.importADEMetadata(adeSchemaMappingPath);
		} catch (SQLException e) {				
			throw new ADERegistrationException("Error occurred while importing ADE metadata into database", e);
		}		
		
		// create database tables, FKs, indexes, and sequences etc.
		LOG.info("Create ADE database schema...");
		ADEsqlScriptRunner adeSQLRunner = new ADEsqlScriptRunner(connection);
		String createDbScriptPath = config.getCreateDbScriptPath();
		try {			
			int srid = dbPool.getActiveDatabaseAdapter().getUtil().getDatabaseInfo().getReferenceSystem().getSrid();
			adeSQLRunner.runScript(createDbScriptPath, srid);
		} catch (SQLException e) {
			throw new ADERegistrationException("Error occurred while executing the SQL script for creating ADE database schema", e);
		} 
		
		// commit database transactions
		try {
			adeMetadataManager.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to import ADE metadata into database", e);
		}
		
		try {
			adeSQLRunner.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to create ADE database schema", e);
		}		
		
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
		/* TODO
		 * In order to remove an ADE from a 3DCityDB instance, the following processing steps are required:
		 * 1) clean up all the ADE data from the CityGML and ADE tables by calling a cleanup_[ade_name] function. 
		 * 2) the drop_ade_db script shall be executed to remove ADE database schema, which can be persisted in the 
		 * 	  ADE metadata table through the registration process.  
		 * 3) Regenerate the entire delete-functions again, because the function like "delete_cityobject()" must be updated due to
		 *    deregistration of the ADE
		 * 4) Delete ADE metadata content from the 3DCityDB metadata tables
		 */
		initDBConneciton();
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection);
		try {
			adeMetadataManager.deleteADEMetadata(adeId);
		} catch (SQLException e) {	
			throw new ADERegistrationException("Error occurred while removing ADE metadata from database", e);
		} 
		
		LOG.info("Metadata have been successfully deleted. Start dropping database schema of the selected ADE...");	
		ADEsqlScriptRunner adeSQLRunner = new ADEsqlScriptRunner(connection);
		String dropDbScriptPath = config.getDropDbScriptPath();
		try {			
			adeSQLRunner.runScript(dropDbScriptPath);
		} catch (SQLException  e) {		
			throw new ADERegistrationException("Error occurred while dropping ADE database schema", e);
		} 

		// commit database transactions
		try {
			adeMetadataManager.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to remove ADE metadata from database", e);
		}
		
		try {
			adeSQLRunner.commit();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to drop ADE database schema", e);
		}	

		LOG.info("ADE Deregistration is completed.");
		return true;
	}
	
	public List<ADEMetadataEntity> queryRegisteredADEs() throws ADERegistrationException {
		initDBConneciton();		
		List<ADEMetadataEntity> adeList = new ArrayList<ADEMetadataEntity>();		
		
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection);
		try {
			adeList = adeMetadataManager.queryADEMetadata();
		} catch (SQLException e) {		
			throw new ADERegistrationException("Failed to query those ADEs that have already been registered in the connected database",e);
		} 
		
		return adeList;
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

	@Override
	public void handleEvent(Event event) throws Exception {
		//
	}	
	
}
