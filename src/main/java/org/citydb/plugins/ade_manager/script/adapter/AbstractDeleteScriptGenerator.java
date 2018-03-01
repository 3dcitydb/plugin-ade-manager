package org.citydb.plugins.ade_manager.script.adapter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.script.DsgException;
import org.citydb.plugins.ade_manager.script.IDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.script.RelationType;

public abstract class AbstractDeleteScriptGenerator implements IDeleteScriptGenerator {
	private final int MAX_FUNCNAME_LENGTH = 30;
	protected final String br = System.lineSeparator();
	protected final String space = " ";
	protected final String brDent1 = br + "  ";
	protected final String brDent2 = brDent1 + "  ";
	protected final String brDent3 = brDent2 + "  ";
	protected final String brDent4 = brDent3 + "  ";
	protected final String brDent5 = brDent4 + "  ";
	
	protected String updateConstraintsSql = "";
	protected Map<String, String> deleteFuncNames;
	protected Map<String, String> deleteFuncDefs;
	protected Map<QName, Boolean> tableAggregationInfo;
	
	protected DatabaseConnectionPool dbPool;
		
	@Override
	public void doProcess(DatabaseConnectionPool dbPool, File outputFile) throws DsgException {	
		this.deleteFuncNames = new HashMap<String, String>();
		this.deleteFuncDefs = new HashMap<String, String>();
		this.tableAggregationInfo = new HashMap<QName, Boolean>();
		this.dbPool = dbPool;
		
		try {
			queryTableAggregationInfo();
		} catch (SQLException e) {
			throw new DsgException("Failed to fetch the table aggregation information from 3dcitydb", e);
		}
		
		generateDeleteFuncs("cityobject", "citydb");
		
		writeToFile(outputFile);
	}
	
	protected abstract void generateDeleteFuncs(String initTableName, String schemaName) throws DsgException;
		
	protected abstract String create_delete_function(String tableName, String schemaName) throws SQLException;
	
	protected String buildComment(String text) {
		{
			StringBuilder builder = new StringBuilder();
			builder.append("/*").append(brDent1).append(text).append(br).append("*/");			
			return builder.toString();
	    }
	}
	
	protected void createDeleteFunc(String tableName, String schemaName) throws SQLException {
		if (!deleteFuncDefs.containsKey(tableName)) {
			deleteFuncDefs.put(tableName, ""); // dummy
			deleteFuncDefs.put(tableName, create_delete_function(tableName, schemaName));
		}			
	}
	
	protected String getFuncName(String tableName) {
		if (deleteFuncNames.containsKey(tableName))
			return deleteFuncNames.get(tableName);
		
		String funcName = "delete_" + tableName;
		if (funcName.length() >= MAX_FUNCNAME_LENGTH)
			funcName = funcName.substring(0, MAX_FUNCNAME_LENGTH);
		
		deleteFuncNames.put(tableName, funcName);
		
		return funcName;
	}
	
	protected void queryTableAggregationInfo() throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Connection conn = null;
		
		try {
			conn = dbPool.getConnection();
			pstsmt = conn.prepareStatement("Select * from citydb_table");
			rs = pstsmt.executeQuery();
						
			while (rs.next()) {
				String childTable = rs.getString(1).toLowerCase();
				String parentTable = rs.getString(2).toLowerCase();
				boolean isComposite = (rs.getInt(3) == 1);
				tableAggregationInfo.put(new QName(childTable, parentTable), isComposite);
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
	}
	
	protected RelationType checkTableRelation(String childTable, String parentTable) {
		QName key = new QName(childTable, parentTable);
		if (tableAggregationInfo.containsKey(key)) {
			boolean isComposite = tableAggregationInfo.get(key);
			if (isComposite)
				return RelationType.composition;
			else
				return RelationType.aggregation;
		} 			
		else {
			return RelationType.no_agg_comp;
		} 			
	}	
	
	private void writeToFile(File outputFile) throws DsgException {
		PrintWriter writer = null;
		try {
			// header part
			writer = new PrintWriter(outputFile);
			writer.println(buildComment("Automatically generated SQL statements for updating some certain FK constraints"));
			writer.println(updateConstraintsSql);			
			writer.println(buildComment("Automatically generated 3DcityDB-delete-functions"));
			for (String funcName: deleteFuncNames.values()) {
				writer.println("--" + funcName);
				System.out.println(funcName);
			}
			writer.println("------------------------------------------");
			// function definition part
			for (String tableName: deleteFuncDefs.keySet()) {
				writer.println(buildComment("Delete function for table: " + tableName.toUpperCase() 
						+ brDent1 + "caller = 0 (default): function is called from neither its parent, nor children tables"
						+ brDent1 + "caller = 1 : function is called from its parent table" 
						+ brDent1 + "caller = 2 : function is called from its children tables" ));
				writer.println(deleteFuncDefs.get(tableName));
				writer.println("------------------------------------------");
			} 
		} catch (IOException e) {
			throw new DsgException("Failed to open file '" + outputFile.getName() + "' for writing.", e);
		} finally {
			writer.close();	
		}	
	}
	
	protected class ReferencingEntry {
		private String refColumn;
		private String refTable;
		
		public ReferencingEntry(String refTable, String refColumn) {
			this.refTable = refTable;
			this.refColumn = refColumn;
		}
		
		public String getRefColumn() {
			return refColumn;
		}

		public void setRefColumn(String refColumn) {
			this.refColumn = refColumn;
		}

		public String getRefTable() {
			return refTable;
		}

		public void setRefTable(String refTable) {
			this.refTable = refTable;
		}		
	}
	
	protected class ReferencedEntry {
		private String refTable;
		private String refColumn;
		private String[] fkColumns;

		public ReferencedEntry(String refTable, String refColumn, String[] fkColumns) {
			this.refTable = refTable;
			this.refColumn = refColumn;
			this.fkColumns = fkColumns;
		}
		
		public String getRefTable() {
			return refTable;
		}

		public void setRefTable(String refTable) {
			this.refTable = refTable;
		}

		public String getRefColumn() {
			return refColumn;
		}

		public void setRefColumn(String refColumn) {
			this.refColumn = refColumn;
		}

		public String[] getFkColumns() {
			return fkColumns;
		}

		public void setFkColumns(String[] fkColumns) {
			this.fkColumns = fkColumns;
		}
	}

	protected class MnRefEntry {
		private String rootTableName;
		private String nTableName;
		private String nFkColumnName;
		private String nFkName;
		private String mTableName;
		private String mFkColumnName;
		private String mRefColumnName;
		private String mFkName;
		private boolean nColIsNotNull;

		public boolean isnColIsNotNull() {
			return nColIsNotNull;
		}

		public void setnColIsNotNull(boolean nColIsNotNull) {
			this.nColIsNotNull = nColIsNotNull;
		}

		public MnRefEntry() {}

		public String getRootTableName() {
			return rootTableName;
		}

		public void setRootTableName(String rootTableName) {
			this.rootTableName = rootTableName;
		}

		public String getnTableName() {
			return nTableName;
		}

		public void setnTableName(String nTableName) {
			this.nTableName = nTableName;
		}

		public String getnFkColumnName() {
			return nFkColumnName;
		}

		public void setnFkColumnName(String nFkColumnName) {
			this.nFkColumnName = nFkColumnName;
		}

		public String getmTableName() {
			return mTableName;
		}

		public void setmTableName(String mTableName) {
			this.mTableName = mTableName;
		}

		public String getmFkColumnName() {
			return mFkColumnName;
		}

		public void setmFkColumnName(String mFkColumnName) {
			this.mFkColumnName = mFkColumnName;
		}

		public String getmRefColumnName() {
			return mRefColumnName;
		}

		public void setmRefColumnName(String mRefColumnName) {
			this.mRefColumnName = mRefColumnName;
		}

		public String getnFkName() {
			return nFkName;
		}

		public void setnFkName(String nFkName) {
			this.nFkName = nFkName;
		}

		public String getmFkName() {
			return mFkName;
		}

		public void setmFkName(String mFkName) {
			this.mFkName = mFkName;
		}

	}
	
}
