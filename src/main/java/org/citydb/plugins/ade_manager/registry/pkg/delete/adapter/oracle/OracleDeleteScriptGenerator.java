package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.oracle;

import java.sql.Connection;
import java.sql.SQLException;

import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.AbstractDeleteScriptGenerator;

public class OracleDeleteScriptGenerator extends AbstractDeleteScriptGenerator {

	public OracleDeleteScriptGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	protected void generateDeleteScript(String initTableName, String schemaName) {
		// TODO
	}

	@Override
	protected String constructDeleteFunction(String tableName, String schemaName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
