/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.plugins.ade_manager.registry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.EventDispatcher;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.event.ScriptCreationEvent;
import org.citydb.plugins.ade_manager.registry.install.DBScriptInstaller;
import org.citydb.plugins.ade_manager.registry.install.DBScriptInstallerFactory;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.DBScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.DBScriptGeneratorFactory;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManagerFactory;
import org.citydb.registry.ObjectRegistry;

public class ADERegistrationController {
	private final Logger log = Logger.getInstance();
	private final EventDispatcher eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();	
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	private final ADEManagerPlugin plugin;
	private Connection connection;
	
	public ADERegistrationController(ADEManagerPlugin plugin) {
		this.plugin = plugin;
    }
	
	public void initDBConneciton() throws ADERegistrationException {
		try {
			connection = dbPool.getConnection();
			// disable database auto-commit in order to enable rolling back database transactions
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to connect to database.");
		}	
	}

	public boolean registerADE() throws ADERegistrationException {
		log.info("ADE registration started...");

		// import ADE metadata from schema mapping file into database	
		log.info("Importing ADE metadata into database...");
		ADEMetadataManager adeMetadataManager = null;
		try {
			adeMetadataManager = new ADEMetadataManager(connection, plugin.getConfig());
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to initialize ADE metadata manager.", e);
		}
		try {						
			adeMetadataManager.importADEMetadata();
		} catch (SQLException e) {				
			throw new ADERegistrationException("Failed to import ADE metadata into database.", e);
		}		
		
		// create database tables, FKs, indexes, and sequences etc. 
		log.info("Creating ADE database schema...");
		ADEDBSchemaManager adeDatabasSchemaManager = ADEDBSchemaManagerFactory.getInstance()
				.createADEDatabaseSchemaManager(connection, plugin.getConfig());
		try {	
			adeDatabasSchemaManager.createADEDatabaseSchema();
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to create ADE database schema.", e);
		} 	
		
		// create and install delete-functions.
		log.info("Creating and installing delete functions...");
		try {	
			DBSQLScript deleteScript = createDeleteScripts();
			installDBScript(deleteScript);			
			eventDispatcher.triggerEvent(new ScriptCreationEvent(deleteScript, true, this));
		} catch (ADERegistrationException e) {
			log.info("Failed to create and install delete functions in database (skipped).");
		} 	
		
		// create and install envelope-functions.
		log.info("Creating and installing envelope functions...");
		try {	
			DBSQLScript envelopeScript = createEnvelopeScripts();
			installDBScript(envelopeScript);	
			eventDispatcher.triggerEvent(new ScriptCreationEvent(envelopeScript, true, this));
		} catch (ADERegistrationException e) {
			log.info("Failed to create and install envelope functions into database (skipped).");
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
				.createADEDatabaseSchemaManager(connection, plugin.getConfig());
		// Step 1: Cleanup ADE data content by calling the corresponding delete-functions
		log.info("Deleting ADE data content...");
		try {
			adeDatabasSchemaManager.cleanupADEData(adeId);
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to clean up ADE data.", e);
		}
		
		// Step 2: Dropping ADE database schema and delete-functions	
		log.info("Dropping ADE database schema and all delete functions...");
		try {	
			adeDatabasSchemaManager.dropADEDatabaseSchema(adeId);
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to drop ADE database schema.", e);
		} 
		
		// Step 3: Removing ADE metadata
		log.info("Removing ADE metadata");
		ADEMetadataManager adeMetadataManager = null;
		try {
			adeMetadataManager = new ADEMetadataManager(connection, plugin.getConfig());
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to initialize ADE metadata manager.", e);
		}	
		try {
			adeMetadataManager.deleteADEMetadata(adeId);
		} catch (SQLException e) {	
			throw new ADERegistrationException("Failed to delete ADE metadata from database.", e);
		} 		

		// Step 4: re-create and install delete-functions
		log.info("Re-creating and installing delete functions...");
		try {	
			DBSQLScript deleteScript = createDeleteScripts();
			installDBScript(deleteScript);			
			eventDispatcher.triggerEvent(new ScriptCreationEvent(deleteScript, true, this));
		} catch (ADERegistrationException e) {
			log.info("Failed to create and install delete functions in database (skipped).");
		} 

		// Step 5: re-create and install envelope-functions.
		log.info("Re-Creating and installing envelope functions...");
		try {	
			DBSQLScript envelopeScript = createEnvelopeScripts();
			installDBScript(envelopeScript);	
			eventDispatcher.triggerEvent(new ScriptCreationEvent(envelopeScript, true, this));
		} catch (ADERegistrationException e) {
			log.info("Failed to create and install envelope functions in database (skipped).");
		} 
		
		return true;
	}
	
	public List<ADEMetadataInfo> queryRegisteredADEs() throws ADERegistrationException {	
		List<ADEMetadataInfo> adeList = new ArrayList<ADEMetadataInfo>();				
		ADEMetadataManager adeMetadataManager = null;
		try {
			adeMetadataManager = new ADEMetadataManager(connection, plugin.getConfig());
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to initialize ADE metadata manager.", e);
		}
		try {
			adeList = adeMetadataManager.getADEMetadata();
		} catch (SQLException e) {		
			throw new ADERegistrationException("Failed to query ADEs that have already been registered in the database.", e);
		} 
		
		return adeList;
	}
	
	public DBSQLScript createDeleteScripts() throws ADERegistrationException {
		ADEMetadataManager adeMetadataManager = null;
		try {
			adeMetadataManager = new ADEMetadataManager(connection, plugin.getConfig());
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to initialize ADE metadata manager.", e);
		}
		
		log.info("Creating delete functions for the current 3DCityDB instance (this process may take a while for Oracle)...");
		DBSQLScript deleteScript = null;
		DBScriptGenerator deleteScriptGenerator = DBScriptGeneratorFactory.getInstance().
				createDeleteScriptGenerator(connection, plugin.getConfig(), adeMetadataManager);
		
		try {
			deleteScript = deleteScriptGenerator.generateDBScript();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to create delete functions for the current 3DCityDB instance", e);
		}		
		
		log.info("Delete functions successfully created for the current 3DCityDB instance.");
		
		return deleteScript;
	}

	public DBSQLScript createEnvelopeScripts() throws ADERegistrationException {
		ADEMetadataManager adeMetadataManager = null;
		try {
			adeMetadataManager = new ADEMetadataManager(connection, plugin.getConfig());
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to initialize ADE metadata manager.", e);
		}
		
		log.info("Creating envelope functions for the current 3DCityDB instance...");
		DBSQLScript envelopeScript = null;
		DBScriptGenerator envelopeScriptGenerator = DBScriptGeneratorFactory.getInstance()
				.createEnvelopeScriptGenerator(connection, plugin.getConfig(), adeMetadataManager);
		try {
			envelopeScript = envelopeScriptGenerator.generateDBScript();			
		} catch (SQLException e) {
			throw new ADERegistrationException("Failed to create envelope functions for the current 3DCityDB instance.", e);
		}		
		log.info("Envelope functions is successfully created for the current 3DCityDB instance.");
				
		return envelopeScript;
	}
	
	public void installDBScript(DBSQLScript dbScript) throws ADERegistrationException {		
		DBScriptInstaller scriptInstaller = DBScriptInstallerFactory.getInstance().createScriptInstaller(connection);
		try {
			scriptInstaller.installScript(dbScript);
		} catch (SQLException e) {
			throw new ADERegistrationException("Error occurred while installing the generated database script.", e);
		}				
	}

	public void closeDBConnection() {
		try {
			if (connection != null) {
				connection.close();	
			}
						
		} catch (SQLException e) {
			log.error("Failed to close database connection.");
		}
	}
	
	public void commitTransactions() throws ADERegistrationException {
		try {
			if (connection != null) {
				connection.commit();
			}  		
    	} catch (SQLException e) {
			throw new ADERegistrationException("Failed to execute database transaction.", e);
		} 	
	}
	
	public void rollbackTransactions() {
		if (connection != null) {
			try {
				connection.rollback();
			} catch (SQLException e) {
				log.error("Failed to rollback database transactions.");
			}	
		}
	}
		
}
