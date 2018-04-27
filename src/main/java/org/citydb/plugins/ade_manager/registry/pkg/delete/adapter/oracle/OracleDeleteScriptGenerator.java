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
	//	List<String> test1 = querier.query_selfref_fk("building", schemaName);
	//	List<MnRefEntry> test2 = querier.query_ref_fk("building", schemaName);
	//	String test3 = querier.query_ref_to_parent_fk("building", schemaName);
	//	List<ReferencedEntry> test4 = querier.query_ref_to_fk("building", schemaName);
		return null;
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
