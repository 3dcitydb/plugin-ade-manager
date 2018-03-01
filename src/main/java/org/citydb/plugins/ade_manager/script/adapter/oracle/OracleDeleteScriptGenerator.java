package org.citydb.plugins.ade_manager.script.adapter.oracle;

import java.sql.SQLException;

import org.citydb.plugins.ade_manager.script.adapter.AbstractDeleteScriptGenerator;

public class OracleDeleteScriptGenerator extends AbstractDeleteScriptGenerator {

	@Override
	protected void generateDeleteFuncs(String initTableName, String schemaName) {
		StringBuilder scriptBuilder = new StringBuilder(); 
		scriptBuilder.append(buildComment("Oracle Version"));
	}

	@Override
	protected String create_delete_function(String tableName, String schemaName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
