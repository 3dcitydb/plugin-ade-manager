package org.citydb.plugins.ade_manager.registry.pkg.envelope.postgis;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeFunction;

public class PostgisEnvelopeGeneratorGenerator extends EnvelopeScriptGenerator {

	public PostgisEnvelopeGeneratorGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}
	
	@Override
	public void installScript(String scriptString) throws SQLException {
		// TODO
	}

	@Override
	protected void constructEnvelopeFunction(EnvelopeFunction deleteFunction) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void printFunctionsDDL(PrintStream writer) {
		// TODO Auto-generated method stub
		
	}

}
