package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.oracle;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.AbstractDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.query.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencingEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.RelationType;

public class OracleDeleteScriptGenerator extends AbstractDeleteScriptGenerator {
	private final String SCRIPT_DELIMITER = "---DELIMITER---";
	/** -- SQL-Script for Tests
	 * declare
		   dummy_ids ID_ARRAY;
		   object_id number;
		   cur sys_refcursor;
		begin
		  open cur for 'select id from cityobject where objectclass_id = 26';
		  loop
		    fetch cur into object_id;
		    exit when cur%notfound;
		    begin
		       -- Call the function
		       dummy_ids := citydb_delete.del_cityobject (ID_ARRAY(object_id));
		       dbms_output.put_line('cityobject with ID ' || object_id || ' deleted!');
		    exception
		      when others then
		        dbms_output.put_line('Error occurred while deleting cityobject with ID ' || object_id || ' threw: ' || SQLERRM);
		    end;
		  end loop;
		end; 
	 * **/
	public OracleDeleteScriptGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	public void installDeleteScript(String scriptString) throws SQLException{
		String[] splitStr = scriptString.replaceAll("\\/", "").split(SCRIPT_DELIMITER);
		String pkgHeader = splitStr[0];
		String pkgBody = splitStr[1];
		Statement headerStmt = null;
		Statement bodyStmt = null;
		try {
			headerStmt = connection.createStatement();
			headerStmt.execute(pkgHeader);
			bodyStmt = connection.createStatement();
			bodyStmt.execute(pkgBody);
		} catch (SQLException e) {
			throw new SQLException(e);
		} finally {
			if (headerStmt != null)
				headerStmt.close();
			if (bodyStmt != null)
				bodyStmt.close();
		}
	}
	
	@Override
	protected String constructDeleteFunction(String tableName, String schemaName) throws SQLException {
		AtomicInteger var_index = new AtomicInteger(0);
		
		String delete_func_ddl =
				dent + "FUNCTION " + createFunctionName(tableName) + "(pids ID_ARRAY, caller int := 0) RETURN ID_ARRAY " + 
				brDent1 + "IS";
		
		String declare_block = 
					brDent2 + "object_id number;" +
					brDent2 + "objectclass_id number;" +			
					brDent2 + "object_ids ID_ARRAY := ID_ARRAY();" +		
					brDent2 + "deleted_ids ID_ARRAY := ID_ARRAY();" +							
					brDent2 + "dummy_ids ID_ARRAY := ID_ARRAY();";
		
		String pre_block = "";
		String post_block = "";
		String delete_block = "";	
		
		String delete_into_block = 
					brDent2 + "BULK COLLECT INTO"  
					  + brDent3 +  "deleted_ids";
		
		String return_block = 
					brDent2 + "RETURN deleted_ids;" + br;
		
		// Code-block for deleting self-references in case of e.g. building/buildingParts
		if (checkTableRelationType(tableName, tableName) == RelationType.composition) {
			pre_block += this.create_selfref_delete(tableName, schemaName);
		}
				
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
		
		String exception_block = 
					brDent2 + "EXCEPTION" + 
							brDent3 + "WHEN NO_DATA_FOUND THEN" + 	
								brDent4 + "RETURN deleted_ids;";	
				
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
					exception_block +
					brDent1 + "END;";	

		return delete_func_ddl;
	}

	@Override
	protected void printDDLForAllDeleteFunctions(PrintStream writer) {
		// package header
		String script = 					
				"CREATE OR REPLACE PACKAGE citydb_delete" + br +
				"AS";
		
		for (String tableName: functionCollection.keySet()) {
			if (tableName.equalsIgnoreCase(lineage_delete_funcname))
				script += brDent1 + "FUNCTION " + functionNames.get(tableName) + "(lineage_value varchar2, objectclass_id int := 0) RETURN ID_ARRAY;";
			else {
				script += brDent1 + "FUNCTION " + functionNames.get(tableName) + "(pids ID_ARRAY, caller int := 0) RETURN ID_ARRAY;";
			}	
		};
		script += br 
			   + "END citydb_delete;"
			   + br 
			   + "/"
			   + br
			   + SCRIPT_DELIMITER 
			   + br + br;
		
		// package body	
		script += "CREATE OR REPLACE PACKAGE BODY citydb_delete" + br +
				  "AS " + br;
		
		for (String tableName: functionCollection.keySet()) {
			String functionBody = functionCollection.get(tableName);
			script += functionBody + 
					brDent1 + "------------------------------------------" + br + br;
		};	
		script += "END citydb_delete;"
				+ br 
				+ "/";
		writer.println(script);
	}
	
	@Override
	protected String constructLineageDeleteFunction(String schemaName) {
		String delete_func_ddl = "";
		delete_func_ddl += dent +  
				"FUNCTION " + lineage_delete_funcname + "(lineage_value varchar2, objectclass_id int := 0) RETURN ID_ARRAY " + 
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
							brDent4 + "dummy_ids := del_cityobject(ID_ARRAY(deleted_ids(i)), 1);" + 
						brDent3 + "END LOOP;" +
					brDent2 + "END IF;" + 
					br + 
					brDent2 + "RETURN deleted_ids;" + 
					br + 					
					brDent2 + "EXCEPTION" + 
							brDent3 + "WHEN NO_DATA_FOUND THEN" + 
								brDent4 + "RETURN deleted_ids;" + 
				brDent1 + "END;";
		
		return delete_func_ddl;
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
							+ brDent3 + "AND t.id != a.COLUMN_VALUE;" 
						+ br  // space line
						+ brDent2 + "IF object_ids IS NOT EMPTY THEN"
							+ brDent3 + "dummy_ids := " + createFunctionName(tableName) + "(object_ids);"
						+ brDent2 + "END IF;" + br;	
		}
		
		return self_block; 
	}
	
	private String[] create_ref_delete(String tableName, String schemaName, AtomicInteger var_index) throws SQLException {
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

			if (!functionCollection.containsKey(n_table_name) && m_table_name == null) {
				registerFunction(n_table_name, schemaName);
			}				
			
			if (n_fk_column_name.equalsIgnoreCase("id")) { 
				directChildTables.add(n_table_name);
				if (!subObjectclasses.containsValue(n_table_name)) {
					// code-block for deleting ADE hook data 
					ref_hook_block += brDent2 + "-- delete " + n_table_name + "s"
									+ brDent2 + "IF pids IS NOT EMPTY THEN"
								 		+ brDent3 + "dummy_ids := " + createFunctionName(n_table_name) + "(pids, 1);"
								 	+ brDent2 + "END IF;"
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
				if (!functionCollection.containsKey(m_table_name)) {
					registerFunction(m_table_name, schemaName);
				}					

				RelationType mRootRelation = checkTableRelationType(m_table_name, rootTableName);
				
				// In case of composition or aggregation between the root table and table m, the corresponding 
				// records in the tables n and m should be deleted using an explicit code-block created below 
				if (mRootRelation != RelationType.no_agg_comp) {
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
						|| !tableExists(childTableName))
					continue;
				
				int caller = 0;
				if (directChildTables.contains(childTableName))
					caller = 1;
				
				ref_child_block += br
						 + brDent5 + "-- delete " + childTableName						 	
						 + brDent5 + "IF objectclass_id = " + childObjectclassId + " THEN"	
					 	 	+ brDent6 + "dummy_ids := " + createFunctionName(childTableName) + "(ID_ARRAY(object_id), " + caller + ");"
						 + brDent5 + "END IF;";
			}			
		}
		
		if (ref_child_block.length() > 0) {
			ref_child_block  = brDent2 + "IF caller <> 2 THEN"	
								 + brDent3 + "FOR i in 1..pids.count"
								 + brDent3 + "LOOP"
								 	+ brDent4 + "object_id := pids(i);"
									+ brDent4 + "EXECUTE IMMEDIATE " +  "'SELECT objectclass_id FROM " + tableName + " WHERE id = :1' "  
								    		  + "INTO objectclass_id USING object_id;"
									+ ref_child_block 
								 + brDent3 + "END LOOP;"
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
						+ brDent3 + "dummy_ids := " + createFunctionName(tableName) + "(object_ids);"
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
							+ brDent4 + refTable + " n" + index
							+ brDent4 + "ON n" + index + "." + refColumn + "  = a.COLUMN_VALUE";			
			where_block += "n" + index + "." + refColumn + " IS NULL";
			if (index < aggComprefList.size()) {
				join_block += brDent3;
				where_block += brDent4 + "AND ";
			}				
			index++; 
		}
		
		code_block += brDent2 + "-- delete " + m_table_name + "(s)"
					+ brDent2 + "IF " + varName + " IS NOT EMPTY THEN"
						+ brDent3 + "SELECT DISTINCT"
							+ brDent4 + "a.COLUMN_VALUE"
						+ brDent3 + "BULK COLLECT INTO"
							+ brDent4 + "object_ids"
						+ brDent3 + "FROM"
							+ brDent4 + "TABLE(" + varName + ") a";
		
		// In the case of composition, the sub-features shall be directly deleted 
		// without needing to check if they are referenced by another super features
		// In other cases (aggregation), this check is required.
		if (tableRelation != RelationType.composition) {
			code_block += brDent3 + join_block
						+ brDent3 + "WHERE " + where_block + ";";
		}
		else {
			code_block += ";";
		}
		
		code_block  += br
				 + brDent3 + "IF object_ids IS NOT EMPTY THEN"
					+ brDent4 + "dummy_ids := " + createFunctionName(m_table_name) + "(object_ids);"
				 + brDent3 + "END IF;"
			  + brDent2 + "END IF;" + br;

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

			RelationType tableRelation = checkTableRelationType(ref_table_name, tableName);
			
			// Exclude the case of normal associations for which the referenced features should be not be deleted. 
			if (tableRelation != RelationType.no_agg_comp) {
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
					registerFunction(ref_table_name, schemaName);
				
				// Check if we need add additional code-block for cleaning up the sub-features
				// for the case of aggregation relationship. 
				fk_block += this.create_m_ref_delete(ref_table_name, schemaName, tableRelation, mainVarName);
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
					registerFunction(parent_table, schemaName);
				
				code_block += brDent2 + "IF caller <> 1 THEN"
						    	+ brDent3 + "-- delete " + parent_table
						    	+ brDent3 + "IF deleted_ids IS NOT EMPTY THEN"
						    		+ brDent4 + "dummy_ids := " + createFunctionName(parent_table) + "(deleted_ids, 2);"
						    	+ brDent3 + "END IF;"
						    + brDent2 + "END IF;" + br;
			}			
		}
		return code_block;
	}
	
	private boolean tableExists(String tableName) throws SQLException {
		boolean exist = false;
		Statement stmt = null;
		ResultSet rs = null;
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select 1 from all_tables where table_name = upper('" + tableName + "')");		
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
