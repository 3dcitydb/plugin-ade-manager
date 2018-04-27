package org.citydb.plugins.ade_manager.registry.schema.adapter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.SQLScriptRunner;

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
	
	protected Map<Integer, String> queryADECityobjectIds(String adeId) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		Map<Integer, String> objectclassIds = new java.util.HashMap<Integer, String>();	
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select " + schema + ".cityobject.id, " + schema + ".objectclass.classname "
					+ "FROM " + schema + ".cityobject, " + schema + ".objectclass, "+ schema + ".ade "
					+ "WHERE " + schema + ".cityobject.objectclass_id = " + schema + ".objectclass.id "
					+ "AND " + schema + ".ade.id = " + schema + ".objectclass.ade_id "
					+ "AND " + schema + ".ade.adeid = '" + adeId + "'");
			
			while (rs.next()) {
				int adeid = rs.getInt(1);
				String className = rs.getString(2);
				objectclassIds.put(adeid, className);				
			}
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}
	
		return objectclassIds;		
	}

	protected abstract String readCreateADEDBScript() throws IOException;
	protected abstract String processScript(String inputScript) throws SQLException;
	protected abstract void dropCurrentDeleteFunctions() throws SQLException;
	
}
