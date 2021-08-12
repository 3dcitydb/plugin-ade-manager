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
package org.citydb.plugins.ade_manager.registry.query;

import org.citydb.plugins.ade_manager.registry.query.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.query.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.query.sql.SQLBuilder;
import org.citydb.plugins.ade_manager.registry.query.sql.SQLBuilderFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Querier {
	private final Connection connection;
	private final SQLBuilder sqlBuilder;
	private List<String> associativeTables;
	
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
		List<String> aTables = getAssociativeTables(schemaName);
		try {
			String sql = sqlBuilder.create_query_ref_fk(tableName, schemaName);
			pstsmt = connection.prepareStatement(sql);
			rs = pstsmt.executeQuery();			
			while (rs.next()) {
				String rootTable = removeSchemaPrefix(rs.getString(1));
				String nTable = removeSchemaPrefix(rs.getString(2));
				String nFkColumn = removeSchemaPrefix(rs.getString(3));
				MnRefEntry refEntry = new MnRefEntry();
				refEntry.setRootTableName(rootTable);
				refEntry.setnTableName(nTable);
				refEntry.setnFkColumnName(nFkColumn);
				if (aTables.contains(nTable)) {
					List<ReferencedEntry> rfs = query_ref_to_fk(nTable, schemaName);
					for (ReferencedEntry rf: rfs) {
						String ref_to_table = rf.getRefTable();
						String[] fk_columns = rf.getFkColumns();
						for (int i = 0; i < fk_columns.length; i++) {
							String mFk_column = fk_columns[i];
							if (!mFk_column.equalsIgnoreCase(nFkColumn)) {
								refEntry.setmTableName(ref_to_table);
								refEntry.setmFkColumnName(mFk_column);
							}
						}						
					}
				}
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

	public List<String> getAssociativeTables(String schemaName) throws SQLException {
		if (associativeTables == null) {
			associativeTables = new ArrayList<String>();
			PreparedStatement pstsmt = null;
			ResultSet rs = null;
			try {
				String sql = sqlBuilder.create_query_associative_tables(schemaName);
				pstsmt = connection.prepareStatement(sql);
				rs = pstsmt.executeQuery();
							
				while (rs.next()) {
					String aTable = removeSchemaPrefix(rs.getString(1));	
					associativeTables.add(aTable);	
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
		}
		
		return associativeTables;
	}

	private String removeSchemaPrefix(String tableName) {
		if (tableName == null)
			return tableName;
		return tableName.toLowerCase().substring(tableName.indexOf(".") + 1);
	}
	
}
