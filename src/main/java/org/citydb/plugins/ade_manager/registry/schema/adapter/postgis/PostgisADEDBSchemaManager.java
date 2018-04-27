package org.citydb.plugins.ade_manager.registry.schema.adapter.postgis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;
import org.citydb.plugins.ade_manager.util.PathResolver;

public class PostgisADEDBSchemaManager extends AbstractADEDBSchemaManager {

	public PostgisADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	public void cleanupADEData(String adeId) throws SQLException {
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		PreparedStatement ps = null;
		Map<Integer, String>cityobjectIds = queryADECityobjectIds(adeId);
		int batchSize = 100;
		try {
			int sum = cityobjectIds.size();
			String call = "select " + schema + ".del_cityobject(?);";
			ps = connection.prepareStatement(call);	
			int counter = 0;
			Object[] inputArray = new Object[batchSize];
			for (Integer objectId: cityobjectIds.keySet()) {
				inputArray[counter] = objectId;
				counter++;				
				if (counter == batchSize) {
					ps.setArray(1, connection.createArrayOf("INTEGER", inputArray));
					ps.executeQuery();
					counter = 0;
					inputArray = new Object[batchSize];
				}					
				String className = cityobjectIds.get(objectId);
				LOG.info(className + "(ID = " + objectId + ")" + " deleted.");
				LOG.info("Number of remaining ADE objects to be deleted: " + --sum);
			}
			if (counter > 0) {
				ps.setArray(1, connection.createArrayOf("INTEGER", inputArray));
				ps.executeQuery();
			}								
		} finally {
			if (ps != null)
				ps.close();
		}
	}

	@Override
	protected String readCreateADEDBScript() throws IOException {
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, DatabaseType.POSTGIS);	
		
		return new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
	}
	
	@Override
	protected String processScript(String inputScript) {
		return inputScript;
	}

	@Override
	protected void dropCurrentDeleteFunctions() throws SQLException {
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		Map<String, String> deleteFunctions = queryDeleteFunctions(schema);
		for (String funcName: deleteFunctions.keySet()) {
			String funcDeclaration = deleteFunctions.get(funcName);
			PreparedStatement pstsmt = null;
			try {
				pstsmt = connection.prepareStatement("DROP FUNCTION IF EXISTS " + schema + "." + funcDeclaration);
				pstsmt.executeUpdate();		
				LOG.debug("DB-function '" + funcName + "' successfully dropped");
			} 
			finally {			
				if (pstsmt != null) { 
					try {
						pstsmt.close();
					} catch (SQLException e) {
						throw e;
					} 
				}	
			}
		}		
	}
	
	private Map<String, String> queryDeleteFunctions(String schema) throws SQLException{
		Map<String, String> funcNames = new HashMap<String, String>();
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
				
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT routines.routine_name, routines.data_type, parameters.data_type, parameters.ordinal_position ")
				      .append("FROM information_schema.routines ")
				      .append("LEFT JOIN information_schema.parameters ON routines.specific_name=parameters.specific_name ")
				      .append("WHERE routines.specific_schema= '").append(schema).append("' and routines.routine_name like 'del_%' ")
				      .append("ORDER BY routines.routine_name, parameters.ordinal_position");
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();						
			while (rs.next()) {
				String funcName = rs.getString(1);
				String instinctParamType = rs.getString(2);
				String paramType = rs.getString(3);
				if (paramType.equalsIgnoreCase("array"))
					paramType = instinctParamType + "[]";
				
				String funcDeclaration = null;
				if (!funcNames.containsKey(funcName)) {
					funcDeclaration = funcName + "(" + paramType;
				} else {
					funcDeclaration = funcNames.get(funcName) + "," + paramType;					
				}
				funcNames.put(funcName, funcDeclaration);	
			}	
			for (String key: funcNames.keySet()) {
				funcNames.put(key, funcNames.get(key) + ")");
			}
		} 
		finally {			
			if (rs != null) { 
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}
			}	
			if (pstsmt != null) { 
				try {
					pstsmt.close();
				} catch (SQLException e) {
					throw e;
				} 
			}	
		}
		
		return funcNames;
	}

}
