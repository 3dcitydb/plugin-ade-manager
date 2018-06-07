package org.citydb.plugins.ade_manager.registry.schema.adapter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;

import org.citydb.config.ConfigNamespaceFilter;
import org.citydb.config.project.exporter.SimpleQuery;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.delete.DBDeleteController;
import org.citydb.plugins.ade_manager.delete.DBDeleteException;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.SQLScriptRunner;
import org.citydb.query.Query;
import org.citydb.query.builder.QueryBuildException;
import org.citydb.query.builder.config.ConfigQueryBuilder;
import org.citydb.query.filter.projection.ProjectionFilter;
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
			dropCurrentDeleteFunctions();
		} catch (SQLException e) {		
			throw new SQLException("Error occurred while dropping the current delete functions", e);
		} 
	}
	
	public void cleanupADEData(String adeId) throws SQLException {
		SchemaMapping schemaMapping = ObjectRegistry.getInstance().getSchemaMapping();
		AbstractDatabaseAdapter databaseAdapter = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter();
		ConfigQueryBuilder queryBuilder = new ConfigQueryBuilder(schemaMapping, databaseAdapter);
		Query query = null;
		try {			
			query = queryBuilder.buildQuery(new SimpleQuery(), new ConfigNamespaceFilter());
		} catch (QueryBuildException e) {
			throw new SQLException("Failed to build the query expression for cleaning up ADE data.", e);
		}		

		Statement stmt = null;
		ResultSet rs = null;			
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select o.id from " + schema + ".objectclass o, " + schema + ".ade a" 
					+ " where o.ade_id = a.id and a.adeid = '" + adeId + "'");			
			while (rs.next()) {
				int objectclassId = rs.getInt(1);	
				AbstractObjectType<?> objectType = schemaMapping.getAbstractObjectType(objectclassId);
				if (objectType != null) {
					ProjectionFilter projectionFilter = new ProjectionFilter(objectType);
					query.addProjectionFilter(projectionFilter);
				}			
			}						
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}	
		
		final ReentrantLock lock = new ReentrantLock();;
		lock.lock();		
		try {
			DBDeleteController deleter = new DBDeleteController(query);
			deleter.doProcess();
			deleter.cleanup();
		} catch (DBDeleteException e) {
			throw new SQLException("Error occurred: ", e);
		} finally {
			lock.unlock();
		}
	}

	protected abstract String readCreateADEDBScript() throws IOException;
	protected abstract String processScript(String inputScript) throws SQLException;
	protected abstract void dropCurrentDeleteFunctions() throws SQLException;
	
}
