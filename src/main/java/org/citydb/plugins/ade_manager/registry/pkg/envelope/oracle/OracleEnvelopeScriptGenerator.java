package org.citydb.plugins.ade_manager.registry.pkg.envelope.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeScriptGenerator;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeFunction;

public class OracleEnvelopeScriptGenerator extends EnvelopeScriptGenerator {

	public OracleEnvelopeScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
	}


	@Override
	protected void constructEnvelopeFunction(EnvelopeFunction deleteFunction) throws SQLException {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected DBSQLScript buildEnvelopeScript() throws SQLException {
		return new DBSQLScript();
	}

}
