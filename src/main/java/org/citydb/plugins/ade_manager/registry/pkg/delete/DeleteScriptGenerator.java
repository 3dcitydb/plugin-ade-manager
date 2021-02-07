/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2020
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.plugins.ade_manager.registry.pkg.delete;

import java.sql.Connection;
import java.sql.SQLException;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.DefaultDBScriptGenerator;

public abstract class DeleteScriptGenerator extends DefaultDBScriptGenerator {
	protected final String lineage_delete_funcname = "del_cityobjects_by_lineage";
	protected final String appearance_cleanup_funcname = "cleanup_appearances";
	protected final String schema_cleanup_funcname = "cleanup_schema";
	protected final String table_cleanup_funcname = "cleanup_table";
	
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
		return singleDeleteFuncname + "";
	}
	
	protected abstract String getArrayDeleteFunctionDeclareField(String arrayDeleteFuncName, String schemaName); 
	protected abstract String getSingleDeleteFunctionDeclareField(String singleDeleteFuncName, String schemaName); 
	protected abstract DBSQLScript buildDeleteScript() throws SQLException; 	
	protected abstract void constructArrayDeleteFunction(DeleteFunction arrayDeleteFunction) throws SQLException;
	protected abstract void constructSingleDeleteFunction(DeleteFunction singleDeleteFunction, String arrayDeleteFuncname);
	protected abstract void constructLineageDeleteFunction(DeleteFunction singleDeleteFunction);
	protected abstract void constructAppearanceCleanupFunction(DeleteFunction cleanupFunction);
	protected abstract void constructSchemaCleanupFunction(DeleteFunction cleanupFunction);
	protected abstract void constructTableCleanupFunction(DeleteFunction cleanupFunction);

	protected void registerDeleteFunction(String tableName, String schemaName) throws SQLException {
		// create array-delete function
		String arrayDeleteFuncName = getArrayDeleteFunctionName(tableName);
		String arrayDeleteDeclareField = getArrayDeleteFunctionDeclareField(arrayDeleteFuncName, schemaName);
		if (!functionCollection.containsKey(arrayDeleteDeclareField)) {	
			DeleteFunction deleteFunction = new DeleteFunction(tableName, arrayDeleteFuncName, arrayDeleteDeclareField, schemaName);
			functionCollection.put(arrayDeleteDeclareField, deleteFunction); 
			constructArrayDeleteFunction(deleteFunction);
			log.info("Delete function '" + arrayDeleteFuncName + "' created." );
		}	
		
		// create single-delete function
		String singleDeleteFuncName = getSingleDeleteFunctionName(tableName);
		String singleDeleteDeclareField = getSingleDeleteFunctionDeclareField(singleDeleteFuncName, schemaName);
		if (!functionCollection.containsKey(singleDeleteDeclareField)) {	
			DeleteFunction singleDeleteFunction = new DeleteFunction(tableName, singleDeleteFuncName, singleDeleteDeclareField, schemaName);
			functionCollection.put(singleDeleteDeclareField, singleDeleteFunction); 
			constructSingleDeleteFunction(singleDeleteFunction, arrayDeleteFuncName);
			log.info("Delete function '" + singleDeleteFuncName + "' created." );
		}	
	}
	
	private void registerExtraFunctions(String schemaName) {
		// Lineage delete function
		DeleteFunction lineageDeleteFunction = new DeleteFunction(lineage_delete_funcname, schemaName);
		constructLineageDeleteFunction(lineageDeleteFunction);
		functionCollection.put(lineageDeleteFunction.getDeclareField(), lineageDeleteFunction);
		log.info("Delete function '" + lineage_delete_funcname + "' created." );

		// Appearance cleanup function
		DeleteFunction cleanupAppearancesFunction = new DeleteFunction(appearance_cleanup_funcname, schemaName);
		constructAppearanceCleanupFunction(cleanupAppearancesFunction);
		functionCollection.put(cleanupAppearancesFunction.getDeclareField(), cleanupAppearancesFunction);
		log.info("Cleanup-function '" + appearance_cleanup_funcname + "' created." );
		
		// Schema cleanup function
		DeleteFunction cleanupSchemaFunction = new DeleteFunction(schema_cleanup_funcname, schemaName);
		constructSchemaCleanupFunction(cleanupSchemaFunction);
		functionCollection.put(cleanupSchemaFunction.getDeclareField(), cleanupSchemaFunction);
		log.info("Cleanup-function '" + schema_cleanup_funcname + "' created." );
		
		// table cleanup function
		DeleteFunction cleanupTableFunction = new DeleteFunction(table_cleanup_funcname, schemaName);
		constructTableCleanupFunction(cleanupTableFunction);
		functionCollection.put(cleanupTableFunction.getDeclareField(), cleanupTableFunction);
		log.info("Cleanup-function '" + table_cleanup_funcname + "' created." );
	}
}
