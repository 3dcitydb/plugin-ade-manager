package org.citydb.plugins.ade_manager.registry.pkg.delete;

import java.sql.Connection;
import java.sql.SQLException;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.DefaultDBScriptGenerator;

public abstract class DeleteScriptGenerator extends DefaultDBScriptGenerator {
	protected final String lineage_delete_funcname = "del_cityobjects_by_lineage";
	protected final String appearance_cleanup_funcname = "cleanup_global_appearances";
	protected final String schema_cleanup_funcname = "cleanup_schema";
	
	public DeleteScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
	}
	
	@Override
	protected DBSQLScript generateScript(String schemaName) throws SQLException {
		registerDeleteFunction("cityobject", schemaName);	
		registerExtraFunctions(schemaName);
		
		return buildDeleteScript();		
	}
	
	protected String getArrayDeleteFunctionName(String tableName) {
		return convertSingleToArrayDeleteFunctionName(getSingleDeleteFunctionName(tableName));
	}
	
	protected String getSingleDeleteFunctionName(String tableName) {
		return "del_" + tableName;
	}
	
	protected String convertSingleToArrayDeleteFunctionName(String singleDeleteFuncname) {
		return singleDeleteFuncname + "s";
	}
	
	protected abstract DBSQLScript buildDeleteScript() throws SQLException; 	
	protected abstract void constructArrayDeleteFunction(DeleteFunction arrayDeleteFunction) throws SQLException;
	protected abstract void constructSingleDeleteFunction(DeleteFunction singleDeleteFunction, String arrayDeleteFuncname);
	protected abstract void constructLineageDeleteFunction(DeleteFunction singleDeleteFunction);
	protected abstract void constructAppearanceCleanupFunction(DeleteFunction cleanupFunction);
	protected abstract void constructSchemaCleanupFunction(DeleteFunction cleanupFunction);

	protected void registerDeleteFunction(String tableName, String schemaName) throws SQLException {
		// create array-delete function
		String arrayDeleteFuncName = getArrayDeleteFunctionName(tableName);
		if (!functionCollection.containsKey(arrayDeleteFuncName)) {	
			DeleteFunction deleteFunction = new DeleteFunction(tableName, arrayDeleteFuncName, schemaName);
			functionCollection.put(arrayDeleteFuncName, deleteFunction); 
			constructArrayDeleteFunction(deleteFunction);
			LOG.info("Delete-function '" + arrayDeleteFuncName + "' created." );
		}	
		
		// create single-delete function
		String singleDeleteFuncName = getSingleDeleteFunctionName(tableName);
		if (!functionCollection.containsKey(singleDeleteFuncName)) {	
			DeleteFunction singleDeleteFunction = new DeleteFunction(tableName, singleDeleteFuncName, schemaName);
			functionCollection.put(singleDeleteFuncName, singleDeleteFunction); 
			constructSingleDeleteFunction(singleDeleteFunction, arrayDeleteFuncName);
			LOG.info("Delete-function '" + singleDeleteFuncName + "' created." );
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
