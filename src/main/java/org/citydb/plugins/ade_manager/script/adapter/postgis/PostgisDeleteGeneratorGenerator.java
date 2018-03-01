package org.citydb.plugins.ade_manager.script.adapter.postgis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.plugins.ade_manager.script.DsgException;
import org.citydb.plugins.ade_manager.script.RelationType;
import org.citydb.plugins.ade_manager.script.adapter.AbstractDeleteScriptGenerator;

public class PostgisDeleteGeneratorGenerator extends AbstractDeleteScriptGenerator {

	@Override
	protected void generateDeleteFuncs(String initTableName, String schemaName) throws DsgException {
		try {
			createDeleteFunc(initTableName, schemaName);
		} catch (SQLException e) {
			throw new DsgException ("Failed to create generate delete functions from the database", e);
		}
	}
	
	@Override
	protected String create_delete_function(String tableName, String schemaName) throws SQLException  {
		String delete_func_ddl =
				"CREATE OR REPLACE FUNCTION " + schemaName + "." + getFuncName(tableName) + 
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
		if (checkTableRelation(tableName, tableName) == RelationType.composition) {
			pre_block = this.create_selfref_delete(tableName, schemaName);
		}
				
		// Code-block for deleting referenced sub-features with aggregation/composition or inheritance relationship
		String[] result = create_ref_delete(tableName, schemaName);
		declare_block += result[0];
		pre_block += result[1];
	
		// Main Delete for the current table
		delete_block += create_local_delete(tableName);
		
		// Code-block for deleting referenced tables with N:0..1 aggregation, composition 
		// or normal association. e.g. the composition relationship between building and surface geometry,
		// the aggregation relationship between a feature and its referenced implicit geometry	
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
		List<String> selfFkColumns = query_selfref_fk(tableName, schemaName);
		String code_block = "";
		for (String fkColumn : selfFkColumns) {			
			code_block += brDent1 + "-- delete referenced parts"
						+ brDent1 + "PERFORM"
							+ brDent2 + schemaName + "."+ getFuncName(tableName) + "(array_agg(t.id))"
						+ brDent1 + "FROM"
							+ brDent2 + tableName + " t,"
							+ brDent2 + "unnest($1) a(a_id)"
						+ brDent1 + "WHERE"
							+ brDent2 + "t." + fkColumn + " = a.a_id"
							+ brDent2 + "AND t.id != a.a_id;" + br;
		}
		return code_block;
	}
	
	private List<String> query_selfref_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Connection conn = null;
		List<String> result = new ArrayList<>();
		
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT a.attname ")
				  .append("FROM pg_constraint c ")
				  .append("JOIN pg_attribute a ")
				      .append("ON a.attrelid = c.conrelid ")
				      .append("AND a.attnum = ANY (c.conkey) ")
				  .append("WHERE c.conrelid = ('").append(schemaName).append(".").append(tableName).append("')::regclass::oid ")
				      .append("AND c.conrelid = c.confrelid ")
				      .append("AND c.contype = 'f'");
	
		try {
			conn = dbPool.getConnection();
			pstsmt = conn.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();			
			
			while (rs.next()) {
				result.add(rs.getString(1));
			}				
		} 
		finally {			
			if (rs != null) { 
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}
			}	
			if (pstsmt != null) { 
				try {
					pstsmt.close();
				} catch (SQLException e) {
					throw e;
				} 
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					throw e;
				}
			}
		}
		
		return result;
	}

	private String[] create_ref_delete(String tableName, String schemaName) throws SQLException {
		String vars = "";
		String ref_block = "";
		String ref_child_block = "";
		
		List<MnRefEntry> refEntries = query_ref_fk(tableName, schemaName);
		for (MnRefEntry ref : refEntries) {
			String rootTableName = ref.getRootTableName();			
			String n_table_name = ref.getnTableName();
			String n_fk_column_name = ref.getnFkColumnName();
			String n_fk_name = ref.getnFkName();
			String m_table_name = ref.getmTableName();
			String m_fk_column_name = ref.getmFkColumnName();
			String m_ref_column_name = ref.getmRefColumnName();
			boolean n_column_is_not_null = ref.isnColIsNotNull();
			
			RelationType nRootRelation = checkTableRelation(n_table_name, rootTableName);
			
			if (!deleteFuncDefs.containsKey(n_table_name) && m_table_name == null)
				createDeleteFunc(n_table_name, schemaName);

			// PF = FK case e.g. ADE hook and inheritance relationships
			if (n_fk_column_name.equalsIgnoreCase("id")) { 						
				ref_child_block += brDent1 + "IF $2 <> 2 THEN"
								 	+ brDent2 + "-- delete " + n_table_name + "s"
								 	+ brDent2 + "PERFORM " + schemaName + "." + getFuncName(n_table_name) + "($1, 1);"
								 	+ brDent1 + "END IF;" + br;
			}
			else {											
				if (nRootRelation == RelationType.composition) {							
					ref_block += create_n_ref_delete(n_table_name, n_fk_column_name, schemaName);
				}		
				else {
					// in this case, the table n is an associative table for M:N relation
					if (!n_column_is_not_null) { 
						updateConstraintsSql += "select citydb_pkg.update_table_constraint('"
								+ n_fk_name + "', '"
								+ n_table_name + "', '"
								+ n_fk_column_name + "', '"
								+ rootTableName + "', '"
								+ "id', '"
								+ "NOT NULL', '"
								+ schemaName + "');" + br;
					}											
				}
			}	
			
			if (m_table_name != null) {												
				if (!deleteFuncDefs.containsKey(m_table_name))
					createDeleteFunc(m_table_name, schemaName);

				RelationType mRootRelation = checkTableRelation(m_table_name, rootTableName);

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
				else {
					updateConstraintsSql += "select citydb_pkg.update_table_constraint('"
							+ n_fk_name + "', '"
							+ n_table_name + "', '"
							+ n_fk_column_name + "', '"
							+ rootTableName + "', '"
							+ "id', '"
							+ "CASCADE', '"
							+ schemaName + "');" + br;
				}
			}
		} 
		ref_block += ref_child_block;
		
		String[] result = {vars, ref_block};
		return result; 
	}

	private List<MnRefEntry> query_ref_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Connection conn = null;
		List<MnRefEntry> result = new ArrayList<MnRefEntry>();
		
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("select ")
				  	  .append("ref.root_table_name, ")
				  	  .append("ref.n_table_name::regclass::text, ")
				  	  .append("ref.n_fk_column_name, ")
				  	  .append("ref.n_fk_name, ")
				  	  .append("ref.n_column_is_not_null, ")
				  	  .append("m.m_table_name::regclass::text, ")
				  	  .append("m.m_fk_column_name::text, ")
				  	  .append("m.m_fk_name::text, ")
				  	  .append("m.m_ref_column_name::text ")
				  .append("FROM (")
				  	  .append("SELECT ")
				  	 	  .append("c.confrelid::regclass::text AS root_table_name, ")
				  	 	  .append("c.conrelid AS n_table_name, ")
				  	 	  .append("c.conkey, ")
				  	 	  .append("c.conname AS n_fk_name, ")
				  	 	  .append("a.attnotnull AS n_column_is_not_null, ")
				  	 	  .append("a.attname::text AS n_fk_column_name, ")
				  	 	  .append("COALESCE(n.ref_depth, 1) AS ref_depth ")
				  	  .append("FROM ")
				  	  	  .append("pg_constraint c ")
				  	  .append("JOIN ")
				  	  	  .append("pg_attribute a ")
				  	  	  .append("ON a.attrelid = c.conrelid ")
				  	  	  .append("AND a.attnum = ANY (c.conkey)")
				  	  .append("LEFT JOIN (")
				  	  	   .append("WITH RECURSIVE ref_table_depth(parent_table, ref_table, depth) AS (")
				  	  	       .append("SELECT ")
				  	  	           .append("confrelid AS parent_table, ")
				  	  	           .append("conrelid AS ref_table, ")
				  	  	           .append("1 AS depth ")
				  	  	       .append("FROM ")
				  	  	           .append("pg_constraint ")
				  	  	       .append("WHERE ")
				  	  	           .append("confrelid = ('" + schemaName + "." + tableName + "')::regclass::oid ")
				  	  	           .append("AND conrelid <> confrelid ")
				  	  	           .append("AND contype = 'f' ")
				  	  	       .append("UNION ALL ")
				  	  	           .append("SELECT ")
				  	  	               .append("r.confrelid AS parent_table, ")
				  	  	               .append("r.conrelid AS ref_table, ")
				  	  	               .append("d.depth + 1 AS depth ")
				  	  	           .append("FROM ")
				  	  	               .append("pg_constraint r, ")
				  	  	               .append("ref_table_depth d ")
				  	  	           .append("WHERE ")
				  	  	               .append("d.ref_table = r.confrelid ")
				  	  	               .append("AND d.ref_table <> r.conrelid ")
				  	  	               .append("AND r.contype = 'f' ")	
				  	  	   .append(") ")
				  	  	   .append("SELECT ")
				  	  	       .append("parent_table, ")
				  	  	       .append("max(depth) AS ref_depth ")
				  	  	   .append("FROM ")
				  	  	       .append("ref_table_depth ")
				  	  	   .append("GROUP BY ")
				  	  	       .append("parent_table")
				  	  	   .append(") n ")
				  	  	   .append("ON n.parent_table = c.conrelid ")
				  	  .append("WHERE ")
				  	       .append("c.confrelid = ('" + schemaName + "." + tableName + "')::regclass::oid ")
				  	       .append("AND c.conrelid <> c.confrelid ")
				  	       .append("AND c.contype = 'f' ")
				 .append(") ref ")
				 .append("LEFT JOIN LATERAL (")
				     .append("SELECT ")
					     .append("mn.confrelid AS m_table_name, ")
					     .append("mn.conname AS m_fk_name, ")
					     .append("mna.attname AS m_fk_column_name, ")
					     .append("mna_ref.attname AS m_ref_column_name ")
					 .append("FROM ")
					     .append("pg_constraint mn ")
					 .append("JOIN ")
					     .append("pg_attribute mna ")
					     .append("ON mna.attrelid = mn.conrelid ")
					     .append("AND mna.attnum = ANY (mn.conkey) ")
					 .append("JOIN ")
					     .append("pg_attribute mna_ref ")
					     .append("ON mna_ref.attrelid = mn.confrelid ")
					     .append("AND mna_ref.attnum = ANY (mn.confkey) ")
					 .append("JOIN ")
					     .append("pg_constraint pk ")
					     .append("ON pk.conrelid = mn.conrelid ")
					     .append("AND pk.conkey @> (ref.conkey || mn.conkey || '{}') ")
					 .append("WHERE ")
					     .append("mn.conrelid = ref.n_table_name ")
					     .append("AND mn.confrelid <> ref.n_table_name ")
					     .append("AND mn.contype = 'f' ")
					     .append("AND mn.confrelid <> ('"+ schemaName + "." + tableName + "')::regclass::oid ")
					     .append("AND pk.contype = 'p'")
					 .append(") m ON (true) ")
					 .append("ORDER BY ")
					 	.append("ref.ref_depth DESC NULLS FIRST, ")
					 	.append("ref.n_table_name, ")
					 	.append("m.m_table_name");

		try {
			conn = dbPool.getConnection();
			pstsmt = conn.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();			
			while (rs.next()) {
				MnRefEntry refEntry = new MnRefEntry();
				refEntry.setRootTableName(rs.getString(1));
				refEntry.setnTableName(rs.getString(2));
				refEntry.setnFkColumnName(rs.getString(3));
				refEntry.setnFkName(rs.getString(4));
				refEntry.setnColIsNotNull(rs.getBoolean(5));
				refEntry.setmTableName(rs.getString(6));
				refEntry.setmFkColumnName(rs.getString(7));
				refEntry.setmFkName(rs.getString(8));
				refEntry.setmRefColumnName(rs.getString(9));
				result.add(refEntry);		
			}				
		} 
		finally {			
			if (rs != null) { 
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}
			}	
			if (pstsmt != null) { 
				try {
					pstsmt.close();
				} catch (SQLException e) {
					throw e;
				} 
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					throw e;
				}
			}
		}
		
		return result;
	}

	private String create_n_ref_delete(String tableName, String fk_column_name, String schemaName) {
		String code_block = "";
		code_block += brDent1 + "--delete " + tableName + "s"		
					+ brDent1 + "PERFORM"
						+ brDent2 + schemaName + "." + getFuncName(tableName) + "(array_agg(t.id))"
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
					+ brDent1 + "WITH " + getFuncName(m_table_name) + "_refs AS ("
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
						+ brDent2 + getFuncName(m_table_name) + "_refs;" + br
				+ create_m_ref_delete(m_table_name, m_ref_column_name, schemaName, tableRelation);		
		
		return code_block;
	}
	
	private String create_m_ref_delete(String m_table_name, String m_ref_column_name, String schemaName, RelationType tableRelation) throws SQLException {
		List<ReferencingEntry> refList = this.query_ref_tables_and_columns(m_table_name, schemaName);
		String code_block = "";
		
		String join_block = "";
		String where_block = "";
		int index = 1;
		for (ReferencingEntry ref : refList) {			
			String refTable = ref.getRefTable();
			String refColumn = ref.getRefColumn();
			join_block += "LEFT JOIN"
							+ brDent3 + refTable + " n" + index
							+ brDent3 + "ON n" + index + "." + refColumn + "  = a.a_id";			
			where_block += "n" + index + "." + refColumn + " IS NULL";
			if (index < refList.size()) {
				join_block += brDent2;
				where_block += brDent3 + "AND ";
			}				
			index++; 
		}
		
		code_block += brDent1 + "-- delete " + m_table_name + "(s)"
					+ brDent1 + "IF -1 = ALL(" + m_table_name + "_ids) IS NOT NULL THEN"
						+ brDent2 + "PERFORM"
							+ brDent3 + schemaName + "." + getFuncName(m_table_name) + "(array_agg(a.a_id))"
						+ brDent2 + "FROM"
							+ brDent3 + "(SELECT DISTINCT unnest(" + m_table_name + "_ids) AS a_id) a";
		
		// In the case of composition, the sub-features shall be directly deleted 
		// without needing to check if they are referenced by another super features
		// In other cases (aggregation or normal association), this check is required.
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
	
	private List<ReferencingEntry> query_ref_tables_and_columns(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Connection conn = null;
		
		List<ReferencingEntry> result = new ArrayList<ReferencingEntry>();
		
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
				      .append("c.conrelid::regclass::text, ")
				      .append("a.attname::text ")
				  .append("FROM ")
				      .append("pg_constraint c ")
				  .append("JOIN ")
				      .append("pg_attribute a ")
				      .append("ON a.attrelid = c.conrelid ")
				      .append("AND a.attnum = ANY (c.conkey) ")
				  .append("WHERE ")
				      .append("c.confrelid = ('").append(schemaName).append(".").append(tableName).append("')::regclass::oid ")
				      .append("AND a.attname::text <> 'id' ")
				      .append("AND c.contype = 'f'");
		try {
			conn = dbPool.getConnection();
			pstsmt = conn.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();			
			
			while (rs.next()) {
				String refTable = rs.getString(1);
				String refColumn = rs.getString(2);
				if (checkTableRelation(refTable, tableName) == RelationType.no_agg_comp) {
					result.add(new ReferencingEntry(refTable, refColumn));
				}				
			}				
		} 
		finally {			
			if (rs != null) { 
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}
			}	
			if (pstsmt != null) { 
				try {
					pstsmt.close();
				} catch (SQLException e) {
					throw e;
				} 
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					throw e;
				}
			}
		}
		return result;
	}
	
	private String create_ref_to_parent_delete(String tableName, String schemaName) throws SQLException {
		String parent_table = query_ref_to_parent_fk(tableName, schemaName);
		String code_block = "";
		if (parent_table != null) {
			if (!deleteFuncDefs.containsKey(parent_table))
				createDeleteFunc(parent_table, schemaName);			
			code_block += brDent1 + "IF $2 <> 1 THEN"
					    	+ brDent2 + "-- delete " + parent_table
					    	+ brDent2 + "PERFORM " + schemaName + "." + getFuncName(parent_table) + "(deleted_ids, 2);"
					    + brDent1 + "END IF;" + br;
		}
		return code_block;
	}
	
	private String query_ref_to_parent_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Connection conn = null;
		String result = null;
		
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
				      .append("f.confrelid::regclass::text AS parent_table ")
				  .append("FROM ")
				      .append("pg_constraint f, ")
				      .append("pg_constraint p ")
				  .append("WHERE ")
				      .append("f.conrelid = ('").append(schemaName).append(".").append(tableName).append("')::regclass::oid ")
				      .append("AND p.conrelid = ('").append(schemaName).append(".").append(tableName).append("')::regclass::oid ")
				      .append("AND f.conkey = p.conkey ")
				      .append("AND f.contype = 'f' ")
				      .append("AND p.contype = 'p'");

		try {
			conn = dbPool.getConnection();
			pstsmt = conn.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();
						
			if (rs.next())
				result = rs.getString(1);				
		} 
		finally {			
			if (rs != null) { 
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}
			}	
			if (pstsmt != null) { 
				try {
					pstsmt.close();
				} catch (SQLException e) {
					throw e;
				} 
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					throw e;
				}
			}
		}

		return result;
	}
	
	private String[] create_ref_to_delete(String tableName, String schemaName) throws SQLException {
		String vars = "";
		String returning_block = "";
		String collect_block = "";
		String into_block = "";
		String fk_block = "";
		List<ReferencedEntry> refEntries = query_ref_to_fk(tableName, schemaName);
		
		for (ReferencedEntry entry : refEntries) {
			String ref_table_name = entry.getRefTable();
			String ref_column_name = entry.getRefColumn();
			String[] fk_columns = entry.getFkColumns();

			RelationType tableRelation = checkTableRelation(ref_table_name, tableName);
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
				
				if (!deleteFuncDefs.containsKey(ref_table_name))
					createDeleteFunc(ref_table_name, schemaName);
				
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
	
	private List<ReferencedEntry> query_ref_to_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Connection conn = null;
		List<ReferencedEntry> result = new ArrayList<ReferencedEntry>();
		
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
				      .append("c.confrelid::regclass::text AS ref_table_name, ")
				      .append("a_ref.attname::text AS ref_column_name, ")
				      .append("array_agg(a.attname::text ORDER BY a.attnum) AS fk_columns ")
				  .append("FROM ")
				      .append("pg_constraint c ")
				  .append("JOIN ")
				      .append("pg_attribute a ")
				      .append("ON a.attrelid = c.conrelid ")
				      .append("AND a.attnum = ANY (c.conkey) ")
				  .append("JOIN ")
				      .append("pg_attribute a_ref ")
				      .append("ON a_ref.attrelid = c.confrelid ")
				      .append("AND a_ref.attnum = ANY (c.confkey) ")
				  .append("WHERE ")
				      .append("c.conrelid = ('").append(schemaName).append(".").append(tableName).append("')::regclass::oid ")
				      .append("AND c.conrelid <> c.confrelid ")
				      .append("AND c.contype = 'f' ")
				      .append("AND c.confrelid::regclass::text NOT LIKE '%cityobject' ")
				  .append("GROUP BY ")
				      .append("c.confrelid, ")
				      .append("a_ref.attname");
		try {
			conn = dbPool.getConnection();
			pstsmt = conn.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();
						
			while (rs.next()) {
				String refTable = rs.getString(1);
				String refColumn = rs.getString(2);				
				String[] fkColumns = (String[])rs.getArray(3).getArray();
				result.add(new ReferencedEntry(refTable, refColumn, fkColumns));
			}							
		} 
		finally {			
			if (rs != null) { 
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}
			}	
			if (pstsmt != null) { 
				try {
					pstsmt.close();
				} catch (SQLException e) {
					throw e;
				} 
			}			
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					throw e;
				}
			}
		}
		
		return result;
	}
}
