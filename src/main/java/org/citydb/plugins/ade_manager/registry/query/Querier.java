package org.citydb.plugins.ade_manager.registry.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.plugins.ade_manager.registry.query.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.query.sql.SQLBuilder;
import org.citydb.plugins.ade_manager.registry.query.sql.SQLBuilderFactory;

public class Querier {
	private final Connection connection;
	private final SQLBuilder sqlBuilder;
	
	public Querier (Connection connection) {
		this.connection = connection;
		this.sqlBuilder = SQLBuilderFactory.getInstance().createSQLBuilder();
	}
	
	public List<String> query_selfref_fk(String tableName, String schemaName) throws SQLException {		
		List<String> result = new ArrayList<String>();		
		PreparedStatement pstsmt = null;
		ResultSet rs = null;

		try {
			String sql = sqlBuilder.create_query_selfref_fk(tableName, schemaName);
			pstsmt = connection.prepareStatement(sql);
			rs = pstsmt.executeQuery();			
			
			while (rs.next()) {
				result.add(removeSchemaPrefix(rs.getString(1)));
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

		try {
			String sql = sqlBuilder.create_query_ref_fk(tableName, schemaName);
			pstsmt = connection.prepareStatement(sql);
			rs = pstsmt.executeQuery();			
			while (rs.next()) {
				MnRefEntry refEntry = new MnRefEntry();
				refEntry.setRootTableName(removeSchemaPrefix(rs.getString(1)));
				refEntry.setnTableName(removeSchemaPrefix(rs.getString(2)));
				refEntry.setnFkColumnName(removeSchemaPrefix(rs.getString(3)));
				refEntry.setmTableName(removeSchemaPrefix(rs.getString(4)));
				refEntry.setmFkColumnName(removeSchemaPrefix(rs.getString(5)));
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

	public String query_ref_to_parent_fk(String tableName, String schemaName) throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		String result = null;
		
		try {
			String sql = sqlBuilder.create_query_ref_to_parent_fk(tableName, schemaName);
			pstsmt = connection.prepareStatement(sql);
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
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		List<ReferencedEntry> result = new ArrayList<ReferencedEntry>();
				
		try {
			String sql = sqlBuilder.create_query_ref_to_fk(tableName, schemaName);
			pstsmt = connection.prepareStatement(sql);
			rs = pstsmt.executeQuery();
						
			while (rs.next()) {
				String refTable = removeSchemaPrefix(rs.getString(1));	
				String[] fkColumns = rs.getString(2).toLowerCase().split(",");
				boolean shouldAdd = true;
				for (int i = 0; i < fkColumns.length; i++) {
					if (fkColumns[i].equalsIgnoreCase("id"))
						shouldAdd = false;
				}
				if (shouldAdd)
					result.add(new ReferencedEntry(refTable, fkColumns));				
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

	private String removeSchemaPrefix(String tableName) {
		if (tableName == null)
			return tableName;
		return tableName.toLowerCase().substring(tableName.indexOf(".") + 1);
	}
}
