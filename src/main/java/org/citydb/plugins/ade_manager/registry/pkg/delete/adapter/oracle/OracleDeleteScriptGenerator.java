package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.AbstractDeleteScriptGenerator;

public class OracleDeleteScriptGenerator extends AbstractDeleteScriptGenerator {
	private final String scriptSeparator = "---";
	
	public OracleDeleteScriptGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	public void installDeleteScript(String scriptString) throws SQLException{
		String pkgHeader = scriptString.split(scriptSeparator)[0];
		String pkgBody = scriptString.split(scriptSeparator)[1];
		Statement headerStmt = null;
		Statement bodyStmt = null;
		try {
			headerStmt = connection.createStatement();
			headerStmt.execute(pkgHeader);
			bodyStmt = connection.createStatement();
			bodyStmt.execute(pkgBody);
		} catch (SQLException e) {
			throw new SQLException(e);
		} finally {
			if (headerStmt != null)
				headerStmt.close();
			if (bodyStmt != null)
				bodyStmt.close();
		}
	}
	
	@Override
	protected String constructDeleteFunction(String tableName, String schemaName) throws SQLException {
		String delete_func_ddl =
				"CREATE OR REPLACE FUNCTION " + schemaName + "." + createFunctionName(tableName) + 
				"((pids ID_ARRAY, caller int := 0) RETURN ID_ARRAY " + br + "IS";
		
		String declare_block = 
							brDent1 + "deleted_ids ID_ARRAY := ID_ARRAY();"+
							brDent1 + "object_id number;" +
							brDent1 + "objectclass_id number;";
		
		String pre_block = "";
		String post_block = "";
		String delete_block = "";	
		
		String delete_into_block = 
				brDent1 + "INTO"  
				  + brDent2 +  "deleted_id";
		
		String return_block = 
				brDent1 + "RETURN deleted_id;";
		
		// TODO...
				
		// Putting all together
		delete_func_ddl += 
				declare_block + 
				br + "BEGIN" + 
				pre_block + 
				brDent1 + "-- delete " + schemaName + "." + tableName.toLowerCase() + "s" + 
				delete_block + 
				delete_into_block + ";" +
				br +
				post_block +  
				br + 
				return_block +
				"END;";	

		return delete_func_ddl;
	}

	@Override
	protected String printScript() {
		// Test...
		String ddl = 
			// package header	
			"CREATE OR REPLACE PACKAGE citydb_delete" + br +
			"AS" + br +
			"  FUNCTION delete_test1(a number) RETURN NUMBER;" + br +
			"  FUNCTION delete_test2(a number) RETURN NUMBER;" + br +			
			"END citydb_delete; " + br +
			scriptSeparator + br +
			// package body	
			"CREATE OR REPLACE PACKAGE BODY citydb_delete" + br +
			"AS " + br +
			"  FUNCTION delete_test1(a number) RETURN NUMBER" + br +
			"  IS " + br +
			"  BEGIN" +  br +
			"    RETURN a;" + br +
			"  END; " + br +
			"  FUNCTION delete_test2(a number) RETURN NUMBER" + br +
			"  IS " + br +
			"  BEGIN" +  br +
			"    RETURN a;" + br +
			"  END; " + br +			
			"END citydb_delete;";
		return ddl;
	}

}
