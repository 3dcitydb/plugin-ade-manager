/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2019
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
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
package org.citydb.plugins.ade_manager.registry.schema.adapter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.citydb.citygml.deleter.concurrent.DBDeleteWorker;
import org.citydb.citygml.deleter.util.BundledDBConnection;
import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.SQLScriptRunner;
import org.citydb.registry.ObjectRegistry;

public abstract class AbstractADEDBSchemaManager implements ADEDBSchemaManager {
	protected final Logger LOG = Logger.getInstance();
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	protected final Connection connection;
	protected final ConfigImpl config;
	protected String schema;	
	
	public AbstractADEDBSchemaManager(Connection connection, ConfigImpl config) {
		this.connection = connection;
		this.config = config;
		this.schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
	}
	
	public void createADEDatabaseSchema() throws SQLException {		
		try {	
			String createDBscriptString = readCreateADEDBScript();
			SQLScriptRunner.getInstance().runScript(processScript(createDBscriptString), connection);
		} catch (SQLException | IOException e) {
			throw new SQLException("Error occurred while reading and running ADE database creation script", e);
		}
	}
	
	public void dropADEDatabaseSchema(String adeId) throws SQLException {
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);
		try {
			String dropDBScriptString = adeMetadataManager.getDropDBScript(adeId);
			SQLScriptRunner.getInstance().runScript(dropDBScriptString, connection);
			dropCurrentFunctions();
		} catch (SQLException e) {		
			throw new SQLException("Error occurred while dropping the current delete functions", e);
		} 
	}
	
	public void cleanupADEData(String adeId) throws SQLException {
		ADEMetadataManager adeMetadataManager = new ADEMetadataManager(connection, config);
		SchemaMapping schemaMapping = adeMetadataManager.getMergedSchemaMapping();
		Statement stmt = null;
		ResultSet rs = null;
		List<DBSplittingResult> deleteWorks = new ArrayList<>();
		try {				
			stmt = connection.createStatement();
			rs = stmt.executeQuery(
						"select co.id, co.objectclass_id "
						+ "FROM " + schema + ".cityobject co, " + schema + ".objectclass oc, " + schema + ".ade a "
						+ "WHERE co.objectclass_id = oc.id "
						+ "AND a.id = oc.ade_id "
						+ "AND a.adeid = '" + adeId + "'");			
			while (rs.next()) {
				int objId = rs.getInt(1);
				int objectclassId = rs.getInt(2);
				AbstractObjectType<?> objectType = schemaMapping.getAbstractObjectType(objectclassId);
				deleteWorks.add(new DBSplittingResult(objId, objectType));		
			}
		} finally {
			if (stmt != null) 
				stmt.close();			
		}	
		
		int totalWorkNumber = deleteWorks.size();
		DBDeleteWorker deleteWorker = null;
		BundledDBConnection transaction = new BundledDBConnection(true);
		final ReentrantLock lock = new ReentrantLock();		
		try {			
			
			deleteWorker = new DBDeleteWorker(ObjectRegistry.getInstance().getEventDispatcher(), transaction);
			for (DBSplittingResult work: deleteWorks) {
				deleteWorker.doWork(work);	
				LOG.info("Remaining number of ADE objects going to be deleted: " + (--totalWorkNumber));
			}							
		} finally {
			if (deleteWorker != null)
				deleteWorker.shutdown();		
			transaction.close();
			lock.lock();			
		}
	}

	protected abstract String readCreateADEDBScript() throws IOException;
	protected abstract String processScript(String inputScript) throws SQLException;
	protected abstract void dropCurrentFunctions() throws SQLException;
	
}
