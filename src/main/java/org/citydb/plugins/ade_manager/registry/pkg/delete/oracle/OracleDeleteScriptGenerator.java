package org.citydb.plugins.ade_manager.registry.pkg.delete.oracle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.citydb.database.schema.mapping.RelationType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteFunction;
import org.citydb.plugins.ade_manager.registry.query.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencingEntry;

public class OracleDeleteScriptGenerator extends DeleteScriptGenerator {

	public OracleDeleteScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
	}
	
	@Override
	protected DBSQLScript buildDeleteScript() throws SQLException {
		DBSQLScript dbScript = new DBSQLScript();
		
		// package header
		String packageHeader = 
					"CREATE OR REPLACE PACKAGE citydb_delete" + br +
					"AS" + br +
					functionCollection.printFunctionDeclareFields(dent) +
					"END citydb_delete;" + br +
					"/";		
		dbScript.addSQLBlock(packageHeader);
		
		// package body	
		String packageBody =
					"CREATE OR REPLACE PACKAGE BODY citydb_delete" + br +
					"AS " + br +		
					functionCollection.printFunctionDefinitions(dent + separatorLine) + 
					"END citydb_delete;" + br + 
					"/";		
		dbScript.addSQLBlock(packageBody);
		
		return dbScript;
	}

	@Override
	protected void constructArrayDeleteFunction(DeleteFunction deleteFunction) throws SQLException {
		String tableName = deleteFunction.getTargetTable();
		String funcName = deleteFunction.getName();
		String schemaName = deleteFunction.getOwnerSchema();		
		AtomicInteger var_index = new AtomicInteger(0);
		
		String declareField = "FUNCTION " + funcName + "(pids ID_ARRAY, caller int := 0) RETURN ID_ARRAY";		
		deleteFunction.setDeclareField(declareField);
		
		String delete_func_ddl =
				dent + declareField + 
				brDent1 + "IS";
		
		String declare_block = 
					brDent2 + "object_id number;" +
					brDent2 + "objectclass_id number;" +			
					brDent2 + "object_ids ID_ARRAY := ID_ARRAY();" +
					brDent2 + "deleted_child_ids ID_ARRAY := ID_ARRAY();" +
					brDent2 + "deleted_ids ID_ARRAY := ID_ARRAY();" +							
					brDent2 + "dummy_ids ID_ARRAY := ID_ARRAY();" +
					brDent2 + "cur sys_refcursor;";
		
		String pre_block = "";
		String post_block = "";
		String delete_block = "";	
		
		String delete_into_block = 
					brDent2 + "BULK COLLECT INTO"  
					  + brDent3 +  "deleted_ids";
		
		String return_block =
					brDent2 + "IF deleted_child_ids IS NOT EMPTY THEN" +
				 		brDent3 + "deleted_ids := deleted_child_ids;" + 
				 	brDent2 + "END IF;" + 
				 	br +
					brDent2 + "RETURN deleted_ids;" + br;
		
		// Code-block for deleting self-references in case of e.g. building/buildingParts
		pre_block += this.create_selfref_delete(tableName, schemaName);
				
		// Code-block for deleting referenced sub-features with aggregation/composition or inheritance relationship
		String[] result = create_ref_delete(tableName, schemaName, var_index);
		declare_block += result[0];
		pre_block += result[1];
		
		// Main Delete for the current table
		delete_block += create_local_delete(tableName , schemaName);
				
		// Code-block for deleting referenced tables with 1:0..1 composition or N: 0..1 aggregation 
		// e.g. the composition relationship between building and surface geometry,
		// the aggregation relationship between features and their shared implicit geometry	
		String[] tmp = create_ref_to_delete(tableName, schemaName, var_index);
		String vars = tmp[0]; 
		String returning_block = tmp[1]; 
		String collect_block = tmp[2];
		String into_block = tmp[3];
		String fk_block = tmp[4]; 
		
		declare_block += vars;
		delete_block += returning_block;
		delete_into_block += into_block;
		post_block += collect_block + fk_block;
		
		// Code-block for deleting the records in the parent table: e.g. deleting a record in the BUILDING table requires
		// the deletion of the corresponding record in the CITYOBJECT table.
		post_block += create_ref_to_parent_delete(tableName, schemaName);
				
		// Putting all together
		delete_func_ddl += 
					declare_block + 
					brDent1 + "BEGIN" + 
					pre_block + 
					brDent2 + "-- delete " + tableName + "s" + 
					delete_block + 
					delete_into_block + ";" +
					br +
					post_block +  					
					return_block +	
					brDent1 + "END;";	

		deleteFunction.setDefinition(delete_func_ddl);
	}

	@Override
	protected void constructSingleDeleteFunction(DeleteFunction singleDeleteFunction, String arrayDeleteFuncname) {
		String funcName = singleDeleteFunction.getName();
		
		String declareField = "FUNCTION " + funcName + "(pid NUMBER) RETURN NUMBER";		
		singleDeleteFunction.setDeclareField(declareField);
		
		String delete_func_ddl =
				dent + declareField + 
				brDent1 + "IS" +
					brDent2 + "deleted_id NUMBER;" + 
					brDent2 + "dummy_ids ID_ARRAY;" + 
				brDent1 + "BEGIN" + 
					brDent2 + "dummy_ids := " + arrayDeleteFuncname +  "(ID_ARRAY(pid));" +
					br + 
					brDent2 + "IF dummy_ids IS NOT EMPTY THEN" +
						brDent3 + "deleted_id := dummy_ids(1);" + 
					brDent2 + "END IF;" + 
					br + 
					brDent2 + "RETURN deleted_id;" + 
					br + 				
					brDent2 + "EXCEPTION" + 
							brDent3 + "WHEN NO_DATA_FOUND THEN" + 
								brDent4 + "RETURN deleted_id;" + 	
				brDent1 + "END;";
		
		singleDeleteFunction.setDefinition(delete_func_ddl);
	}

	@Override
	protected void constructLineageDeleteFunction(DeleteFunction deleteFunction) {
		String declareField = "FUNCTION " + deleteFunction.getName() + "(lineage_value varchar2, objectclass_id int := 0) RETURN ID_ARRAY";
		deleteFunction.setDeclareField(declareField);
		
		String delete_func_ddl = "";
		delete_func_ddl += dent +  
				declareField + 
				brDent1 + "IS" + 
					brDent2 + "deleted_ids id_array := id_array();" +
					brDent2 + "dummy_ids id_array := id_array();" +
				brDent1 + "BEGIN" + 
					brDent2 + "IF objectclass_id = 0 THEN" +	
						brDent3 + "SELECT" + 
							brDent4 + "c.id" + 
						brDent3 + "BULK COLLECT INTO" + 
							brDent4 + "deleted_ids" + 
						brDent3 + "FROM" + 						
							brDent4 + "cityobject c" + 
						brDent3 + "WHERE" +
							brDent4 + "c.lineage = lineage_value;" + 
					brDent2 + "ELSE" + 
						brDent3 + "SELECT" + 
							brDent4 + "c.id" + 
						brDent3 + "BULK COLLECT INTO" + 
							brDent4 + "deleted_ids" + 
						brDent3 + "FROM" + 						
							brDent4 + "cityobject c" + 
						brDent3 + "WHERE" +
							brDent4 + "c.lineage = lineage_value AND c.objectclass_id = objectclass_id;" + 				
					brDent2 + "END IF;" + 
					br +
					brDent2 + "IF deleted_ids IS NOT EMPTY THEN" + 
						brDent3 + "FOR i in 1..deleted_ids.count" +
						brDent3 + "LOOP" +
							brDent4 + "dummy_ids := " + getArrayDeleteFunctionName("cityobject") + "(ID_ARRAY(deleted_ids(i)), 1);" + 
						brDent3 + "END LOOP;" +
					brDent2 + "END IF;" + 
					br + 
					brDent2 + "RETURN deleted_ids;" + 
				brDent1 + "END;";
		
		deleteFunction.setDefinition(delete_func_ddl);
	}

	@Override
	protected void constructAppearanceCleanupFunction(DeleteFunction cleanupFunction) {
		String declareField = "FUNCTION " + cleanupFunction.getName() + " RETURN ID_ARRAY";
		cleanupFunction.setDeclareField(declareField);
		
		String cleanup_func_ddl = "";
		cleanup_func_ddl += dent + 
				declareField + 
				brDent1 + "IS" +   
					brDent2 + "deleted_ids ID_ARRAY := ID_ARRAY();" +
					brDent2 + "surface_data_ids ID_ARRAY;" +
					brDent2 + "appearance_ids ID_ARRAY;" +
					brDent2 + "dummy_ids ID_ARRAY := ID_ARRAY();" + 
				brDent1 + "BEGIN" + 	
					brDent2 + "SELECT" + 
						brDent3 + "s.id" + 
					brDent2 + "BULK COLLECT INTO" + 
						brDent3 + "surface_data_ids" + 
					brDent2 + "FROM" + 						
						brDent3 + "surface_data s" + 
					brDent2 + "LEFT OUTER JOIN" +
						brDent3 + "textureparam t " +
						brDent3	+ "ON s.id=t.surface_data_id" + 
					brDent2 + "WHERE" +
						brDent3 + "t.surface_data_id IS NULL;" + 
					br +
					brDent2 + "IF surface_data_ids IS NOT EMPTY THEN" + 	
						brDent3 + "dummy_ids := " + getArrayDeleteFunctionName("surface_data") + "(surface_data_ids);" + 
					brDent2 + "END IF;" +
					br +
					brDent2 + "SELECT" + 
						brDent4 + "a.id" + 
					brDent3 + "BULK COLLECT INTO" + 
						brDent4 + "appearance_ids" + 
					brDent3 + "FROM" + 						
						brDent4 + "appearance a" + 
					brDent3 + "LEFT OUTER JOIN" +
						brDent4 + "appear_to_surface_data asd" + 			
						brDent4 + "ON a.id=asd.appearance_id" +
					brDent3 + "WHERE" + 						
						brDent4 + "a.cityobject_id IS NULL" +
						brDent4 + "AND asd.appearance_id IS NULL;" +
					br +
					brDent2 + "IF appearance_ids IS NOT EMPTY THEN" + 
						brDent3 + "deleted_ids := " + getArrayDeleteFunctionName("appearance") + "(appearance_ids);" +
					brDent2 + "END IF;" + 
					br + 
					brDent2 + "RETURN deleted_ids;" + 
				brDent1 + "END;";

		cleanupFunction.setDefinition(cleanup_func_ddl);
	}

	@Override
	protected void constructSchemaCleanupFunction(DeleteFunction cleanupFunction) {
		String declareField = "PROCEDURE " + cleanupFunction.getName();
		cleanupFunction.setDeclareField(declareField);
		
		String cleanup_func_ddl = "";
		cleanup_func_ddl += dent + 
				declareField + 
				brDent1 + "IS" + 
					brDent2 + "dummy_str strarray;" +
					brDent2 + "seq_value number;" +				
				brDent1 + "BEGIN" + 
					br +
					brDent2 + "dummy_str := citydb_idx.drop_spatial_indexes();" + 
					br +
					brDent2 + "for uc in (" + 
						brDent3 + "select constraint_name, table_name from user_constraints" + 
					brDent2 + ")" + 
					brDent2 + "LOOP" + 
						brDent3 + "execute immediate 'alter table '||uc.table_name||' disable constraint '||uc.constraint_name||'';" + 						
					brDent2 + "END loop;" + 
					br +
					brDent2 + "for ut in (" +
						brDent3 + "select table_name FROM user_tables" +
						brDent3	+ "WHERE table_name NOT IN ("
									+ "'DATABASE_SRS', "
									+ "'OBJECTCLASS', "
									+ "'INDEX_TABLE', "
									+ "'ADE', "
									+ "'SCHEMA', "
									+ "'SCHEMA_TO_OBJECTCLASS', "
									+ "'SCHEMA_REFERENCING', "
									+ "'AGGREGATION_INFO')" + 
						brDent3 + "AND table_name NOT LIKE '%\\_AUX' ESCAPE '\\'" +
						brDent3 + "AND table_name NOT LIKE '%TMP\\_%' ESCAPE '\\'" + 
						brDent3 + "AND table_name NOT LIKE '%MDRT%'" + 	
						brDent3 + "AND table_name NOT LIKE '%MDXT%'" + 
						brDent3 + "AND table_name NOT LIKE '%MDNT%'" +
					brDent2 + ")" + 
					brDent2 + "LOOP" + 
						brDent3 + "execute immediate 'truncate table '||ut.table_name||'';" + 
					brDent2 + "END loop;" +
					br +
					brDent2 + "for uc in (" + 						
						brDent3 + "select constraint_name, table_name from user_constraints" + 
					brDent2 + ")" +
					brDent2 + "LOOP" + 			
						brDent3 + "execute immediate 'alter table '||uc.table_name||' enable constraint '||uc.constraint_name||'';" +
					brDent2 + "END loop;" + 
					br +
					brDent2 + "for us in (" +
						brDent3 + "select sequence_name from user_sequences" +
						brDent3	+ "WHERE sequence_name NOT IN ("
									+ "'INDEX_TABLE_SEQ', "						
									+ "'ADE_SEQ', "		
									+ "'SCHEMA_SEQ')" + 					
						brDent3 + "AND sequence_name NOT LIKE '%\\_AUX' ESCAPE '\\'" + 
						brDent3 + "AND sequence_name NOT LIKE '%TMP\\_%' ESCAPE '\\'" +
						brDent3 + "AND sequence_name NOT LIKE '%MDRS%'" + 
						brDent3 + "AND sequence_name NOT LIKE '%MDXS%'" +  					
						brDent3 + "AND sequence_name NOT LIKE '%MDNS%'" + 
					brDent2 + ")" + 
					brDent2 + "LOOP" + 
						brDent3 + "execute immediate 'select ' || us.sequence_name || '.nextval from dual' into seq_value;" +
						brDent3 + "if (seq_value = 1) then" +
							brDent4 + "execute immediate 'select ' || us.sequence_name || '.nextval from dual' into seq_value;" +
						brDent3 + "end if;" +
						brDent3 + "execute immediate 'alter sequence ' || us.sequence_name || ' increment by ' || (seq_value-1)*-1;" +
						brDent3 + "execute immediate 'select ' || us.sequence_name || '.nextval from dual' into seq_value;" +
						brDent3 + "execute immediate 'alter sequence ' || us.sequence_name || ' increment by 1';" +
					brDent2 + "END LOOP;" + 						
					br +
					brDent2 + "dummy_str := citydb_idx.create_spatial_indexes();" + 
					br +								
				brDent1 + "END;";

		cleanupFunction.setDefinition(cleanup_func_ddl);
	}

	private String create_local_delete(String tableName, String schemaName) {
		String code_blcok = "";		
		code_blcok += brDent2 + "DELETE FROM"
						+ brDent3 + tableName + " t"
					+ brDent2 + "WHERE EXISTS ("
						+ brDent3 + "SELECT"
							+ brDent4 + "a.COLUMN_VALUE"
						+ brDent3 + "FROM"
							+ brDent4 + "TABLE(pids) a"
						+ brDent3 + "WHERE"
							+ brDent4 + "a.COLUMN_VALUE = t.id"
						+ brDent3 + ")"	
					+ brDent2 + "RETURNING"
						+ brDent3 + "id";		
		return code_blcok;
	}

	private String create_selfref_delete(String tableName, String schemaName) throws SQLException {
		String self_block = "";		
		List<String> selfFkColumns = querier.query_selfref_fk(tableName, schemaName);		
		for (String fkColumn : selfFkColumns) {	
			if (aggregationInfoCollection.getTableRelationType(tableName, tableName, fkColumn) == RelationType.COMPOSITION) {
				self_block += brDent2 + "-- delete referenced parts"
						+ brDent2 + "SELECT"
							+ brDent3 + "t.id"
						+ brDent2 + "BULK COLLECT INTO"
							+ brDent3 + "object_ids"
						+ brDent2 + "FROM"
							+ brDent3 + tableName + " t,"
							+ brDent3 + "TABLE(pids) a"
						+ brDent2 + "WHERE"
							+ brDent3 + "t." + fkColumn + " = a.COLUMN_VALUE"
							+ brDent3 + "AND t.id <> a.COLUMN_VALUE;" 
						+ br  // space line
						+ brDent2 + "IF object_ids IS NOT EMPTY THEN"
							+ brDent3 + "dummy_ids := " + getArrayDeleteFunctionName(tableName) + "(object_ids);"
						+ brDent2 + "END IF;" + br;	
			}		
		}
		
		return self_block; 
	}
	
	private String[] create_ref_delete(String tableName, String schemaName, AtomicInteger var_index) throws SQLException {
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

			if (!functionCollection.containsKey(n_table_name) && m_table_name == null) {
				registerDeleteFunction(n_table_name, schemaName);
			}				
			
			if (n_fk_column_name.equalsIgnoreCase("id")) { 
				directChildTables.add(n_table_name);
				if (!subObjectclasses.containsValue(n_table_name)) {
					// code-block for deleting ADE hook data 
					ref_hook_block += brDent2 + "-- delete " + n_table_name + "s"
									+ brDent2 + "IF pids IS NOT EMPTY THEN"
								 		+ brDent3 + "dummy_ids := " + getArrayDeleteFunctionName(n_table_name) + "(pids, 1);"
								 	+ brDent2 + "END IF;"
								 	+ br;
				}			 	 
			}
			else {											
				if (nRootRelation == RelationType.COMPOSITION) {		
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
				if (!functionCollection.containsKey(m_table_name)) {
					registerDeleteFunction(m_table_name, schemaName);
				}					

				RelationType mRootRelation = aggregationInfoCollection.getTableRelationType(m_table_name, rootTableName, n_table_name);					
				// In case of composition or aggregation between the root table and table m, the corresponding 
				// records in the tables n and m should be deleted using an explicit code-block created below 
				if (mRootRelation != RelationType.ASSOCIATION) {
					String varName = m_table_name + "_ids" + var_index.getAndIncrement();
					vars += brDent2 + varName +" ID_ARRAY := ID_ARRAY();";					
					ref_block += create_n_m_ref_delete(n_table_name, 
														n_fk_column_name, 
														m_table_name,
														m_fk_column_name,
														schemaName, 
														mRootRelation,
														varName);
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
				if (childTableName.equalsIgnoreCase(tableName) 
						|| querier.getAssociativeTables(schemaName).contains(childTableName)
						|| !tableExists(childTableName, schemaName))
					continue;
				
				int caller = 0;
				if (directChildTables.contains(childTableName))
					caller = 1;
				
				ref_child_block += br
						 + brDent5 + "-- delete " + childTableName						 	
						 + brDent5 + "IF objectclass_id = " + childObjectclassId + " THEN"	
					 	 	+ brDent6 + "dummy_ids := " + getArrayDeleteFunctionName(childTableName) + "(ID_ARRAY(object_id), " + caller + ");"
					 	 + brDent5 + "END IF;";
			}			
		}
		
		if (ref_child_block.length() > 0) {
			ref_child_block  = brDent2 + "IF caller <> 2 THEN"	
								 + brDent3 + "OPEN cur FOR" 
								 	+ brDent4 + "SELECT"
								 		+ brDent5 + "co.id, co.objectclass_id"
								 	+ brDent4 + "FROM" 
								 		+ brDent5 + "cityobject co, TABLE(pids) a" 
								 	+ brDent4 + "WHERE"
								 		+ brDent5 + "a.COLUMN_VALUE = co.id;"
								 + brDent3 + "LOOP"
								 	+ brDent4 + "FETCH cur into object_id, objectclass_id;"
									+ brDent4 + "EXIT WHEN cur%notfound;"  
									+ ref_child_block 
									+ br		
							 	 	+ brDent4 + "IF dummy_ids IS NOT EMPTY THEN"
										+ brDent5 + "IF dummy_ids(1) = object_id THEN"
											+ brDent6 + "deleted_child_ids := deleted_child_ids MULTISET UNION ALL dummy_ids;"
										+ brDent5 + "END IF;"						 	 			
						 	 		+ brDent4 + "END IF;"											
								 + brDent3 + "END LOOP;"
								 + brDent3 + "CLOSE cur;"
							 + brDent2 + "END IF;"
						 + brDent1;
		}
		
		ref_block += ref_hook_block	+ ref_child_block;

		String[] result = {vars, ref_block};
		return result; 
	}
	
	private String create_n_ref_delete(String tableName, String fk_column_name, String schemaName) {
		String code_block = "";
		code_block += brDent2 + "--delete " + tableName + "s"		
					+ brDent2 + "SELECT"
						+ brDent3 + "t.id"
					+ brDent2 + "BULK COLLECT INTO"
						+ brDent3 + "object_ids"		
					+ brDent2 + "FROM"
						+ brDent3 + tableName + " t,"
						+ brDent3 + "TABLE(pids) a"
					+ brDent2 + "WHERE"
						+ brDent3 + "t." + fk_column_name + " = a.COLUMN_VALUE;"
					+ br  // space line
					+ brDent2 + "IF object_ids IS NOT EMPTY THEN"
						+ brDent3 + "dummy_ids := " + getArrayDeleteFunctionName(tableName) + "(object_ids);"
					+ brDent2 + "END IF;" + br;	
		
		return code_block;
	}
	
	private String create_n_m_ref_delete(String n_m_table_name, String n_fk_column_name, String m_table_name, 
			String m_fk_column_name, String schemaName, RelationType tableRelation, String varName) throws SQLException {
		String code_block = "";
		code_block += brDent2 + "-- delete references to " + m_table_name + "s"
					+ brDent2 + "DELETE FROM"
						+ brDent3 + n_m_table_name + " t"
					+ brDent2 + "WHERE EXISTS ("
						+ brDent3 + "SELECT"
							+ brDent4 + "1"
						+ brDent3 + "FROM"
							+ brDent4 + "TABLE(pids) a"
						+ brDent3 + "WHERE"
							+ brDent4 + "a.COLUMN_VALUE = t." + n_fk_column_name
					+ brDent2 + ")"
					+ brDent2 + "RETURNING"
						+ brDent3 + m_fk_column_name
					+ brDent2 + "BULK COLLECT INTO"
						+ brDent3 + varName + ";" + br
				+ create_m_ref_delete(m_table_name, schemaName, tableRelation, varName);		
		
		return code_block;
	}
	
	private String create_m_ref_delete(String m_table_name, String schemaName, RelationType tableRelation, String varName) throws SQLException {	
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
			
		String code_block = "";		
		String join_block = "";
		String where_block = "";
		String tmp_block = "";
		
		code_block += brDent2 + "-- delete " + m_table_name + "(s)";		
		tmp_block = brDent2 + "IF " + varName + " IS NOT EMPTY THEN"
						+ brDent3 + "SELECT DISTINCT"
							+ brDent4 + "a.COLUMN_VALUE"
						+ brDent3 + "BULK COLLECT INTO"
							+ brDent4 + "object_ids"
						+ brDent3 + "FROM"
							+ brDent4 + "TABLE(" + varName + ") a";
		
		// In the case of composition, the sub-features shall be directly deleted 
		// without needing to check if they are referenced by another super features
		// In other cases (aggregation), this check is required.
		if (tableRelation != RelationType.COMPOSITION) {
			int index = 1;
			for (ReferencingEntry ref : aggComprefList) {				
				String refTable = ref.getRefTable();
				String refColumn = ref.getRefColumn();
				join_block += "LEFT JOIN"
								+ brDent4 + refTable + " n" + index
								+ brDent4 + "ON n" + index + "." + refColumn + "  = a.COLUMN_VALUE";			
				where_block += "n" + index + "." + refColumn + " IS NULL";
				if (index < aggComprefList.size()) {
					join_block += brDent3;
					where_block += brDent4 + "AND ";
				}				
				index++; 
			}
			tmp_block += brDent3 + join_block
						+ brDent3 + "WHERE " + where_block + ";";
		}
		else {
			tmp_block += ";";
		}
		
		if (join_block.length() > 0) {
			code_block += tmp_block + br
					 + brDent3 + "IF object_ids IS NOT EMPTY THEN"
						+ brDent4 + "dummy_ids := " + getArrayDeleteFunctionName(m_table_name) + "(object_ids);"
					 + brDent3 + "END IF;"
				  + brDent2 + "END IF;" + br;
		}
		else {
			code_block += 
					   brDent2 + "IF " + varName + " IS NOT EMPTY THEN"
						+ brDent3 + "dummy_ids := " + getArrayDeleteFunctionName(m_table_name) + "(" + varName + ");"
					 + brDent2 + "END IF;" + br;
		}

		return code_block;
	}
	
	private String[] create_ref_to_delete(String tableName, String schemaName, AtomicInteger var_index) throws SQLException {
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
				String mainVarName = ref_table_name + "_ids" + var_index.getAndIncrement();
				vars += brDent2 +  mainVarName + " ID_ARRAY := ID_ARRAY();";
				collect_block += brDent2 + "-- collect all " + ref_table_name + "ids into one nested table";
				for (int i = 0; i < fk_columns.length; i++) {
					String subVarName = ref_table_name + "_ids" + var_index.getAndIncrement();
					vars += brDent2 + subVarName + " ID_ARRAY := ID_ARRAY();";
					into_block += "," + brDent3 + subVarName;
					returning_block += "," + brDent3 + fk_columns[i];
					collect_block += brDent2 + mainVarName + " := " +  mainVarName + " MULTISET UNION ALL " +  subVarName + ";";			
				}
				collect_block += br;

				if (!functionCollection.containsKey(ref_table_name))
					registerDeleteFunction(ref_table_name, schemaName);
				
				// Check if we need add additional code-block for cleaning up the sub-features
				// for the case of aggregation relationship. 
				fk_block += this.create_m_ref_delete(ref_table_name, schemaName, defaultTableRelation, mainVarName);
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
				
				code_block += brDent2 + "IF caller <> 1 THEN"
						    	+ brDent3 + "-- delete " + parent_table
						    	+ brDent3 + "IF deleted_ids IS NOT EMPTY THEN"
						    		+ brDent4 + "dummy_ids := " + getArrayDeleteFunctionName(parent_table) + "(deleted_ids, 2);"
						    	+ brDent3 + "END IF;"
						    + brDent2 + "END IF;" + br;
			}			
		}
		return code_block;
	}
	
	private boolean tableExists(String tableName, String schemaName) throws SQLException {
		boolean exist = false;
		Statement stmt = null;
		ResultSet rs = null;
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select 1 from ALL_TABLES "
					 + "where TABLE_NAME = upper('" + tableName + "') "
					 + "and OWNER = upper('" + schemaName + "')");		
			if (rs.next()) 
				exist = true;	
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}

		return exist;
	}
}
