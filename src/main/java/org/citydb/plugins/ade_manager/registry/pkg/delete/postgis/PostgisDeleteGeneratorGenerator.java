/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
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
package org.citydb.plugins.ade_manager.registry.pkg.delete.postgis;

import org.citydb.core.database.connection.DatabaseConnectionPool;
import org.citydb.core.database.schema.mapping.RelationType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteFunction;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.query.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencingEntry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PostgisDeleteGeneratorGenerator extends DeleteScriptGenerator {
	private final String idType;

	public PostgisDeleteGeneratorGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
		idType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 2, 0) < 0 ?
				"integer" :
				"bigint";
	}
	
	@Override
	protected DBSQLScript buildDeleteScript() throws SQLException {
		DBSQLScript dbScript = new DBSQLScript();
		dbScript.addSQLBlock(functionCollection.printFunctionDefinitions(separatorLine));		
		return dbScript;
	}

	@Override
	protected void constructArrayDeleteFunction(DeleteFunction deleteFunction) throws SQLException  {
		String tableName = deleteFunction.getTargetTable();
		String schemaName = deleteFunction.getOwnerSchema();		
		String declareField = deleteFunction.getDeclareField();
		
		String delete_func_ddl = "CREATE OR REPLACE " + declareField + " AS" + br + "$body$" + br;
		
		String declare_block = 
				"DECLARE" + 
				brDent1 + "deleted_ids " + idType + "[] := '{}';"+
				brDent1 + "dummy_id " + idType + ";" +
				brDent1 + "deleted_child_ids " + idType + "[] := '{}';"+
				brDent1 + "object_id " + idType + ";" +
				brDent1 + "objectclass_id integer;" +
				brDent1 + "rec RECORD;";
		
		String pre_block = "";
		String post_block = "";
		
		String delete_agg_start = 
				brDent1 + "WITH delete_objects AS (";
		
		String delete_block = "";
		
		String delete_agg_end = 
				brDent1 + ")" + 								
				brDent1 + "SELECT" + 
					brDent2 + "array_agg(id)";
		
		String return_block = 	
				brDent1 + "IF array_length(deleted_child_ids, 1) > 0 THEN" + 				
					brDent2 + "deleted_ids := deleted_child_ids;" +
				brDent1 + "END IF;" +
				br +
				brDent1 + "RETURN QUERY" +
					brDent2 + "SELECT unnest(deleted_ids);";
		
		// Code-block for deleting self-references in case of e.g. building/buildingParts
		// and Composite(Multi)Surface/SurfaceGeometry which have composition relations
		pre_block = this.create_selfref_delete(tableName, schemaName);
				
		// Code-block for deleting referenced sub-features with aggregation/composition or inheritance relationship
		String[] result = create_ref_delete(tableName, schemaName);
		declare_block += result[0];
		pre_block += result[1];
	
		// Main Delete for the current table
		delete_block += create_local_delete(tableName , schemaName);
		
		// Code-block for deleting referenced tables with 1:0..1 composition or N: 0..1 aggregation 
		// e.g. the composition relationship between building and surface geometry,
		// the aggregation relationship between features and their shared implicit geometry	
		String[] tmp = create_ref_to_delete(tableName, schemaName);
		String vars = tmp[0]; 
		String returning_block = tmp[1]; 
		String collect_block = tmp[2];
		String into_block = tmp[3];
		String fk_block = tmp[4]; 
		
		declare_block += vars;
		delete_block += returning_block;
		delete_agg_end += collect_block + 
				brDent1 + "INTO" + 
					brDent2 + "deleted_ids" + into_block + 
				brDent1 + "FROM" + 
					brDent2 + "delete_objects;" + br;
		post_block += fk_block;
		
		// Code-block for deleting the records in the parent table: e.g. deleting a record in the BUILDING table requires
		// the deletion of the corresponding record in the CITYOBJECT table.
		post_block += create_ref_to_parent_delete(tableName, schemaName);
		
		// Putting all together
		delete_func_ddl +=  
				declare_block + br + 
				"BEGIN" + pre_block +
				brDent1 + "-- delete " + wrapSchemaName(tableName, schemaName) + "s" + 
				delete_agg_start + 
				delete_block + 
				delete_agg_end + 
				post_block + 
				return_block + br + 
				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";	

		deleteFunction.setDefinition(delete_func_ddl);
	}

	@Override
	protected void constructSingleDeleteFunction(DeleteFunction singleDeleteFunction, String arrayDeleteFuncname) {
		String schemaName = singleDeleteFunction.getOwnerSchema();		
		String declareField = singleDeleteFunction.getDeclareField();
		
		String delete_func_ddl =
				"CREATE OR REPLACE " + declareField + " AS" + br + 
				"$body$" + br +
				"DECLARE" +
				brDent1 + "deleted_id " + idType + ";" + br +
				"BEGIN" + 
				brDent1 + "deleted_id := " + wrapSchemaName(arrayDeleteFuncname, schemaName) + "(ARRAY[pid]);" +
				brDent1 + "RETURN deleted_id;" + br +
				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";	
				
		singleDeleteFunction.setDefinition(delete_func_ddl);
	}

	@Override
	protected void constructLineageDeleteFunction(DeleteFunction deleteFunction) {
		String schemaName = deleteFunction.getOwnerSchema();
		String declareField = "FUNCTION " + wrapSchemaName(deleteFunction.getName(), schemaName) + 
				"(lineage_value TEXT, objectclass_id INTEGER DEFAULT 0) RETURNS SETOF " + idType + "";
		deleteFunction.setDeclareField(declareField);
		
		String delete_func_ddl = 
				"CREATE OR REPLACE " + declareField + " AS" + br + 
				"$body$" + br +
				commentPrefix + "Function for deleting cityobjects by lineage value" + br + 
				"DECLARE" + 
				brDent1 + "deleted_ids " + idType + "[] := '{}';" + br +
				"BEGIN" + 
				brDent1 + "IF $2 = 0 THEN" +	
					brDent2 + "SELECT array_agg(c.id) FROM" + 
						brDent3 +  wrapSchemaName("cityobject", schemaName) + " c" + 
					brDent2 + "INTO" + 
						brDent3 +  "deleted_ids" + 						
					brDent2 + "WHERE" + 
						brDent3 + "c.lineage = $1;" +
				brDent1 + "ELSE" + 
					brDent2 + "SELECT array_agg(c.id) FROM" + 
						brDent3 +  wrapSchemaName("cityobject", schemaName) + " c" + 
					brDent2 + "INTO" + 
						brDent3 +  "deleted_ids" + 						
					brDent2 + "WHERE" + 
						brDent3 + "c.lineage = $1 AND c.objectclass_id = $2;" +					
				brDent1 + "END IF;" + 
				br +
				brDent1 + "IF -1 = ALL(deleted_ids) IS NOT NULL THEN" + 				
					brDent2 +  "PERFORM " + wrapSchemaName(getArrayDeleteFunctionName("cityobject") + "(deleted_ids)", schemaName) + ";" +
				brDent1 + "END IF;" + 
				br + 
				brDent1 + "RETURN QUERY" +
					brDent2 + "SELECT unnest(deleted_ids);" + br + 
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";		
		
		deleteFunction.setDefinition(delete_func_ddl);
	}

	@Override
	protected void constructAppearanceCleanupFunction(DeleteFunction cleanupFunction) {
		String schemaName = cleanupFunction.getOwnerSchema();
		String declareField = "FUNCTION " + wrapSchemaName(cleanupFunction.getName(), schemaName) + 
				"(only_global INTEGER DEFAULT 1) RETURNS SETOF " + idType + "";
		cleanupFunction.setDeclareField(declareField);
		
		String cleanup_func_ddl = 
				"CREATE OR REPLACE " + declareField + " AS" + br + 
				"$body$" + br +
				"DECLARE" + 
				brDent1 + "deleted_id " + idType + ";" +
				brDent1 + "app_id " + idType + ";" + br +
				"BEGIN" + 
				brDent1 + "PERFORM " + wrapSchemaName(getArrayDeleteFunctionName("surface_data"), schemaName) + "(array_agg(s.id))" +	
					brDent2 + "FROM " + wrapSchemaName("surface_data", schemaName) + " s " + 
					brDent2 + "LEFT OUTER JOIN " + wrapSchemaName("textureparam", schemaName) + " t ON s.id = t.surface_data_id" + 
					brDent2 + "WHERE t.surface_data_id IS NULL;" + 
					br +
					brDent2 + "IF only_global=1 THEN" + 
						brDent3 + "FOR app_id IN" + 						
							brDent4 + "SELECT a.id FROM " + wrapSchemaName("appearance", schemaName) + " a" +
								brDent5 + "LEFT OUTER JOIN " + wrapSchemaName("appear_to_surface_data", schemaName) + " asd ON a.id=asd.appearance_id" +
									brDent6 + "WHERE a.cityobject_id IS NULL AND asd.appearance_id IS NULL" + 
						brDent3 + "LOOP" + 
							brDent4 +  "DELETE FROM " + wrapSchemaName("appearance", schemaName) + " WHERE id = app_id RETURNING id INTO deleted_id;" +
							brDent4 +  "RETURN NEXT deleted_id;" + 
						brDent3 + "END LOOP;" + 
					brDent2 + "ELSE" + 
						brDent3 + "FOR app_id IN" + 						
							brDent4 + "SELECT a.id FROM " + wrapSchemaName("appearance", schemaName) + " a" +
								brDent5 + "LEFT OUTER JOIN " + wrapSchemaName("appear_to_surface_data", schemaName) + " asd ON a.id=asd.appearance_id" +
									brDent6 + "WHERE asd.appearance_id IS NULL" + 
						brDent3 + "LOOP" + 
							brDent4 +  "DELETE FROM " + wrapSchemaName("appearance", schemaName) + " WHERE id = app_id RETURNING id INTO deleted_id;" +
							brDent4 +  "RETURN NEXT deleted_id;" + 
						brDent3 + "END LOOP;" + 
					brDent2 + "END IF;" + 	
					br +
				brDent1 + "RETURN;" + br +
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";		

		cleanupFunction.setDefinition(cleanup_func_ddl);
	}

	@Override
	protected void constructSchemaCleanupFunction(DeleteFunction cleanupFunction) {
		String schemaName = cleanupFunction.getOwnerSchema();
		String declareField = "FUNCTION " + wrapSchemaName(cleanupFunction.getName(), schemaName) + 
				"() RETURNS SETOF void";
		cleanupFunction.setDeclareField(declareField);
		
		String cleanup_func_ddl = 
				"CREATE OR REPLACE " + declareField + " AS" + br + 
				"$body$" + br +
				commentPrefix + "Function for cleaning up data schema" + br + 
				"DECLARE" + 
				brDent1 + "rec RECORD;" + 
				br +
				"BEGIN" + 
				brDent1 + "FOR rec IN"+	
					brDent2 + "SELECT table_name FROM information_schema.tables where table_schema = '" + schemaName + "'" + 
					brDent2 + "AND table_name <> 'database_srs'" + 
					brDent2 + "AND table_name <> 'objectclass'" + 
					brDent2 + "AND table_name <> 'index_table'" + 
					brDent2 + "AND table_name <> 'ade'" + 
					brDent2 + "AND table_name <> 'schema'" + 
					brDent2 + "AND table_name <> 'schema_to_objectclass'" + 
					brDent2 + "AND table_name <> 'schema_referencing'" + 
					brDent2 + "AND table_name <> 'aggregation_info'" + 
					brDent2 + "AND table_name NOT LIKE 'tmp_%'" + 
				brDent1 + "LOOP" + 						
					brDent2 + "EXECUTE format('TRUNCATE TABLE " + schemaName + ".%I CASCADE', rec.table_name);" + 
				brDent1 + "END LOOP;" + 
				br +				
				brDent1 + "FOR rec IN " +
					brDent2 + "SELECT sequence_name FROM information_schema.sequences where sequence_schema = '" + schemaName + "'" +  
					brDent2 + "AND sequence_name <> 'ade_seq'" + 	
					brDent2 + "AND sequence_name <> 'schema_seq'" + 	
				brDent1 + "LOOP" + 						
					brDent2 + "EXECUTE format('ALTER SEQUENCE " + schemaName + ".%I RESTART', rec.sequence_name);	" + 
				brDent1 + "END LOOP;" + 					
				br +
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql;";		

		cleanupFunction.setDefinition(cleanup_func_ddl);
	}

	@Override
	protected void constructTableCleanupFunction(DeleteFunction cleanupFunction) {
		String schemaName = cleanupFunction.getOwnerSchema();
		String declareField = "FUNCTION " + wrapSchemaName(cleanupFunction.getName(), schemaName) + 
				"(tab_name TEXT) RETURNS SETOF " + idType + "";
		cleanupFunction.setDeclareField(declareField);
		
		String cleanup_func_ddl = 
				"CREATE OR REPLACE " + declareField + " AS" + br + 
				"$body$" + br +
				"DECLARE" + 
				brDent1 + "rec RECORD;" + 
				brDent1 + "rec_id " + idType + ";" +
				brDent1 + "where_clause TEXT;" +
				brDent1 + "query_ddl TEXT;" +
				brDent1 + "counter " + idType + ";" +
				brDent1 + "table_alias TEXT;" +
				brDent1 + "table_name_with_schemaprefix TEXT;" +
				brDent1 + "del_func_name TEXT;" +
				brDent1 + "schema_name TEXT;" +
				brDent1 + "deleted_id " + idType + ";" +
				br +
				"BEGIN" + 
				brDent1 + "schema_name = '" + schemaName + "';" + 
				brDent1 + "IF md5(schema_name) <> '373663016e8a76eedd0e1ac37f392d2a' THEN" +
					brDent2 + "table_name_with_schemaprefix = schema_name || '.' || tab_name;" +
				brDent1 + "ELSE" +
					brDent2 + "table_name_with_schemaprefix = tab_name;" +
				brDent1 + "END IF;" +
				br +
				brDent1 + "counter = 0;" +
				brDent1 + "del_func_name = 'del_' || tab_name;" +
				brDent1 + "query_ddl = 'SELECT id FROM ' || schema_name || '.' || tab_name || ' WHERE id IN ('" +
					brDent2 + "|| 'SELECT a.id FROM ' || schema_name || '.' || tab_name || ' a';" +	
				br +
				brDent1 + "FOR rec IN" + 
					brDent2 + "SELECT" + 
						brDent3 + "c.confrelid::regclass::text AS root_table_name," + 
						brDent3 + "c.conrelid::regclass::text AS fk_table_name," + 
						brDent3 + "a.attname::text AS fk_column_name" + 
					brDent2 + "FROM" + 
						brDent3 + "pg_constraint c" + 
					brDent2 + "JOIN" + 
						brDent3 + "pg_attribute a" + 
						brDent3 + "ON a.attrelid = c.conrelid" + 
						brDent3 + "AND a.attnum = ANY (c.conkey)" + 
					brDent2 + "WHERE" + 
						brDent3 + "upper(c.confrelid::regclass::text) = upper(table_name_with_schemaprefix)" + 
						brDent3 + "AND c.conrelid <> c.confrelid" + 
						brDent3 + "AND c.contype = 'f'" + 						
					brDent2 + "ORDER BY" + 
						brDent3 + "fk_table_name," + 
						brDent3 + "fk_column_name" + 					
				brDent1 + "LOOP" + 						
					brDent2 + "counter = counter + 1;" + 
					brDent2 + "table_alias = 'n' || counter;" + 
					brDent2 + "IF counter = 1 THEN" + 
						brDent3 + "where_clause = ' WHERE ' || table_alias || '.' || rec.fk_column_name || ' IS NULL';" +
					brDent2 + "ELSE" + 
						brDent3 + "where_clause = where_clause || ' AND ' || table_alias || '.' || rec.fk_column_name || ' IS NULL';" +
					brDent2 + "END IF;" + 
					br + 
				brDent2 + "IF md5(schema_name) <> '373663016e8a76eedd0e1ac37f392d2a' THEN" +
					brDent3 + "query_ddl = query_ddl || ' LEFT JOIN ' || rec.fk_table_name || ' ' || table_alias || ' ON '" +
						brDent4 + "|| table_alias || '.' || rec.fk_column_name || ' = a.id';" + 
				brDent2 + "ELSE" +
					brDent3 + "query_ddl = query_ddl || ' LEFT JOIN ' || schema_name || '.' || rec.fk_table_name || ' ' || table_alias || ' ON '" +
						brDent4 + "|| table_alias || '.' || rec.fk_column_name || ' = a.id';" + 
				brDent2 + "END IF;" +					
					
				brDent1 + "END LOOP;" +  					
				br +
				brDent1 + "query_ddl = query_ddl || where_clause || ')';" +  
				br + 
				brDent1 + "FOR rec_id IN EXECUTE query_ddl LOOP" +
					brDent2 + "EXECUTE 'SELECT ' || schema_name || '.' || del_func_name || '(' || rec_id || ')' INTO deleted_id;" + 
					brDent2 + "RETURN NEXT deleted_id;" + 
				brDent1 + "END LOOP;" + 
				br +
				brDent1 + "RETURN;" + 
				br +
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql;";		

		cleanupFunction.setDefinition(cleanup_func_ddl);
	}

	@Override
	protected String getArrayDeleteFunctionDeclareField(String arrayDeleteFuncName, String schemaName) {
		return "FUNCTION " + wrapSchemaName(arrayDeleteFuncName, schemaName) + 
				"(" + idType + "[], caller INTEGER DEFAULT 0) RETURNS SETOF " + idType + "";
	}

	@Override
	protected String getSingleDeleteFunctionDeclareField(String singleDeleteFuncName, String schemaName) {
		return "FUNCTION " + wrapSchemaName(singleDeleteFuncName, schemaName) + "(pid " + idType + ") RETURNS " + idType + "";
	}

	private String create_local_delete(String tableName, String schemaName) {
		String code_blcok = "";		
		code_blcok += brDent2 + "DELETE FROM"
						+ brDent3 + wrapSchemaName(tableName, schemaName) + " t"
					+ brDent2 + "USING"
						+ brDent3 + "unnest($1) a(a_id)"
					+ brDent2 + "WHERE"
						+ brDent3 + "t.id = a.a_id"
					+ brDent2 + "RETURNING"
						+ brDent3 + "id";		
		return code_blcok;
	}
	
	private String create_selfref_delete(String tableName, String schemaName) throws SQLException {
		List<String> selfFkColumns = querier.query_selfref_fk(tableName, schemaName);
		String code_block = "";
		for (String fkColumn : selfFkColumns) {	
			if (aggregationInfoCollection.getTableRelationType(tableName, tableName, fkColumn) == RelationType.COMPOSITION) {
				code_block += brDent1 + "-- delete referenced parts"
						+ brDent1 + "PERFORM"
							+ brDent2 + wrapSchemaName(getArrayDeleteFunctionName(tableName), schemaName) + "(array_agg(t.id))"
						+ brDent1 + "FROM"
							+ brDent2 + wrapSchemaName(tableName, schemaName) + " t,"
							+ brDent2 + "unnest($1) a(a_id)"
						+ brDent1 + "WHERE"
							+ brDent2 + "t." + fkColumn + " = a.a_id"
							+ brDent2 + "AND t.id <> a.a_id;" + br;
			}			
		}
		return code_block;
	}

	private String[] create_ref_delete(String tableName, String schemaName) throws SQLException {
		String vars = "";
		String ref_block = "";
		String ref_hook_block = "";
		String ref_child_block = "";
		
		Map<Integer, String> subObjectclasses = adeMetadataManager.getSubObjectclassesFromSuperTable(tableName);
		List<String> directChildTables = new ArrayList<String>();
		List<MnRefEntry> refEntries = querier.query_ref_fk(tableName, schemaName);		
		for (MnRefEntry ref : refEntries) {
			String rootTableName = ref.getRootTableName();			
			String n_table_name = ref.getnTableName();
			String n_fk_column_name = ref.getnFkColumnName();
			String m_table_name = ref.getmTableName();
			String m_fk_column_name = ref.getmFkColumnName();
			
			RelationType nRootRelation = aggregationInfoCollection.getTableRelationType(n_table_name, rootTableName, n_fk_column_name);

			if (m_table_name == null)
				registerDeleteFunction(n_table_name, schemaName);
			
			if (n_fk_column_name.equalsIgnoreCase("id")) { 
				directChildTables.add(n_table_name);
				if (!subObjectclasses.containsValue(n_table_name)) {
					// code-block for deleting ADE hook data 
					ref_hook_block += brDent1 + "-- delete " + n_table_name + "s"
								 	+ brDent1 + "PERFORM " + wrapSchemaName(getArrayDeleteFunctionName(n_table_name), schemaName) + "($1, 1);"
								 	+ br;
				}			 	 
			}
			else {	
				ref_block += create_n_ref_delete(n_table_name, n_fk_column_name, schemaName, nRootRelation);	
				if (nRootRelation == RelationType.AGGREGATION) {
					String var = n_table_name + "_ids";
					if (!vars.contains(var)) {
						vars += brDent1 + var + " " + idType + "[] := '{}';";
					}
				}
			}	
			// If the n_fk_column is not nullable and the table m exists, the table n should be an associative table 
			// between the root table and table m
			if (m_table_name != null) {												
				registerDeleteFunction(m_table_name, schemaName);

				RelationType mRootRelation = aggregationInfoCollection.getTableRelationType(m_table_name, rootTableName, n_table_name);				
				// In case of composition or aggregation between the root table and table m, the corresponding 
				// records in the tables n and m should be deleted using an explicit code-block created below 
				if (mRootRelation != RelationType.ASSOCIATION) {
                    String var = m_table_name + "_ids";
                    if (!vars.contains(var)) {
                        vars += brDent1 + var + " " + idType + "[] := '{}';";
                    }

					ref_block += create_n_m_ref_delete(n_table_name, 
														n_fk_column_name, 
														m_table_name,
														m_fk_column_name,
														schemaName, 
														mRootRelation);
				}	
				// Otherwise, the reverse relation between the root table and table m could be a composition 
				// or aggregation relationship or the two tables have a normal association relationship. In these 
				// both cases, the records in the table n can be directly deleted by means of ON DELETE CASCADE.  
				// The referenced records in the table m shall not be deleted 
				else {
					/**
					updateConstraintsSql += "select citydb_pkg.update_table_constraint('"
							+ n_fk_name + "', '"
							+ n_table_name + "', '"
							+ n_fk_column_name + "', '"
							+ rootTableName + "', '"
							+ "id', '"
							+ "CASCADE', '"
							+ schemaName + "');" + br;
					**/	
				}
			}
		} 

		if (subObjectclasses.size() > 0) {
			for (Entry<Integer, String> entry: subObjectclasses.entrySet()) {
				int childObjectclassId = entry.getKey();
				String childTableName = entry.getValue();
				if (childTableName.equalsIgnoreCase(tableName) || querier.getAssociativeTables(schemaName).contains(childTableName))
					continue;
				
				int caller = 0;
				if (directChildTables.contains(childTableName))
					caller = 1;
				ref_child_block += brDent4 + commentPrefix + "delete " + childTableName						 
								 + brDent4 + "WHEN objectclass_id = " + childObjectclassId + " THEN"
							 	 	+ brDent5 + "dummy_id := " + wrapSchemaName(getArrayDeleteFunctionName(childTableName), schemaName) + "(array_agg(object_id), " + caller + ");";				   
			}			
		}
		
		if (ref_child_block.length() > 0) {
			ref_child_block  = brDent1 + "IF $2 <> 2 THEN"							 
								 + brDent2 + "FOR rec IN"
								 	+ brDent3 + "SELECT"
								 		+ brDent4 + "co.id, co.objectclass_id"
								 	+ brDent3 + "FROM"
								 		+ brDent4 + wrapSchemaName("cityobject", schemaName) + " co, unnest($1) a(a_id)"
								 	+ brDent3 + "WHERE"
								 		+ brDent4 + "co.id = a.a_id"
								 + brDent2 + "LOOP"
									+ brDent3 + "object_id := rec.id::" + idType + ";"
									+ brDent3 + "objectclass_id := rec.objectclass_id::integer;"
									+ brDent3 + "CASE"
										+ ref_child_block 
										+ brDent4 + "ELSE"
											+ brDent5 + "dummy_id := NULL;"
									+ brDent3 + "END CASE;"
									+ br
									+ brDent3 + "IF dummy_id = object_id THEN"
										+ brDent4 + "deleted_child_ids := array_append(deleted_child_ids, dummy_id);"
									+ brDent3 + "END IF;"
								 + brDent2 + "END LOOP;"
							 + brDent1 + "END IF;"
							 + br;
		}
		
		ref_block += ref_hook_block	+ ref_child_block;

		String[] result = {vars, ref_block};
		return result; 
	}

	private String create_n_ref_delete(String tableName, String fk_column_name, String schemaName, RelationType relationType) throws SQLException {
		String code_block = "";
		if (relationType == RelationType.COMPOSITION) {							
			code_block += brDent1 + "--delete " + tableName + "s"		
					+ brDent1 + "PERFORM"
						+ brDent2 + wrapSchemaName(getArrayDeleteFunctionName(tableName), schemaName) + "(array_agg(t.id))"
					+ brDent1 + "FROM"
						+ brDent2 + wrapSchemaName(tableName, schemaName) + " t,"		
						+ brDent2 + "unnest($1) a(a_id)"
					+ brDent1 + "WHERE"
						+ brDent2 + "t." + fk_column_name + " = a.a_id;" + br;	
		}
		else if (relationType == RelationType.AGGREGATION) {
			List<String> joinColumns = adeMetadataManager.getAggregationJoinColumns(tableName);	
				code_block += brDent1 + "--select " + tableName + "s"		
						+ brDent1 + "SELECT"
							+ brDent2 + "array_agg(t.id)"
						+ brDent1 + "INTO"
							+ brDent2 + tableName + "_ids"
						+ brDent1 + "FROM"
							+ brDent2 + wrapSchemaName(tableName, schemaName) + " t,"		
							+ brDent2 + "unnest($1) a(a_id)"
						+ brDent1 + "WHERE"
							+ brDent2 + "t." + fk_column_name + " = a.a_id;"
						+ br;
				if (joinColumns.size() > 1) {	
					code_block += brDent1 + "--update " + tableName + "s"	
						+ brDent1 + "IF -1 = ALL(" + tableName + "_ids) IS NOT NULL THEN"
							+ brDent2 + "UPDATE"
								+ brDent3 + wrapSchemaName(tableName, schemaName)
							+ brDent2 + "SET"
								+ brDent3 + fk_column_name + " = NULL"
							+ brDent2 + "WHERE"
								+ brDent3 + fk_column_name + " IN (SELECT a_id from unnest(" + tableName + "_ids) a(a_id));"
						+ brDent1 + "END IF;" 
						+ br;
			}			
			code_block += this.create_m_ref_delete(tableName, schemaName, RelationType.AGGREGATION);
		}		
		
		return code_block;
	}
	
	private String create_n_m_ref_delete(String n_m_table_name, String n_fk_column_name, String m_table_name, 
			String m_fk_column_name, String schemaName, RelationType tableRelation) throws SQLException {
		String code_block = "";
		code_block += brDent1 + "-- delete references to " + m_table_name + "s"
					+ brDent1 + "WITH " + getArrayDeleteFunctionName(m_table_name) + "_refs AS ("
						+ brDent2 + "DELETE FROM"
							+ brDent3 + wrapSchemaName(n_m_table_name, schemaName) + " t"
						+ brDent2 + "USING"
							+ brDent3 + "unnest($1) a(a_id)"
						+ brDent2 + "WHERE"
							+ brDent3 + "t." + n_fk_column_name + " = a.a_id"
						+ brDent2 + "RETURNING"
							+ brDent3 + "t." + m_fk_column_name
					+ brDent1 + ")"
					+ brDent1 + "SELECT"
						+ brDent2 + "array_agg(" + m_fk_column_name + ")"
					+ brDent1 + "INTO"
						+ brDent2 + m_table_name + "_ids"
					+ brDent1 + "FROM"
						+ brDent2 + getArrayDeleteFunctionName(m_table_name) + "_refs;" + br
				+ create_m_ref_delete(m_table_name, schemaName, tableRelation);		
		
		return code_block;
	}
	
	private String create_m_ref_delete(String m_table_name, String schemaName, RelationType tableRelation) throws SQLException {							
		List<MnRefEntry> nmEntries = querier.query_ref_fk(m_table_name, schemaName);	
		List<ReferencingEntry> aggComprefList = new ArrayList<ReferencingEntry>(); 
			
		for (MnRefEntry ref : nmEntries) {
			String _nFkColumn = ref.getnFkColumnName();
			String _mTable = ref.getmTableName();
			String _nTable = ref.getnTableName();
			if (!_nFkColumn.equalsIgnoreCase("id")) {
				if (_mTable != null) {
					if (aggregationInfoCollection.getTableRelationType(m_table_name, _mTable, _nTable) != RelationType.ASSOCIATION) {
						aggComprefList.add(new ReferencingEntry(_nTable, _nFkColumn));
					}	
				}			
				if (aggregationInfoCollection.getTableRelationType(m_table_name, _nTable, _nFkColumn) != RelationType.ASSOCIATION) {
					aggComprefList.add(new ReferencingEntry(_nTable, _nFkColumn));
				}
			}			
		}
		
		String code_block = 
				  brDent1 + "-- delete " + wrapSchemaName(m_table_name, schemaName) + "(s)";
				  
		List<String> joinColumns = adeMetadataManager.getAggregationJoinColumns(m_table_name);
		if (tableRelation != RelationType.COMPOSITION && joinColumns.size() > 1) {
			code_block += 
				  brDent1 + "IF -1 = ALL(" + m_table_name + "_ids) IS NOT NULL THEN";
			code_block +=
					  brDent2 + "SELECT"
						+ brDent3 + "t.id"
					+ brDent2 + "INTO"
						+ brDent3 + m_table_name + "_ids"
					+ brDent2 + "FROM"
						+ brDent3 + wrapSchemaName(m_table_name, schemaName) + " t"		
					+ brDent2 + "WHERE"
						+ brDent3 + "t.id IN (SELECT a_id from unnest(" + m_table_name + "_ids) a(a_id))";
				for (int i = 0; i < joinColumns.size(); i++) {
					code_block += 
							  brDent3 + "AND " + joinColumns.get(i) + " IS NULL";
				}	
			code_block += ";"
				 + brDent1 + "END IF;" + br;	
		}
		
		if (aggComprefList.size() == 0) {
			code_block += 
				   brDent1 + "IF -1 = ALL(" + m_table_name + "_ids) IS NOT NULL THEN"
					+ brDent2 + "PERFORM " + wrapSchemaName(getArrayDeleteFunctionName(m_table_name), schemaName) + "(" + m_table_name + "_ids);"
				 + brDent1 + "END IF;" + br;
			return code_block;
		}	
		
		String join_block = "";
		String where_block = "";
		int index = 1;
		for (ReferencingEntry ref : aggComprefList) {				
			String refTable = ref.getRefTable();
			String refColumn = ref.getRefColumn();
			join_block += "LEFT JOIN"
							+ brDent3 + wrapSchemaName(refTable, schemaName) + " n" + index
							+ brDent3 + "ON n" + index + "." + refColumn + "  = a.a_id";			
			where_block += "n" + index + "." + refColumn + " IS NULL";
			if (index < aggComprefList.size()) {
				join_block += brDent2;
				where_block += brDent3 + "AND ";
			}				
			index++; 
		}
		
		code_block += 
				 brDent1 + "IF -1 = ALL(" + m_table_name + "_ids) IS NOT NULL THEN"							
					+ brDent2 + "PERFORM"
						+ brDent3 + wrapSchemaName(getArrayDeleteFunctionName(m_table_name), schemaName) + "(array_agg(a.a_id))"
					+ brDent2 + "FROM"
						+ brDent3 + "(SELECT DISTINCT unnest(" + m_table_name + "_ids) AS a_id) a";
		
		// In the case of composition, the sub-features shall be directly deleted 
		// without needing to check if they are referenced by another super features
		// In other cases (aggregation), this check is required.
		if (tableRelation != RelationType.COMPOSITION && join_block.length() > 0) {			
			code_block += brDent2 + join_block
						+ brDent2 + "WHERE " + where_block + ";";
		}
		else {
			code_block += ";";
		}
		
		code_block += brDent1 + "END IF;" + br;

		return code_block;
	}

	private String[] create_ref_to_delete(String tableName, String schemaName) throws SQLException {
		String vars = "";
		String returning_block = "";
		String collect_block = "";
		String into_block = "";
		String fk_block = "";
		
		List<ReferencedEntry> refEntries = querier.query_ref_to_fk(tableName, schemaName);
		
		for (ReferencedEntry entry : refEntries) {
			String ref_table_name = entry.getRefTable();
			String[] fk_columns = entry.getFkColumns();
			
			// Exclude the case of normal associations for which the referenced features should be not be deleted. 
			RelationType defaultTableRelation = RelationType.COMPOSITION;
			List<String> tmpList = new ArrayList<String>();
			for (String fk_column: fk_columns) {
				RelationType tableRelation = aggregationInfoCollection.getTableRelationType(ref_table_name, tableName, fk_column);
				if (tableRelation != RelationType.ASSOCIATION) {
					tmpList.add(fk_column);
					if (tableRelation == RelationType.AGGREGATION)
						defaultTableRelation = tableRelation;
				}
			}
			fk_columns = tmpList.toArray(new String[0]);
			if (fk_columns.length > 0) {
                String var = ref_table_name + "_ids";
                if (!vars.contains(var)) {
                    vars += brDent1 + var + " " + idType + "[] := '{}';";
                }

				collect_block += "," 
							  + brDent2;
				for (int i = 0; i < fk_columns.length; i++) {
					returning_block += "," 
									+ brDent3 + fk_columns[i];
					collect_block += "array_agg(" + fk_columns[i] + ")";
					
					if (i < fk_columns.length - 1) {
						collect_block += " ||" 
									  + brDent2;
					}				
				}
				into_block += "," 
						    + brDent2 + ref_table_name + "_ids";
				
				registerDeleteFunction(ref_table_name, schemaName);
				
				// Check if we need add additional code-block for cleaning up the sub-features
				// for the case of aggregation relationship. 
				fk_block += this.create_m_ref_delete(ref_table_name, schemaName, defaultTableRelation);
			}
		}
		
		String[] result = { 
							vars, 
							returning_block, 
							collect_block,
							into_block,
							fk_block 
						};
		
		return result;
	}

	private String create_ref_to_parent_delete(String tableName, String schemaName) throws SQLException {
		String code_block = "";
		String parent_table = querier.query_ref_to_parent_fk(tableName, schemaName);		
		if (parent_table != null) {
			List<String> adeHookTables = adeMetadataManager.getADEHookTables(parent_table);
			if (!adeHookTables.contains(tableName)) {
				registerDeleteFunction(parent_table, schemaName);
				
				code_block += brDent1 + "IF $2 <> 1 THEN"
						    	+ brDent2 + "-- delete " + parent_table
						    	+ brDent2 + "PERFORM " + wrapSchemaName(getArrayDeleteFunctionName(parent_table), schemaName) + "(deleted_ids, 2);"
						    + brDent1 + "END IF;" + br;
			}			
		}
		return code_block;
	}

}
