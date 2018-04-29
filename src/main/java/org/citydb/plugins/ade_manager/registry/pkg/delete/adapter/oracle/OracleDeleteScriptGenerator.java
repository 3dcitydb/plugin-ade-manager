package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.oracle;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.AbstractDeleteScriptGenerator;
import org.citydb.util.CoreConstants;

public class OracleDeleteScriptGenerator extends AbstractDeleteScriptGenerator {
	
	public OracleDeleteScriptGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	public void installDeleteScript(String scriptString) throws SQLException{
		String pkgHeader = scriptString.split(CoreConstants.DEFAULT_DELIMITER)[0];
		String pkgBody = scriptString.split(CoreConstants.DEFAULT_DELIMITER)[1];
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
				dent + "CREATE OR REPLACE FUNCTION " + createFunctionName(tableName) + "(pids ID_ARRAY, caller int := 0) RETURN ID_ARRAY " + 
				brDent1 + "IS";
		
		String declare_block = 
					brDent2 + "deleted_ids ID_ARRAY := ID_ARRAY();"+
					brDent2 + "object_id number;" +
					brDent2 + "objectclass_id number;";
		
		String pre_block = "";
		String post_block = "";
		String delete_block = "";	
		
		String delete_into_block = 
					brDent2 + "INTO"  
					  + brDent3 +  "deleted_id";
		
		String return_block = 
					brDent2 + "RETURN deleted_id;";
		
		// TODO...
				
		// Putting all together
		delete_func_ddl += 
					declare_block + 
					brDent1 + "BEGIN" + 
					pre_block + 
					brDent2 + "-- delete " + schemaName + "." + tableName.toLowerCase() + "s" + 
					delete_block + 
					delete_into_block + ";" +
					br +
					post_block +  
					br + 
					return_block +
					brDent1 + 
					"END;";	

		return delete_func_ddl;
	}

	@Override
	protected void printDDLForAllDeleteFunctions(PrintStream writer) {
		// package header
		String script = 					
				"CREATE OR REPLACE PACKAGE citydb_delete" + br +
				"AS";
		
		for (String tableName: functionCollection.keySet()) {
			script += brDent1 + "FUNCTION " + functionNames.get(tableName) + "(pids ID_ARRAY, caller int := 0) RETURN ID_ARRAY" + br;
		};
		script += "END citydb_delete;" + br
				+ CoreConstants.DEFAULT_DELIMITER + br;
		
		// package body	
		script += "CREATE OR REPLACE PACKAGE BODY citydb_delete" + br +
				  "AS " + br;
		
		for (String tableName: functionCollection.keySet()) {
			String functionBody = functionCollection.get(tableName);
			script += functionBody + 
					brDent1 + "------------------------------------------" + br;
		};	
		script += "END citydb_delete;";
		
		writer.println(script);
	}

}
