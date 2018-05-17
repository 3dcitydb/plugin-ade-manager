package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.postgis;

import java.io.PrintStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DBDeleteFunction;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.AbstractDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.model.DBStoredFunction;
import org.citydb.plugins.ade_manager.registry.query.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencingEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.RelationType;

public class PostgisDeleteGeneratorGenerator extends AbstractDeleteScriptGenerator {

	public PostgisDeleteGeneratorGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}
	
	@Override
	public void installDeleteScript(String scriptString) throws SQLException {	
		CallableStatement cs = null;
		try {
			cs = connection.prepareCall(scriptString);
			cs.execute();
		}
		finally {
			if (cs != null)
				cs.close();
		}		
	}
	
	@Override
	protected void constructDeleteFunction(DBDeleteFunction deleteFunction) throws SQLException  {
		String tableName = deleteFunction.getTargetTable();
		String funcName = deleteFunction.getName();
		String schemaName = deleteFunction.getOwnerSchema();
		
		String delete_func_ddl =
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(funcName, schemaName) + 
				"(int[], caller INTEGER DEFAULT 0) RETURNS SETOF int AS" + br + "$body$" + br;
		
		String declare_block = 
				"DECLARE" + 
				brDent1 + "deleted_ids int[] := '{}';"+
				brDent1 + "object_id integer;" +
				brDent1 + "objectclass_id integer;";
		
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
				brDent1 + "RETURN QUERY" +
					brDent2 + "SELECT unnest(deleted_ids);";
		
		// Code-block for deleting self-references in case of e.g. building/buildingParts
		// and Composite(Multi)Surface/SurfaceGeometry which have composition relations
		if (checkTableRelationType(tableName, tableName) == RelationType.composition) {
			pre_block = this.create_selfref_delete(tableName, schemaName);
		}
				
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
	protected void printDDLForAllDeleteFunctions(PrintStream writer) {
		writer.println("------------------------------------------" + br);
		for (DBStoredFunction func: functionCollection.values()) {
			String funcDefinition = func.getDefinition();
			writer.println(funcDefinition);
			writer.println("------------------------------------------" + br);
		};
	}

	@Override
	protected void constructLineageDeleteFunction(DBDeleteFunction deleteFunction) {
		String schemaName = deleteFunction.getOwnerSchema();
		
		String delete_func_ddl = "";
		delete_func_ddl += 
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(deleteFunction.getName(), schemaName) + 
				"(lineage_value TEXT, objectclass_id INTEGER DEFAULT 0) RETURNS SETOF int AS" + br + 
				"$body$" + br +
				sqlComment("Function for deleting cityobjects by lineage value") + br + 
				"DECLARE" + 
				brDent1 + "deleted_ids int[] := '{}';" + br + 
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
					brDent2 +  "PERFORM " + wrapSchemaName("del_cityobject(deleted_ids)", schemaName) + ";" +
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
	protected void constructAppearanceCleanupFunction(DBDeleteFunction cleanupFunction) {
		String schemaName = cleanupFunction.getOwnerSchema();
			
		String cleanup_func_ddl = "";
		cleanup_func_ddl += 
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(cleanupFunction.getName(), schemaName) + 
				"() RETURNS SETOF int AS" + br + 
				"$body$" + br +
				sqlComment("Function for cleaning up global appearance") + br + 
				"DECLARE" + 
				brDent1 + "deleted_id int;" + 
				brDent1 + "app_id int;" + br +
				"BEGIN" + 
				brDent1 + "PERFORM " + wrapSchemaName("del_surface_data", schemaName) + "(array_agg(s.id))" +	
					brDent2 + "FROM " + wrapSchemaName("surface_data", schemaName) + " s " + 
					brDent2 + "LEFT OUTER JOIN " + wrapSchemaName("textureparam", schemaName) + " t ON s.id = t.surface_data_id" + 
					brDent2 + "WHERE t.surface_data_id IS NULL;" + 
					br +
					brDent2 + "FOR app_id IN" + 						
						brDent3 + "SELECT a.id FROM " + wrapSchemaName("appearance", schemaName) + " a" +
							brDent4 + "LEFT OUTER JOIN appear_to_surface_data asd ON a.id=asd.appearance_id" +
								brDent5 + "WHERE a.cityobject_id IS NULL AND asd.appearance_id IS NULL" + 
					brDent2 + "LOOP" + 
						brDent3 +  "DELETE FROM " + wrapSchemaName("appearance", schemaName) + " WHERE id = app_id RETURNING id INTO deleted_id;" +
						brDent3 +  "RETURN NEXT deleted_id;" + 
					brDent2 + "END LOOP;" + 	
					br +
				brDent1 + "RETURN;" + br +
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";		

		cleanupFunction.setDefinition(cleanup_func_ddl);
	}

	@Override
	protected void constructSchemaCleanupFunction(DBDeleteFunction cleanupFunction) {
		String schemaName = cleanupFunction.getOwnerSchema();
		
		String cleanup_func_ddl = "";
		cleanup_func_ddl += 
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(cleanupFunction.getName(), schemaName) + 
				"() RETURNS SETOF void AS" + br + 
				"$body$" + br +
				sqlComment("Function for cleaning up data schema") + br + 
				"DECLARE" + 
				brDent1 + "rec RECORD;" + 
				br +
				"BEGIN" + 
				brDent1 + "FOR rec IN"+	
					brDent2 + "SELECT table_name FROM information_schema.tables where table_schema = '" + schemaName + "'" + 
					brDent2 + "AND table_name <> 'database_srs'" + 
					brDent2 + "AND table_name <> 'objectclass'" + 
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
			code_block += brDent1 + "-- delete referenced parts"
						+ brDent1 + "PERFORM"
							+ brDent2 + wrapSchemaName(createFunctionName(tableName), schemaName) + "(array_agg(t.id))"
						+ brDent1 + "FROM"
							+ brDent2 + wrapSchemaName(tableName, schemaName) + " t,"
							+ brDent2 + "unnest($1) a(a_id)"
						+ brDent1 + "WHERE"
							+ brDent2 + "t." + fkColumn + " = a.a_id"
							+ brDent2 + "AND t.id <> a.a_id;" + br;
		}
		return code_block;
	}

	private String[] create_ref_delete(String tableName, String schemaName) throws SQLException {
		String vars = "";
		String ref_block = "";
		String ref_hook_block = "";
		String ref_child_block = "";
		
		Map<Integer, String> subObjectclasses = adeMetadataManager.querySubObjectclassesFromSuperTable(tableName);
		List<String> directChildTables = new ArrayList<String>();
		List<MnRefEntry> refEntries = querier.query_ref_fk(tableName, schemaName);		
		for (MnRefEntry ref : refEntries) {
			String rootTableName = ref.getRootTableName();			
			String n_table_name = ref.getnTableName();
			String n_fk_column_name = ref.getnFkColumnName();
			String m_table_name = ref.getmTableName();
			String m_fk_column_name = ref.getmFkColumnName();
			
			RelationType nRootRelation = checkTableRelationType(n_table_name, rootTableName);

			if (!functionCollection.containsKey(n_table_name) && m_table_name == null)
				registerDeleteFunction(n_table_name, schemaName);
			
			if (n_fk_column_name.equalsIgnoreCase("id")) { 
				directChildTables.add(n_table_name);
				if (!subObjectclasses.containsValue(n_table_name)) {
					// code-block for deleting ADE hook data 
					ref_hook_block += brDent1 + "-- delete " + n_table_name + "s"
								 	+ brDent1 + "PERFORM " + wrapSchemaName(createFunctionName(n_table_name), schemaName) + "($1, 1);"
								 	+ br;
				}			 	 
			}
			else {											
				if (nRootRelation == RelationType.composition) {							
					ref_block += create_n_ref_delete(n_table_name, n_fk_column_name, schemaName);
				}		
				else {
					// If the n_fk_column is nullable, the n_fk_column could be a foreign key column like
					// lodx_multi_surf_fk in the BUILDING table, or like building_id_fk in the THEMATIC_SURFACE table 
					// In both these cases, the foreign key on this column shall be defined as "ON DELETE SET NULL"					
					/**
					if (!n_column_is_not_null) { 
						updateConstraintsSql += "select citydb_pkg.update_table_constraint('"
								+ n_fk_name + "', '"
								+ n_table_name + "', '"
								+ n_fk_column_name + "', '"
								+ rootTableName + "', '"
								+ "id', '"
								+ "SET NULL', '"
								+ schemaName + "');" + br;
					}	
					**/					
				}
			}	
			// If the n_fk_column is not nullable and the table m exists, the table n should be an associative table 
			// between the root table and table m
			if (m_table_name != null) {												
				if (!functionCollection.containsKey(m_table_name))
					registerDeleteFunction(m_table_name, schemaName);

				RelationType mRootRelation = checkTableRelationType(m_table_name, rootTableName);
				
				// In case of composition or aggregation between the root table and table m, the corresponding 
				// records in the tables n and m should be deleted using an explicit code-block created below 
				if (mRootRelation != RelationType.no_agg_comp) {
					vars += brDent1 + m_table_name + "_ids int[] := '{}';";
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
				ref_child_block += br
						 + brDent3 + "-- delete " + childTableName						 
						 + brDent3 + "IF objectclass_id = " + childObjectclassId + " THEN"
					 	 + brDent4 + "PERFORM " + wrapSchemaName(createFunctionName(childTableName), schemaName) + "(array_agg(object_id), " + caller + ");"
						 + brDent3 + "END IF;";
			}			
		}
		
		if (ref_child_block.length() > 0) {
			ref_child_block  = brDent1 + "IF $2 <> 2 THEN"							 
							 + brDent2 + "FOREACH object_id IN ARRAY $1"
							 + brDent2 + "LOOP"
								+ brDent3 + "EXECUTE format('SELECT objectclass_id FROM " + wrapSchemaName(tableName, schemaName) + " WHERE id = %L', object_id) INTO objectclass_id;"
								+ ref_child_block 
							 + brDent2 + "END LOOP;"
							 + brDent1 + "END IF;"
							 + br;
		}
		
		ref_block += ref_hook_block	+ ref_child_block;

		String[] result = {vars, ref_block};
		return result; 
	}

	private String create_n_ref_delete(String tableName, String fk_column_name, String schemaName) {
		String code_block = "";
		code_block += brDent1 + "--delete " + tableName + "s"		
					+ brDent1 + "PERFORM"
						+ brDent2 + wrapSchemaName(createFunctionName(tableName), schemaName) + "(array_agg(t.id))"
					+ brDent1 + "FROM"
						+ brDent2 + wrapSchemaName(tableName, schemaName) + " t,"		
						+ brDent2 + "unnest($1) a(a_id)"
					+ brDent1 + "WHERE"
						+ brDent2 + "t." + fk_column_name + " = a.a_id;" + br;	
		
		return code_block;
	}
	
	private String create_n_m_ref_delete(String n_m_table_name, String n_fk_column_name, String m_table_name, 
			String m_fk_column_name, String schemaName, RelationType tableRelation) throws SQLException {
		String code_block = "";
		code_block += brDent1 + "-- delete references to " + m_table_name + "s"
					+ brDent1 + "WITH " + createFunctionName(m_table_name) + "_refs AS ("
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
						+ brDent2 + createFunctionName(m_table_name) + "_refs;" + br
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
					if (checkTableRelationType(m_table_name, _mTable) != RelationType.no_agg_comp) {
						aggComprefList.add(new ReferencingEntry(_nTable, _nFkColumn));
					}	
				}			
				if (checkTableRelationType(m_table_name, _nTable) != RelationType.no_agg_comp) {
					aggComprefList.add(new ReferencingEntry(_nTable, _nFkColumn));
				}
			}			
		}
			
		String code_block = "";		
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
		
		code_block += brDent1 + "-- delete " + wrapSchemaName(m_table_name, schemaName) + "(s)"
					+ brDent1 + "IF -1 = ALL(" + m_table_name + "_ids) IS NOT NULL THEN"
						+ brDent2 + "PERFORM"
							+ brDent3 + wrapSchemaName(createFunctionName(m_table_name), schemaName) + "(array_agg(a.a_id))"
						+ brDent2 + "FROM"
							+ brDent3 + "(SELECT DISTINCT unnest(" + m_table_name + "_ids) AS a_id) a";
		
		// In the case of composition, the sub-features shall be directly deleted 
		// without needing to check if they are referenced by another super features
		// In other cases (aggregation), this check is required.
		if (tableRelation != RelationType.composition) {
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
	
			RelationType tableRelation = checkTableRelationType(ref_table_name, tableName);
			
			// Exclude the case of normal associations for which the referenced features should be not be deleted. 
			if (tableRelation != RelationType.no_agg_comp) {				
				vars += brDent1 + ref_table_name + "_ids int[] := '{}';";
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
				
				if (!functionCollection.containsKey(ref_table_name))
					registerDeleteFunction(ref_table_name, schemaName);
				
				// Check if we need add additional code-block for cleaning up the sub-features
				// for the case of aggregation relationship. 
				fk_block += this.create_m_ref_delete(ref_table_name, schemaName, tableRelation);
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
				if (!functionCollection.containsKey(parent_table))
					registerDeleteFunction(parent_table, schemaName);
				
				code_block += brDent1 + "IF $2 <> 1 THEN"
						    	+ brDent2 + "-- delete " + parent_table
						    	+ brDent2 + "PERFORM " + wrapSchemaName(createFunctionName(parent_table), schemaName) + "(deleted_ids, 2);"
						    + brDent1 + "END IF;" + br;
			}			
		}
		return code_block;
	}

}
