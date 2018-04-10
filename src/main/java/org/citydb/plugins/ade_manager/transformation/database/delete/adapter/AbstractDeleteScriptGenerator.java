package org.citydb.plugins.ade_manager.transformation.database.delete.adapter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.xml.namespace.QName;

import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Table;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.TransformationManager;
import org.citydb.plugins.ade_manager.transformation.database.delete.DsgException;
import org.citydb.plugins.ade_manager.transformation.database.delete.IDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.transformation.database.delete.RelationType;
import org.citydb.plugins.ade_manager.transformation.graph.GraphNodeArcType;

import agg.xt_basis.Arc;
import agg.xt_basis.GraGra;
import agg.xt_basis.Node;
import agg.xt_basis.Type;

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
	protected TransformationManager manager;
		
	@Override
	public void doProcess(TransformationManager manager, DatabaseConnectionPool dbPool, ConfigImpl config) throws DsgException {	
		this.deleteFuncNames = new HashMap<String, String>();
		this.deleteFuncDefs = new HashMap<String, String>();
		this.tableAggregationInfo = new HashMap<QName, Boolean>();
		this.dbPool = dbPool;
		this.manager = manager;

		try {
			queryTableAggregationInfoFromDb();
		} catch (SQLException e) {
			throw new DsgException("Failed to fetch the table aggregation information from 3dcitydb", e);
		}

		generateDeleteFuncs("cityobject", "citydb");
		
		writeToFile(new File(config.getTransformationOutputPath() + File.separator + "3dcitydb-delete-script.sql"));
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
	
	private void queryTableAggregationinfoFromADEGraph() {
		GraGra adeGraph = manager.getAdeGraph();
		Enumeration<Type> e = adeGraph.getTypes();		
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();				
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.ComplexType)) {
				List<Node> nodes = adeGraph.getGraph().getNodes(nodeType);
				for (Node objectNode: nodes) {
					String parentTableName = getMappedTableName(objectNode);				
					Iterator<Arc> propertyArcIter = objectNode.getOutgoingArcs();
					while(propertyArcIter.hasNext()) {
						Arc propertyArc = propertyArcIter.next();
						Node propertyNode = (Node) propertyArc.getTarget();							
						// process featureOrObjectOrDataProperty
						if (propertyNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexTypeProperty)) {
							Iterator<Arc> targetArcIter = propertyNode.getOutgoingArcs();
							while (targetArcIter.hasNext()) {
								Arc targetArc = targetArcIter.next();
								Node targetObjectNode = (Node) targetArc.getTarget();
								if (targetObjectNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexType)) {
									String derivedFrom = (String) targetObjectNode.getAttribute().getValueAt("derivedFrom");
									String childTableName = getMappedTableName(targetObjectNode);
									if (derivedFrom.equalsIgnoreCase("_Object") ) {
										if (targetArc.getType().getName().equalsIgnoreCase(GraphNodeArcType.TargetType)) {
											tableAggregationInfo.put(new QName(childTableName, parentTableName), true);
										}
									}
										
									if (targetArc.getType().getName().equalsIgnoreCase(GraphNodeArcType.TargetType)) {
										
										String relationType = (String)propertyNode.getAttribute().getValueAt("relationType");
										if (relationType != null) {
											if (relationType.equalsIgnoreCase("composition"))
												tableAggregationInfo.put(new QName(childTableName, parentTableName), true);
											else if (relationType.equalsIgnoreCase("aggregation"))
												tableAggregationInfo.put(new QName(childTableName, parentTableName), false);
										}										
									}							
								}
							}
						}	
						// process geometry property
						if (propertyNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.BrepGeometryProperty)) {
							tableAggregationInfo.put(new QName("surface_geometry", parentTableName), true);
						}
						// process implicit geometry property
						if (propertyNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ImplicitGeometryProperty)) {
							tableAggregationInfo.put(new QName("implicit_geometry", parentTableName), false);
						}
					}	
				}
			};
		}
	}
	
	private String getMappedTableName(Node objectNode) {
		String tableName = null;
		Iterator<Arc> arcIter = objectNode.getOutgoingArcs();		
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node targetNode = (Node) arc.getTarget();							
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.DataTable)) {
				tableName = (String) targetNode.getAttribute().getValueAt("name");
			}		
		}
		
		return tableName;
	}

	protected void queryTableAggregationInfoFromDb() throws SQLException {
		queryTableAggregationinfoFromADEGraph();
		
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Connection conn = null;
		
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("select distinct ")
				      .append("c.tablename as child_table_name, ")
				      .append("p.tablename as parent_table_name, ")
				      .append("a.is_composite as is_composite ")
				  .append("FROM ")
				      .append("aggregation_info a ")
				  .append("JOIN ")
				      .append("objectclass c ")
				      .append("on a.child_id = c.id ")
				  .append("JOIN ")
				      .append("objectclass p ")
				      .append("on a.parent_id = p.id ");
		
		try {
			conn = dbPool.getConnection();
			pstsmt = conn.prepareStatement(strBuilder.toString());
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
	
	protected List<String> query_selfref_fk_from_external_db(String localTableName) {
		Database adeDbSchema = this.manager.getAdeDatabaseSchema();
		List<String> result = new ArrayList<String>();
		for (Table table: adeDbSchema.getTables()) {
			if (table.getName().equalsIgnoreCase(localTableName)) {
				for (ForeignKey fk: table.getForeignKeys()) {
					if (fk.getForeignTableName().equalsIgnoreCase(localTableName)
							&& fk.getFirstReference().getForeignColumnName().equalsIgnoreCase("id")) {
						result.add(fk.getFirstReference().getLocalColumnName());
					}
				}
			}
			
		}
		return result;
	}
	
	protected List<MnRefEntry> query_ref_fk_from_external_db(String rootTableName) {
		Database adeDbSchema = this.manager.getAdeDatabaseSchema();
		List<MnRefEntry> result = new ArrayList<MnRefEntry>();		
		for (Table table: adeDbSchema.getTables()) {
			if (!table.getName().equalsIgnoreCase(rootTableName)) {
				for (ForeignKey fk: table.getForeignKeys()) {
					if (fk.getForeignTableName().equalsIgnoreCase(rootTableName)) {
						MnRefEntry entry = new MnRefEntry();
						entry.setRootTableName(rootTableName);
						entry.setnTableName(table.getName());
						entry.setnFkName(fk.getName());
						entry.setnFkColumnName(fk.getFirstReference().getLocalColumnName());
						entry.setnColIsNotNull(fk.getFirstReference().getLocalColumn().isRequired());
						if (entry.isnColIsNotNull()) {
							for (ForeignKey mFk: table.getForeignKeys()) {
								if (!mFk.getName().equalsIgnoreCase(fk.getName())
										&& mFk.getFirstReference().getLocalColumn().isRequired()) {
									entry.setmTableName(mFk.getForeignTableName());
									entry.setmFkColumnName(mFk.getFirstReference().getLocalColumnName());
									entry.setmFkName(mFk.getName());
									entry.setmRefColumnName(mFk.getFirstReference().getForeignColumnName());
								}
							}						
						}
						result.add(entry);
					}
				}
			}		
		}		
		return result;
	}

	protected List<ReferencedEntry> query_ref_to_fk_external_db (String rootTableName) {
		Database adeDbSchema = this.manager.getAdeDatabaseSchema();
		List<ReferencedEntry> result = new ArrayList<ReferencedEntry>();		
		for (Table table: adeDbSchema.getTables()) {
			if (table.getName().equalsIgnoreCase(rootTableName)) {
				Map<String, List<String>> foreignTables = new HashMap<String, List<String>>();  
				for (ForeignKey fk: table.getForeignKeys()) {
					String foreignTableName = fk.getForeignTableName();
					if (!foreignTableName.equalsIgnoreCase(rootTableName) && !foreignTableName.equalsIgnoreCase("cityobject")) {
						if (foreignTables.containsKey(foreignTableName)) {
							foreignTables.get(foreignTableName).add(fk.getFirstReference().getLocalColumnName());
						}
						else {
							List<String> fkList = new ArrayList<String>();
							fkList.add(fk.getFirstReference().getLocalColumnName());
							foreignTables.put(foreignTableName, fkList);
						}						
					}
				}

				for (String foreignTableName: foreignTables.keySet()) {
					List<String> fkList = foreignTables.get(foreignTableName);
					ReferencedEntry entry = new ReferencedEntry(foreignTableName, "id", fkList.toArray(new String[fkList.size()]));
					result.add(entry);
				}
			}		
		}		
		return result;
	}
	
	protected List<ReferencingEntry> query_ref_tables_and_columns_from_external_db(String rootTableName) {
		Database adeDbSchema = this.manager.getAdeDatabaseSchema();
		List<ReferencingEntry> result = new ArrayList<ReferencingEntry>();		
		for (Table table: adeDbSchema.getTables()) {
			for (ForeignKey fk: table.getForeignKeys()) {
				String fkTableName = table.getName();
				String foreignTableName = fk.getForeignTableName();
				String foreignKeyColumnName = fk.getFirstReference().getLocalColumnName();
				if (foreignTableName.equalsIgnoreCase(rootTableName) && !foreignKeyColumnName.equalsIgnoreCase("id")) {
					result.add(new ReferencingEntry(fkTableName, foreignKeyColumnName));
				}
			}	
		}		
		return result;
	}
	
	protected String query_ref_to_parent_fk_from_external_db(String rootTableName) {
		Database adeDbSchema = this.manager.getAdeDatabaseSchema();	
		for (Table table: adeDbSchema.getTables()) {
			if (table.getName().equalsIgnoreCase(rootTableName)) {
				for (ForeignKey fk: table.getForeignKeys()) {
					String fkLocalColumnName = fk.getFirstReference().getLocalColumnName();
					String fkForeignColumnName = fk.getFirstReference().getForeignColumnName();
					if (fkLocalColumnName.equalsIgnoreCase("id") && fkForeignColumnName.equalsIgnoreCase("id")) {
						return fk.getForeignTableName();
					}
				}	
			}
			
		}		
		return null;
	}
	
	private void writeToFile(File outputFile) throws DsgException {
		PrintWriter writer = null;
		try {
			// header part
			writer = new PrintWriter(outputFile);		
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
			if (writer != null)
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
