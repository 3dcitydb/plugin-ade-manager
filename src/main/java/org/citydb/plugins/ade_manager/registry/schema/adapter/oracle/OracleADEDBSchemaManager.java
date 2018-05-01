package org.citydb.plugins.ade_manager.registry.schema.adapter.oracle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;
import org.citydb.plugins.ade_manager.util.PathResolver;

import oracle.jdbc.OracleTypes;

public class OracleADEDBSchemaManager extends AbstractADEDBSchemaManager {

	public OracleADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	public void cleanupADEData(String adeId) throws SQLException {
		Map<Integer, String> cityobjectIds = queryADECityobjectIds(adeId);
		StringBuilder deleteStmt = new StringBuilder().append("{? = call citydb_delete.del_cityobject(ID_ARRAY(?))}");
		int sum = cityobjectIds.size();
		for (Integer objectId: cityobjectIds.keySet()) {			
			CallableStatement deleteCall = connection.prepareCall(deleteStmt.toString());
			try {
				String className = cityobjectIds.get(objectId);
				LOG.debug("Deleting " + className + "(ID = " + objectId + ")");
				deleteCall.registerOutParameter(1, OracleTypes.ARRAY, "ID_ARRAY");
				deleteCall.setInt(2, objectId);
				deleteCall.executeUpdate();				
				LOG.info(className + "(ID = " + objectId + ")" + " deleted.");
				LOG.info("Number of remaining ADE objects to be deleted: " + --sum);
			} finally {
				if (deleteCall != null)
					deleteCall.close();
			}
		}		
	}

	@Override
	protected String readCreateADEDBScript() throws IOException {
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, DatabaseType.ORACLE);	
		return new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
	}

	@Override
	protected String processScript(String inputScript) throws SQLException {
		String result = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream writer = new PrintStream(os); 
		try {
			boolean skip = false;
			Scanner scanner = new Scanner(inputScript);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine(); 
				if (line.indexOf("SET SERVEROUTPUT ON") >= 0) {
					skip = true;
				}
				if (skip != true) {
					writer.println(line);
				}																
				if (line.indexOf("prompt Used SRID for spatial indexes") >= 0) {
					skip = false;
				}				
			}
			scanner.close();
			int srid = dbPool.getActiveDatabaseAdapter().getUtil().getDatabaseInfo().getReferenceSystem().getSrid();
			result = os.toString().replace("&SRSNO", String.valueOf(srid));		
		} catch (SQLException e) {
			throw new SQLException("Failed to get SRID from the database", e);
		}	
		
		return result;
	}

	@Override
	protected void dropCurrentDeleteFunctions() throws SQLException {
		
	}

}
