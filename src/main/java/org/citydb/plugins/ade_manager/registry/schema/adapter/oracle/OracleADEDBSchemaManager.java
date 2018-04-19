package org.citydb.plugins.ade_manager.registry.schema.adapter.oracle;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencingEntry;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;

public class OracleADEDBSchemaManager extends AbstractADEDBSchemaManager {

	public OracleADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	public List<String> query_selfref_fk(String tableName, String schemaName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MnRefEntry> query_ref_fk(String tableName, String schemaName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ReferencingEntry> query_ref_tables_and_columns(String tableName, String schemaName)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String query_ref_to_parent_fk(String tableName, String schemaName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ReferencedEntry> query_ref_to_fk(String tableName, String schemaName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
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

}