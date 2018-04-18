package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.postgis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencingEntry;
import org.citydb.plugins.ade_manager.registry.datatype.RelationType;
import org.citydb.plugins.ade_manager.registry.pkg.delete.adapter.AbstractDeleteScriptGenerator;

public class PostgisDeleteGeneratorGenerator extends AbstractDeleteScriptGenerator {

	public PostgisDeleteGeneratorGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	protected void generateDeleteScript(String initTableName, String schemaName) throws SQLException {
		try {
			registerFunction(initTableName, schemaName);
		} catch (SQLException e) {
			throw new SQLException("Failed to create generate delete script", e);
		}
	}
	
	@Override
	protected String constructDeleteFunction(String tableName, String schemaName) throws SQLException  {
		String delete_func_ddl =
				"CREATE OR REPLACE FUNCTION " + schemaName + "." + createFunctionName(tableName) + 
				"(int[], caller INTEGER DEFAULT 0) RETURNS SETOF int AS" + br + "$body$";
		
		String declare_block = 
				br +  "DECLARE" + 
				brDent1 + "deleted_ids int[] := '{}';";
		
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
		delete_block += create_local_delete(tableName);
		
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
		delete_func_ddl += declare_block + br + 
				"BEGIN" + pre_block +
				brDent1 + "-- delete " + tableName + "s" + 
				delete_agg_start + 
				delete_block + 
				delete_agg_end + 
				post_block + 
				return_block + br + 
				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";	

		return delete_func_ddl;
	}

	private String create_local_delete(String tableName) {
		String code_blcok = "";		
		code_blcok += brDent2 + "DELETE FROM"
						+ brDent3 + tableName + " t"
					+ brDent2 + "USING"
						+ brDent3 + "unnest($1) a(a_id)"
					+ brDent2 + "WHERE"
						+ brDent3 + "t.id = a.a_id"
					+ brDent2 + "RETURNING"
						+ brDent3 + "id";		
		return code_blcok;
	}
	
	private String create_selfref_delete(String tableName, String schemaName) throws SQLException {
		List<String> selfFkColumns = adeDatabaseSchemaManager.query_selfref_fk(tableName, schemaName);
		String code_block = "";
		for (String fkColumn : selfFkColumns) {			
			code_block += brDent1 + "-- delete referenced parts"
						+ brDent1 + "PERFORM"
							+ brDent2 + schemaName + "."+ createFunctionName(tableName) + "(array_agg(t.id))"
						+ brDent1 + "FROM"
							+ brDent2 + tableName + " t,"
							+ brDent2 + "unnest($1) a(a_id)"
						+ brDent1 + "WHERE"
							+ brDent2 + "t." + fkColumn + " = a.a_id"
							+ brDent2 + "AND t.id != a.a_id;" + br;
		}
		return code_block;
	}

	private String[] create_ref_delete(String tableName, String schemaName) throws SQLException {
		String vars = "";
		String ref_block = "";
		String ref_child_block = "";
		
		List<MnRefEntry> refEntries = adeDatabaseSchemaManager.query_ref_fk(tableName, schemaName);
		for (MnRefEntry ref : refEntries) {
			String rootTableName = ref.getRootTableName();			
			String n_table_name = ref.getnTableName();
			String n_fk_column_name = ref.getnFkColumnName();
			String m_table_name = ref.getmTableName();
			String m_fk_column_name = ref.getmFkColumnName();
			String m_ref_column_name = ref.getmRefColumnName();
			boolean n_column_is_not_null = ref.isnColIsNotNull();
			
			RelationType nRootRelation = checkTableRelationType(n_table_name, rootTableName);
			
			if (!functionCollection.containsKey(n_table_name) && m_table_name == null)
				registerFunction(n_table_name, schemaName);

			// PF = FK is the case e.g. ADE hook and inheritance relationships
			if (n_fk_column_name.equalsIgnoreCase("id")) { 						
				ref_child_block += brDent1 + "IF $2 <> 2 THEN"
								 	+ brDent2 + "-- delete " + n_table_name + "s"
								 	+ brDent2 + "PERFORM " + schemaName + "." + createFunctionName(n_table_name) + "($1, 1);"
								 	+ brDent1 + "END IF;" + br;
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
			if (n_column_is_not_null && m_table_name != null) {												
				if (!functionCollection.containsKey(m_table_name))
					registerFunction(m_table_name, schemaName);

				RelationType mRootRelation = checkTableRelationType(m_table_name, rootTableName);
				
				// In case of composition or aggregation between the root table and table m, the corresponding 
				// records in the tables n and m should be deleted using an explicit code-block created below 
				if (mRootRelation != RelationType.no_agg_comp) {
					vars += brDent1 + m_table_name + "_ids int[] := '{}';";
					ref_block += create_n_m_ref_delete(n_table_name, 
														n_fk_column_name, 
														m_table_name,
														m_fk_column_name,
														m_ref_column_name, 
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
		ref_block += ref_child_block;
		
		String[] result = {vars, ref_block};
		return result; 
	}

	private String create_n_ref_delete(String tableName, String fk_column_name, String schemaName) {
		String code_block = "";
		code_block += brDent1 + "--delete " + tableName + "s"		
					+ brDent1 + "PERFORM"
						+ brDent2 + schemaName + "." + createFunctionName(tableName) + "(array_agg(t.id))"
					+ brDent1 + "FROM"
						+ brDent2 + tableName + " t,"		
						+ brDent2 + "unnest($1) a(a_id)"
					+ brDent1 + "WHERE"
						+ brDent2 + "t." + fk_column_name + " = a.a_id;" + br;	
		
		return code_block;
	}
	
	private String create_n_m_ref_delete(String n_m_table_name, String n_fk_column_name, String m_table_name, 
			String m_fk_column_name, String m_ref_column_name, String schemaName, RelationType tableRelation) throws SQLException {
		String code_block = "";
		code_block += brDent1 + "-- delete references to " + m_table_name + "s"
					+ brDent1 + "WITH " + createFunctionName(m_table_name) + "_refs AS ("
						+ brDent2 + "DELETE FROM"
							+ brDent3 + n_m_table_name + " t"
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
				+ create_m_ref_delete(m_table_name, m_ref_column_name, schemaName, tableRelation);		
		
		return code_block;
	}
	
	private String create_m_ref_delete(String m_table_name, String m_ref_column_name, String schemaName, RelationType tableRelation) throws SQLException {
		List<ReferencingEntry> refList = adeDatabaseSchemaManager.query_ref_tables_and_columns(m_table_name, schemaName);
		List<ReferencingEntry> aggComprefList = new ArrayList<ReferencingEntry>(); 
		for (ReferencingEntry ref : refList) {				
			if (checkTableRelationType(ref.getRefTable(), m_table_name) == RelationType.no_agg_comp) {
				aggComprefList.add(ref);
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
							+ brDent3 + refTable + " n" + index
							+ brDent3 + "ON n" + index + "." + refColumn + "  = a.a_id";			
			where_block += "n" + index + "." + refColumn + " IS NULL";
			if (index < aggComprefList.size()) {
				join_block += brDent2;
				where_block += brDent3 + "AND ";
			}				
			index++; 
		}
		
		code_block += brDent1 + "-- delete " + m_table_name + "(s)"
					+ brDent1 + "IF -1 = ALL(" + m_table_name + "_ids) IS NOT NULL THEN"
						+ brDent2 + "PERFORM"
							+ brDent3 + schemaName + "." + createFunctionName(m_table_name) + "(array_agg(a.a_id))"
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

	private String create_ref_to_parent_delete(String tableName, String schemaName) throws SQLException {
		String parent_table = adeDatabaseSchemaManager.query_ref_to_parent_fk(tableName, schemaName);
		String code_block = "";
		if (parent_table != null) {
			if (!functionCollection.containsKey(parent_table))
				registerFunction(parent_table, schemaName);			
			code_block += brDent1 + "IF $2 <> 1 THEN"
					    	+ brDent2 + "-- delete " + parent_table
					    	+ brDent2 + "PERFORM " + schemaName + "." + createFunctionName(parent_table) + "(deleted_ids, 2);"
					    + brDent1 + "END IF;" + br;
		}
		return code_block;
	}

	private String[] create_ref_to_delete(String tableName, String schemaName) throws SQLException {
		String vars = "";
		String returning_block = "";
		String collect_block = "";
		String into_block = "";
		String fk_block = "";
		
		List<ReferencedEntry> refEntries = adeDatabaseSchemaManager.query_ref_to_fk(tableName, schemaName);
		
		for (ReferencedEntry entry : refEntries) {
			String ref_table_name = entry.getRefTable();
			String ref_column_name = entry.getRefColumn();
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
					registerFunction(ref_table_name, schemaName);
				
				// Check if we need add additional code-block for cleaning up the sub-features
				// for the case of aggregation relationship. 
				fk_block += this.create_m_ref_delete(ref_table_name, ref_column_name, schemaName, tableRelation);
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

}
