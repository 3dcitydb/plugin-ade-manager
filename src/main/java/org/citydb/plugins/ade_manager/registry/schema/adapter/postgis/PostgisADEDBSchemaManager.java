package org.citydb.plugins.ade_manager.registry.schema.adapter.postgis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencingEntry;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;

public class PostgisADEDBSchemaManager extends AbstractADEDBSchemaManager {
	
	public PostgisADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
	}
	
	public List<String> query_selfref_fk(String tableName, String schemaName) throws SQLException {
		List<String> result = new ArrayList<String>();		
		PreparedStatement pstsmt = null;
		ResultSet rs = null;

		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT a.attname ")
				  .append("FROM pg_constraint c ")
				  .append("JOIN pg_attribute a ")
				      .append("ON a.attrelid = c.conrelid ")
				      .append("AND a.attnum = ANY (c.conkey) ")
				  .append("WHERE c.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND c.conrelid = c.confrelid ")
				      .append("AND c.contype = 'f'");
	
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
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
		}
		
		return result;
	}
	
	public List<MnRefEntry> query_ref_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
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
				  	  	           .append("confrelid::regclass::text = '" + tableName + "' ")
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
				  	       .append("c.confrelid::regclass::text = '" + tableName + "' ")
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
					     .append("AND mn.confrelid::regclass::text <> '"+ tableName + "' ")
					     .append("AND pk.contype = 'p'")
					 .append(") m ON (true) ")
					 .append("ORDER BY ")
					 	.append("ref.ref_depth DESC NULLS FIRST, ")
					 	.append("ref.n_table_name, ")
					 	.append("m.m_table_name");

		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
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
		}
		
		return result;
	}

	public List<ReferencingEntry> query_ref_tables_and_columns(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
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
				      .append("c.confrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND a.attname::text <> 'id' ")
				      .append("AND c.contype = 'f'");
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();			
			
			while (rs.next()) {
				String refTable = rs.getString(1);
				String refColumn = rs.getString(2);
				result.add(new ReferencingEntry(refTable, refColumn));				
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
		}
		return result;
	}
	
	public String query_ref_to_parent_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		String result = null;
		
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
				      .append("f.confrelid::regclass::text AS parent_table ")
				  .append("FROM ")
				      .append("pg_constraint f, ")
				      .append("pg_constraint p ")
				  .append("WHERE ")
				      .append("f.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND p.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND f.conkey = p.conkey ")
				      .append("AND f.contype = 'f' ")
				      .append("AND p.contype = 'p'");

		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
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
		}

		return result;
	}
	
	public List<ReferencedEntry> query_ref_to_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
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
				      .append("c.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND c.conrelid <> c.confrelid ")
				      .append("AND c.contype = 'f' ")
				      .append("AND c.confrelid::regclass::text <> 'cityobject' ")
				  .append("GROUP BY ")
				      .append("c.confrelid, ")
				      .append("a_ref.attname");
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
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
		}
		
		return result;
	}

	@Override
	protected String processScript(String inputScript) {
		return inputScript;
	}

	@Override
	protected void dropCurrentDeleteFunctions() throws SQLException {
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		Map<String, String> deleteFunctions = queryDeleteFunctions(schema);
		for (String funcName: deleteFunctions.keySet()) {
			String funcDeclaration = deleteFunctions.get(funcName);
			PreparedStatement pstsmt = null;
			try {
				pstsmt = connection.prepareStatement("DROP FUNCTION IF EXISTS " + schema + "." + funcDeclaration);
				pstsmt.executeUpdate();		
				LOG.info("DB-function '" + funcName + "' successfully dropped");
			} 
			finally {			
				if (pstsmt != null) { 
					try {
						pstsmt.close();
					} catch (SQLException e) {
						throw e;
					} 
				}	
			}
		}		
	}
	
	private Map<String, String> queryDeleteFunctions(String schema) throws SQLException{
		Map<String, String> funcNames = new HashMap<String, String>();
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
				
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT routines.routine_name, routines.data_type, parameters.data_type, parameters.ordinal_position ")
				      .append("FROM information_schema.routines ")
				      .append("LEFT JOIN information_schema.parameters ON routines.specific_name=parameters.specific_name ")
				      .append("WHERE routines.specific_schema= '").append(schema).append("' and routines.routine_name like 'delete_%' ")
				      .append("ORDER BY routines.routine_name, parameters.ordinal_position");
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();						
			while (rs.next()) {
				String funcName = rs.getString(1);
				String instinctParamType = rs.getString(2);
				String paramType = rs.getString(3);
				if (paramType.equalsIgnoreCase("array"))
					paramType = instinctParamType + "[]";
				
				String funcDeclaration = null;
				if (!funcNames.containsKey(funcName)) {
					funcDeclaration = funcName + "(" + paramType;
				} else {
					funcDeclaration = funcNames.get(funcName) + "," + paramType;					
				}
				funcNames.put(funcName, funcDeclaration);	
			}	
			for (String key: funcNames.keySet()) {
				funcNames.put(key, funcNames.get(key) + ")");
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
		}
		
		return funcNames;
	}
}
