package org.citydb.plugins.ade_manager.registry.pkg.delete;

import java.sql.Connection;
import java.sql.SQLException;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.DefaultDBScriptGenerator;

public abstract class DeleteScriptGenerator extends DefaultDBScriptGenerator {
	protected final String lineage_delete_funcname = "del_cityobject_by_lineage";
	protected final String appearance_cleanup_funcname = "cleanup_global_appearances";
	protected final String schema_cleanup_funcname = "cleanup_schema";
	
	public DeleteScriptGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}
	
	@Override
	public void registerFunctions(String schemaName) throws SQLException {					
		registerDeleteFunction("cityobject", schemaName);	
		registerExtraFunctions(schemaName);
	}
	
	@Override
	protected String createFunctionName(String tableName) {
		return "del_" + tableName;
	}

	protected abstract void constructDeleteFunction(DeleteFunction deleteFunction) throws SQLException;
	protected abstract void constructLineageDeleteFunction(DeleteFunction deleteFunction);
	protected abstract void constructAppearanceCleanupFunction(DeleteFunction cleanupFunction);
	protected abstract void constructSchemaCleanupFunction(DeleteFunction cleanupFunction);

	protected void registerDeleteFunction(String tableName, String schemaName) throws SQLException {
		String funcName = createFunctionName(tableName);
		if (!functionCollection.containsKey(funcName)) {	
			DeleteFunction deleteFunction = new DeleteFunction(tableName, funcName, schemaName);
			functionCollection.put(funcName, deleteFunction); 
			constructDeleteFunction(deleteFunction);
			LOG.info("Delete-function '" + funcName + "' created." );
		}			
	}
	
	private void registerExtraFunctions(String schemaName) {
		// Lineage delete function
		DeleteFunction lineageDeleteFunction = new DeleteFunction(lineage_delete_funcname, schemaName);
		constructLineageDeleteFunction(lineageDeleteFunction);
		functionCollection.put(lineage_delete_funcname, lineageDeleteFunction);
		LOG.info("Delete-function '" + lineage_delete_funcname + "' created." );
		
		// Appearance cleanup function
		DeleteFunction cleanupAppearancesFunction = new DeleteFunction(appearance_cleanup_funcname, schemaName);
		constructAppearanceCleanupFunction(cleanupAppearancesFunction);
		functionCollection.put(appearance_cleanup_funcname, cleanupAppearancesFunction);
		LOG.info("Cleanup-function '" + appearance_cleanup_funcname + "' created." );
		
		// Appearance cleanup function
		DeleteFunction cleanupSchemaFunction = new DeleteFunction(schema_cleanup_funcname, schemaName);
		constructSchemaCleanupFunction(cleanupSchemaFunction);
		functionCollection.put(schema_cleanup_funcname, cleanupSchemaFunction);
		LOG.info("Cleanup-function '" + schema_cleanup_funcname + "' created." );
	}
}
