package org.citydb.plugins.ade_manager.registry.schema.adapter.oracle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;
import org.citydb.plugins.ade_manager.util.PathResolver;

public class OracleADEDBSchemaManager extends AbstractADEDBSchemaManager {

	public OracleADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
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
