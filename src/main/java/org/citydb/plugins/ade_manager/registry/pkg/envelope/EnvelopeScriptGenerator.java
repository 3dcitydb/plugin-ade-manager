package org.citydb.plugins.ade_manager.registry.pkg.envelope;

import java.sql.Connection;
import java.sql.SQLException;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.DefaultDBScriptGenerator;

public abstract class EnvelopeScriptGenerator extends DefaultDBScriptGenerator {
	public EnvelopeScriptGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}
	
	@Override
	public void registerFunctions(String schemaName) throws SQLException {					
		registerEnvelopeFunction("cityobject", schemaName);	
	}
	
	@Override
	protected String createFunctionName(String tableName) {
		return "env_" + tableName;
	}

	protected abstract void constructEnvelopeFunction(EnvelopeFunction deleteFunction) throws SQLException;

	protected void registerEnvelopeFunction(String tableName, String schemaName) throws SQLException {
		String funcName = createFunctionName(tableName);
		if (!functionCollection.containsKey(funcName)) {	
			EnvelopeFunction envelopeFunction = new EnvelopeFunction(tableName, funcName, schemaName);
			functionCollection.put(funcName, envelopeFunction); 
			constructEnvelopeFunction(envelopeFunction);
			LOG.info("Envelope-function '" + funcName + "' created." );
		}			
	}
	
}
