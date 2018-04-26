package org.citydb.plugins.ade_manager.registry.schema.adapter.postgis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencingEntry;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;
import org.citydb.plugins.ade_manager.util.PathResolver;

public class PostgisADEDBSchemaManager extends AbstractADEDBSchemaManager {

	public PostgisADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	public void cleanupADEData(String adeId) throws SQLException {
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		PreparedStatement ps = null;
		Map<Integer, String>cityobjectIds = queryADECityobjectIds(adeId);
		int batchSize = 100;
		try {
			int sum = cityobjectIds.size();
			String call = "select " + schema + ".del_cityobject(?);";
			ps = connection.prepareStatement(call);	
			int counter = 0;
			Object[] inputArray = new Object[batchSize];
			for (Integer objectId: cityobjectIds.keySet()) {
				inputArray[counter] = objectId;
				counter++;				
				if (counter == batchSize) {
					ps.setArray(1, connection.createArrayOf("INTEGER", inputArray));
					ps.executeQuery();
					counter = 0;
					inputArray = new Object[batchSize];
				}					
				String className = cityobjectIds.get(objectId);
				LOG.info(className + "(ID = " + objectId + ")" + " deleted.");
				LOG.info("Number of remaining ADE objects to be deleted: " + --sum);
			}
			if (counter > 0) {
				ps.setArray(1, connection.createArrayOf("INTEGER", inputArray));
				ps.executeQuery();
			}								
		} finally {
			if (ps != null)
				ps.close();
		}
	}

	public List<String> query_selfref_fk(String tableName, String schemaName) throws SQLException {
		tableName = appendSchemaPrefix(tableName);
		
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
		tableName = appendSchemaPrefix(tableName);
		
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		List<MnRefEntry> result = new ArrayList<MnRefEntry>();
		
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("select ")
				  	  .append("nRef.root_table_name, ")
				  	  .append("nRef.n_table_name, ")
				  	  .append("nRef.n_fk_column_name, ")
				  	  .append("nRef.n_column_is_not_null, ")
				  	  .append("mRef.m_table_name, ")
				  	  .append("mRef.m_fk_column_name ")
				  .append("FROM (")
				  	  .append("SELECT ")
				  	 	  .append("c.confrelid::regclass::text AS root_table_name, ")
				  	 	  .append("c.conrelid::regclass::text AS n_table_name, ")
				  	 	  .append("c.conkey as nFk, ")
				  	 	  .append("a.attnotnull AS n_column_is_not_null, ")
				  	 	  .append("a.attname::text AS n_fk_column_name ")
				  	  .append("FROM ")
				  	  	  .append("pg_constraint c ")
				  	  .append("JOIN ")
				  	  	  .append("pg_attribute a ")
				  	  	  .append("ON a.attrelid = c.conrelid ")
				  	  	  .append("AND a.attnum = ANY (c.conkey) ")
				  	  .append("WHERE ")
				  	       .append("c.confrelid::regclass::text = '").append(tableName).append("' ")
				  	       .append("AND c.conrelid <> c.confrelid ")
				  	       .append("AND c.contype = 'f' ")
				  	       .append(") nRef ")
				 .append("LEFT JOIN (")
				     .append("SELECT ")
				     	 .append("mn.conrelid::regclass::text AS n_table_name, ")
					     .append("mn.confrelid::regclass::text AS m_table_name, ")
					     .append("mna.attname::text AS m_fk_column_name ")
					 .append("FROM ")
					     .append("pg_constraint mn ")
					 .append("JOIN ")
					     .append("pg_attribute mna ")
					     .append("ON mna.attrelid = mn.conrelid ")
					     .append("AND mna.attnum = ANY (mn.conkey) ")
					 .append("JOIN ")
					     .append("pg_constraint pk ")
					     .append("ON pk.conrelid = mn.conrelid ")
					     .append("AND pk.conkey @> mn.conkey ")
					 .append("WHERE ")
					     .append("mn.contype = 'f' ")
					     .append("AND pk.contype = 'p' ")	
					 .append(") mRef ")
				 .append("ON ")
				 	 .append("mRef.n_table_name = nRef.n_table_name ")
					 .append("AND mRef.m_table_name <> nRef.n_table_name ")
					 .append("AND mRef.m_fk_column_name <> nRef.n_fk_column_name ")
					 .append("AND mRef.m_table_name <> nRef.root_table_name ")
				 .append("ORDER BY ")
					 .append("nRef.n_table_name, ")
					 .append("mRef.m_table_name");

		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();			
			while (rs.next()) {
				MnRefEntry refEntry = new MnRefEntry();
				refEntry.setRootTableName(removeSchemaPrefix(rs.getString(1)));
				refEntry.setnTableName(removeSchemaPrefix(rs.getString(2)));
				refEntry.setnFkColumnName(rs.getString(3));
				refEntry.setnColIsNotNull(rs.getBoolean(4));
				refEntry.setmTableName(removeSchemaPrefix(rs.getString(5)));
				refEntry.setmFkColumnName(rs.getString(6));
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
		tableName = appendSchemaPrefix(tableName);
		
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
				String refTable = removeSchemaPrefix(rs.getString(1));
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
		tableName = appendSchemaPrefix(tableName);
		
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
				      .append("AND p.contype = 'p' ");

		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();
						
			if (rs.next())
				result = removeSchemaPrefix(rs.getString(1));				
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
		tableName = appendSchemaPrefix(tableName);
				
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
				  .append("GROUP BY ")
				      .append("c.confrelid, ")
				      .append("a_ref.attname");
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();
						
			while (rs.next()) {
				String refTable = removeSchemaPrefix(rs.getString(1));
				String refColumn = rs.getString(2);				
				String[] fkColumns = (String[])rs.getArray(3).getArray();
				boolean shouldAdd = true;
				for (int i = 0; i < fkColumns.length; i++) {
					if (fkColumns[i].equalsIgnoreCase("id"))
						shouldAdd = false;
				}
				if (shouldAdd)
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
	protected String readCreateADEDBScript() throws IOException {
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, DatabaseType.POSTGIS);	
		
		return new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
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
				LOG.debug("DB-function '" + funcName + "' successfully dropped");
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
				      .append("WHERE routines.specific_schema= '").append(schema).append("' and routines.routine_name like 'del_%' ")
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
