/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2019
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
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
package org.citydb.plugins.ade_manager.transformation.database;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformInfo;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Reference;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.platform.SqlBuilder;
import org.apache.ddlutils.platform.oracle.Oracle10Platform;
import org.apache.ddlutils.platform.postgresql.PostgreSqlPlatform;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.database.extension.RestrictableForeignKey;
import org.citydb.plugins.ade_manager.transformation.database.extension.IndexedColumn;
import org.citydb.plugins.ade_manager.transformation.database.extension.SpatialColumn;
import org.citydb.plugins.ade_manager.transformation.database.extension.TimestampColumn;
import org.citydb.plugins.ade_manager.transformation.graph.ADEschemaHelper;
import org.citydb.plugins.ade_manager.transformation.graph.GraphNodeArcType;
import org.citydb.plugins.ade_manager.util.GlobalConstants;
import org.citydb.plugins.ade_manager.util.NameShortener;
import org.citydb.plugins.ade_manager.util.PathResolver;

import agg.attribute.AttrInstance;
import agg.xt_basis.Arc;
import agg.xt_basis.GraGra;
import agg.xt_basis.Node;
import agg.xt_basis.Type;

public class DBScriptGenerator {	
	private Map<String, Table> databaseTables;
	private GraGra graphGrammar;
	
	private List<String> dbFkConstratintNameList;
	private List<String> dbIndexNameList;
	private List<String> dbTableNameList;
	private List<String> dbSeqeunceNameList;
	private Map<String, List<String>> dbTableColumnsMap;
	private Platform databasePlatform;
	private ConfigImpl config;
	private static final String indentStr = "    ";
	private final Logger LOG = Logger.getInstance();

	public DBScriptGenerator(GraGra graphGrammar, ConfigImpl config) {
		this.graphGrammar = graphGrammar;		
		new ArrayList<String>(); 
		this.dbFkConstratintNameList = new ArrayList<String>(); 
		this.dbIndexNameList = new ArrayList<String>(); 
		this.dbTableNameList = new ArrayList<String>(); 
		this.dbSeqeunceNameList = new ArrayList<String>(); 
		this.dbTableColumnsMap = new HashMap<String, List<String>>();
		this.databaseTables = new HashMap<String, Table>();	
		this.config = config;
	}

	public Database createDatabaseScripts() {
		// shorten database object name;
		this.shrotenDatabaseObjectName();

		// create database tables
		List<Node> tableNodes = getTableNodes();
		Iterator<Node> iter = tableNodes.iterator();
		while (iter.hasNext()) {
			Node tableNode = iter.next();
			this.createDatabaseTable(tableNode);			
		}
		
		// create foreign key constraints
		List<Node> joinNodes = getJoinNodes();
		iter = joinNodes.iterator();
		while (iter.hasNext()) {
			Node joinNode = iter.next();
			this.createForeignKeyContraint(joinNode);			
		}
		
		Database database = new Database();	
		List<Table> list = new ArrayList<Table>(databaseTables.values());
		database.addTables(list);
		
		// Oracle version
		databasePlatform = new Oracle10Platform();		
		this.marshallingDatabaseSchema(databasePlatform, database);
		
		// PgSQL version
		databasePlatform = new PostgreSqlPlatform();		
		this.marshallingDatabaseSchema(databasePlatform, database);
		
		return database;
	}
	
	public Platform getDatabasePlatform () {
		return this.databasePlatform;
	}
	
	private void createDatabaseTable(Node tableNode) {
		String tableName = (String) tableNode.getAttribute().getValueAt("name");	
		Table dbTable = new Table();
		dbTable.setName(tableName);
				
		Iterator<Arc> iter = tableNode.getIncomingArcs();
		while (iter.hasNext()) {
			Arc arc = iter.next();
			if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.BelongsTo)) {
				Node columnNode = (Node) arc.getSource();
				if (columnNode.getType().getParent().getName().equalsIgnoreCase(GraphNodeArcType.JoinColumn)
						|| columnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.JoinColumn)) {
					this.createJoinColumn(dbTable, columnNode);
				}
				else if (columnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.NormalDataColumn)) {
					this.createNoramlDataColumn(dbTable, columnNode);
				}
				else if (columnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.GenericDataColumn)) {
					this.createGeneircDataColumn(dbTable, columnNode);
				}
				else if (columnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.InlineGeometryColumn)) {
					this.createInlineGeometryColumn(dbTable, columnNode);
				}
			}
		}
		
		databaseTables.put(tableName, dbTable);
	}
	
	private void createIndexForColumn(Table dbTable, IndexedColumn indexedColumn, Node columnNode) {
		Iterator<Arc> iter = columnNode.getIncomingArcs();
		while (iter.hasNext()) {
			Arc arc = iter.next();
			if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.TargetColumn)) {
				Node sourceNode = (Node) arc.getSource();
				if (sourceNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.Index)) {
					String indexName = (String) sourceNode.getAttribute().getValueAt("name");
					indexedColumn.setIndexName(indexName);
				}
			}			
		}
	}
	
	private void createGeneircDataColumn(Table dbTable, Node columnNode) {
		String columnName = (String) columnNode.getAttribute().getValueAt("name");
		Column column = new Column();
		column.setName(columnName);
		column.setTypeCode(Types.CLOB);
		dbTable.addColumn(column);
	}
		
	private void createJoinColumn(Table dbTable, Node columnNode) {
		String columnName = (String) columnNode.getAttribute().getValueAt("name");	
		IndexedColumn indexedColumn = new IndexedColumn();
		indexedColumn.setName(columnName);
		indexedColumn.setTypeCode(Types.INTEGER);
		dbTable.addColumn(indexedColumn);		
		if (columnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.PrimaryKeyColumn)) {
			indexedColumn.setPrimaryKey(true);
			indexedColumn.setRequired(true);
			
		}
		if (!columnName.equalsIgnoreCase("id"))
			this.createIndexForColumn(dbTable, indexedColumn, columnNode);				
	}
	
	private void createNoramlDataColumn(Table dbTable, Node columnNode) {
		String columnName = (String)columnNode.getAttribute().getValueAt("name");
		String columnSourceType = (String)columnNode.getAttribute().getValueAt("primitiveDataType");
		Column column = new Column();
		column.setName(columnName);

		if (columnSourceType.equalsIgnoreCase("string")) {
			column.setTypeCode(Types.VARCHAR);
			column.setSize("1000");
		}
		else if (columnSourceType.equalsIgnoreCase("boolean")) {
			column.setTypeCode(Types.NUMERIC);
		}
		else if (columnSourceType.equalsIgnoreCase("double")) {
			column.setTypeCode(Types.NUMERIC);
		}
		else if (columnSourceType.equalsIgnoreCase("date")) {
			column.setTypeCode(Types.DATE);
		}
		else if (columnSourceType.equalsIgnoreCase("timestamp")) {
			column = new TimestampColumn();
			column.setTypeCode(Types.TIMESTAMP);
			column.setName(columnName);
		}
		else if (columnSourceType.equalsIgnoreCase("integer")) {
			column.setTypeCode(Types.INTEGER);
		}
		else {
			column.setTypeCode(Types.CLOB);
		}
		
		dbTable.addColumn(column);
	}
	
	private void createInlineGeometryColumn (Table dbTable, Node columnNode) {
		String columnName = (String) columnNode.getAttribute().getValueAt("name");
		SpatialColumn column = new SpatialColumn(this);
		column.setName(columnName);
		dbTable.addColumn(column);
		this.createIndexForColumn(dbTable, column, columnNode);	
	}
	
	private void createForeignKeyContraint(Node joinNode) {
		Iterator<Arc> iter = joinNode.getOutgoingArcs();
		
		String joinFromColumnName = null;
		String joinFromTableName = null;
		String joinToColumnName = null;
		String joinToTableName = null;
		Node joinFromColumnNode = null;
		Node joinToColumnNode = null;
		boolean joinFromColumnIsPk = false;
		boolean joinToColumnIsPk = false;
		while (iter.hasNext()) {
			Arc arc = iter.next();
			if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.JoinFrom)) {
				joinFromColumnNode = (Node) arc.getTarget();
				Node joinFromTableNode = (Node)joinFromColumnNode.getOutgoingArcs().next().getTarget();
				joinFromColumnName = (String)joinFromColumnNode.getAttribute().getValueAt("name");
				joinFromTableName= (String)joinFromTableNode.getAttribute().getValueAt("name");
				if (joinFromColumnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.PrimaryKeyColumn)) {
					joinFromColumnIsPk = true;
				}
			}
			else if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.JoinTo)) {
				joinToColumnNode = (Node) arc.getTarget();
				Node joinToTableNode = (Node)joinToColumnNode.getOutgoingArcs().next().getTarget();
				joinToColumnName = (String)joinToColumnNode.getAttribute().getValueAt("name");
				joinToTableName= (String)joinToTableNode.getAttribute().getValueAt("name");
				if (joinToColumnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.PrimaryKeyColumn)) {
					joinToColumnIsPk = true;
				}
			}
		}
		
		String fkName = (String) joinNode.getAttribute().getValueAt("name");
		String ondelete = (String) joinNode.getAttribute().getValueAt("ondelete");

		RestrictableForeignKey fk = new RestrictableForeignKey();
		fk.setOndelete(ondelete);
		fk.setName(fkName);
		fk.setForeignTableName(joinToTableName);	
		Reference refer = new Reference();
		
		Column localColumn = new Column();
		localColumn.setName(joinFromColumnName);
		localColumn.setPrimaryKey(joinFromColumnIsPk);
		localColumn.setRequired(joinFromColumnIsPk);
		Column foreignColumn = new Column();
		foreignColumn.setName(joinToColumnName);
		foreignColumn.setPrimaryKey(joinToColumnIsPk);
		foreignColumn.setRequired(joinToColumnIsPk);
		
		refer.setLocalColumn(localColumn);
		refer.setForeignColumn(foreignColumn);
		fk.addReference(refer);		
		Table localTable = databaseTables.get(joinFromTableName);
		localTable.addForeignKey(fk);
	}
	
	private void printComment(String text, Platform platform, PrintWriter writer) throws IOException
    {
        if (platform.isSqlCommentsOn())
        {
        	PlatformInfo platformInfo = platform.getPlatformInfo();
        	writer.print(platformInfo.getCommentPrefix());
        	writer.print(" ");
        	writer.print(text);
        	writer.print(" ");
        	writer.print(platformInfo.getCommentSuffix());
        	writer.println();
        }
    }
	
	private void shrotenDatabaseObjectName() {		
		String prefix = config.getAdeDbPrefix();		
		if (prefix.length() > GlobalConstants.MAX_DB_PREFIX_LENGTH)
			prefix = prefix.substring(0, GlobalConstants.MAX_DB_PREFIX_LENGTH);		
		int prefixLength = prefix.length();
		
		int maxTableNameLengthWithPrefix = GlobalConstants.MAX_TABLE_NAME_LENGTH - prefixLength - 1;
		int maxIndexNameLengthWithPrefix = GlobalConstants.MAX_INDEX_NAME_LENGTH - prefixLength - 1;
		int maxConstraintNameLengthWithPrefix = GlobalConstants.MAX_CONSTRAINT_NAME_LENGTH - prefixLength - 1;
		int maxSequenceNameLengthWithPrefix = GlobalConstants.MAX_SEQEUNCE_NAME_LENGTH - prefixLength - 1;
		
		Enumeration<Type> e = this.graphGrammar.getTypes();
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.DatabaseObject)) {
				List<Node> nodes = this.graphGrammar.getGraph().getNodes(nodeType);
				Iterator<Node> iter = nodes.iterator();
				while (iter.hasNext()) {
					Node databaseObjectNode = iter.next();					
					AttrInstance attr = databaseObjectNode.getAttribute();										
					String nodeTypeName = databaseObjectNode.getType().getName();
					String originalDatabaseObjectName = (String) attr.getValueAt("name");
					String shortenedName = null;
					if (nodeTypeName.equalsIgnoreCase(GraphNodeArcType.DataTable) || nodeTypeName.equalsIgnoreCase(GraphNodeArcType.JoinTable)) {												
						if (!ADEschemaHelper.CityDB_Tables.containsValue(originalDatabaseObjectName)) {
							shortenedName = NameShortener.shortenDbObjectName(originalDatabaseObjectName, maxTableNameLengthWithPrefix);
							shortenedName = prefix + "_" + shortenedName;		
							shortenedName = this.processDuplicatedDbName(dbTableNameList, shortenedName, GlobalConstants.MAX_TABLE_NAME_LENGTH, 0);
							Iterator<Arc> iter2 = databaseObjectNode.getIncomingArcs();
							while (iter2.hasNext()) {
								Arc arc = iter2.next();
								if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.BelongsTo)) {
									Node columnNode = (Node) arc.getSource();
									String columnName = (String)columnNode.getAttribute().getValueAt("name");
									columnName = NameShortener.shortenDbObjectName(columnName, GlobalConstants.MAX_COLUMN_NAME_LENGTH);	
									String processedColumnName = this.processDuplicatedDbColumnName(shortenedName, columnName, GlobalConstants.MAX_COLUMN_NAME_LENGTH, 0);
									columnNode.getAttribute().setValueAt(processedColumnName, "name");								
								}
							}								
						}											
					}
					else if (nodeTypeName.equalsIgnoreCase(GraphNodeArcType.Join)) {
						shortenedName = NameShortener.shortenDbObjectName(originalDatabaseObjectName, maxConstraintNameLengthWithPrefix);
						shortenedName = prefix + "_" + shortenedName;		
						shortenedName = this.processDuplicatedDbName(dbFkConstratintNameList, shortenedName, GlobalConstants.MAX_CONSTRAINT_NAME_LENGTH, 0);
					}	
					else if (nodeTypeName.equalsIgnoreCase(GraphNodeArcType.Index)) {
						shortenedName = NameShortener.shortenDbObjectName(originalDatabaseObjectName, maxIndexNameLengthWithPrefix);
						shortenedName = prefix + "_" + shortenedName;		
						shortenedName = this.processDuplicatedDbName(dbIndexNameList, shortenedName, GlobalConstants.MAX_INDEX_NAME_LENGTH, 0);
					}
					else if (nodeTypeName.equalsIgnoreCase(GraphNodeArcType.Sequence)) {
						shortenedName = NameShortener.shortenDbObjectName(originalDatabaseObjectName, maxSequenceNameLengthWithPrefix);
						shortenedName = prefix + "_" + shortenedName;		
						shortenedName = this.processDuplicatedDbName(dbSeqeunceNameList, shortenedName, GlobalConstants.MAX_SEQEUNCE_NAME_LENGTH, 0);
					}
					
					if (shortenedName != null) {
						attr.setValueAt(shortenedName, "name");
					}									
				}		
				break;
			};
		}
	}
	
	private String processDuplicatedDbName(List<String> dbNameList, String inputString, int maxLength, int k) {
		if (!dbNameList.contains(inputString)) {
			dbNameList.add(inputString);		
			return inputString;
		}
		else {
			k++;
			inputString = NameShortener.shortenDbObjectName(inputString, maxLength, k);	
			return processDuplicatedDbName(dbNameList, inputString, maxLength, k);
		}
	}
	
	private String processDuplicatedDbColumnName(String tableName, String inputString, int maxLength, int k) {
		if (!dbTableColumnsMap.containsKey(tableName)) {
			dbTableColumnsMap.put(tableName, new ArrayList<String>());
		}
		
		List<String> columnList = dbTableColumnsMap.get(tableName);
		if (!columnList.contains(inputString)) {
			columnList.add(inputString);		
			return inputString;
		}
		else {
			k++;
			inputString = NameShortener.shortenDbObjectName(inputString, maxLength, k);	
			return processDuplicatedDbColumnName(tableName, inputString, maxLength, k);
		}
	}
	
	private List<Node> getTableNodes() {
		Enumeration<Type> e = this.graphGrammar.getTypes();
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.Table)) {
				List<Node> nodes = this.graphGrammar.getGraph().getNodes(nodeType);
				return nodes;
			};
		}
		return null;
	}
	
	private List<Node> getJoinNodes() {
		Enumeration<Type> e = this.graphGrammar.getTypes();
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.Join)) {
				List<Node> nodes = this.graphGrammar.getGraph().getNodes(nodeType);
				return nodes;
			};
		}
		return null;
	}
	
	private Node getTableNodeByName(String tName) {
		List<Node> tableNodeList =  this.getTableNodes();
		Iterator<Node> iter = tableNodeList.iterator();
		while (iter.hasNext()) {
			Node tableNode = iter.next();
			String tableName = (String) tableNode.getAttribute().getValueAt("name");
			if (tableName.equalsIgnoreCase(tName))
				return tableNode;
		}
		return null;
	}
	
	private void sortTableColumns(Table table) {
		Column[] unsortedColumns = table.getColumns();
		List<Column> sortedColumns = new ArrayList<Column>();
		for (Column column: unsortedColumns) {
			if (!column.getName().equalsIgnoreCase("id")) {
				table.removeColumn(column);	
				sortedColumns.add(column);
			}												
		}
		
		Collections.sort(sortedColumns, new Comparator<Column>() {
		    @Override
		    public int compare(Column o1, Column o2) {
		        return o1.getName().compareTo(o2.getName());
		    }
		});
		
		table.addColumns(sortedColumns);
	}
	
	private boolean isMappedFromforeignClass(String tableName) {
		if (tableName.equalsIgnoreCase("Objectclass") || tableName.equalsIgnoreCase("surface_geometry") || tableName.equalsIgnoreCase("implicit_geometry"))
			return true;
		
		Node tableNode = this.getTableNodeByName(tableName);
		Iterator<Arc> iter = tableNode.getIncomingArcs();
		while (iter.hasNext()) {
			Arc arc = iter.next();
			Node sourceNode = (Node) arc.getSource();
			String sourceNodeTypeName = sourceNode.getType().getName();
			if (sourceNodeTypeName.equalsIgnoreCase(GraphNodeArcType.ComplexType)) {
				if ((boolean) sourceNode.getAttribute().getValueAt("isForeign")) {					
					return true;
				}
				else {					
					return false;
				}
			}
		}
		return false;
	}

	private void marshallingDatabaseSchema (Platform databasePlatform, Database database) {
		String headerText = "This document was automatically created by the ADE-Manager "
				+ "tool of 3DCityDB (https://www.3dcitydb.org) on "
				+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		Map<String, Table> adeTables = new TreeMap<String, Table>(databaseTables);
		
		String outputPath = config.getTransformationOutputPath();
		
		File citydbRootFolderpath = new File(PathResolver.get_citydb_folder_path(outputPath));
		if (!citydbRootFolderpath.exists()) 
			citydbRootFolderpath.mkdir();
		
		DatabaseType databaseType = null;
		if (databasePlatform instanceof Oracle10Platform) {
			databaseType = DatabaseType.ORACLE;
		}
		else if (databasePlatform instanceof PostgreSqlPlatform) {
			databaseType = DatabaseType.POSTGIS;
		}

		File dbSchemaFolder = new File(PathResolver.get_citydb_schema_folder_path(outputPath, databaseType));
		if (!dbSchemaFolder.exists()) 
			dbSchemaFolder.mkdir();

		// Create Database Schema for ADE...
		PrintWriter writer = null;
		int tableCounter = 0;	
		
		try {
			File createDbFile = new File(PathResolver.get_create_ade_db_filepath(outputPath, databaseType));
			// create tables...
			writer = new PrintWriter(createDbFile);
			SqlBuilder sqlBuilder = new SqlBuilder(databasePlatform) {};		
			sqlBuilder.setWriter(writer);
			printComment(headerText, databasePlatform, writer);	
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
			printComment("*********************************** Create tables **************************************", databasePlatform, writer);
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);				
			Iterator<Table> iterator = adeTables.values().iterator();			
			while (iterator.hasNext()) {
				Table table = iterator.next();
				if (!isMappedFromforeignClass(table.getName())) {
					printComment("--------------------------------------------------------------------", databasePlatform, writer);						
					printComment(table.getName(), databasePlatform, writer);
					printComment("--------------------------------------------------------------------", databasePlatform, writer);					
					sortTableColumns(table);					
					sqlBuilder.createTable(database, table);
					tableCounter++;	
				}
			}
			LOG.info(tableCounter + " tables are created");

			// create foreign key constraints
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
			printComment("*********************************** Create foreign keys ********************************", databasePlatform, writer);
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);	
			iterator = adeTables.values().iterator();
			while (iterator.hasNext()) {
				Table table = iterator.next();
				if (!isMappedFromforeignClass(table.getName()) && table.getForeignKeyCount() > 0) {
					this.printForeignkeyConstraints(table, writer);
				}
			}
			
			// create Indexes 
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
			printComment("*********************************** Create Indexes *************************************", databasePlatform, writer);
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);	
			
			if (databasePlatform instanceof Oracle10Platform && checkExistenceOfNodeOrArc(GraphNodeArcType.InlineGeometryColumn))
				this.printGetSridScript(writer);
			
			iterator = adeTables.values().iterator();
			while (iterator.hasNext()) {
				Table table = iterator.next();
				if (!isMappedFromforeignClass(table.getName())) {
					this.printIndexes(table, writer);
				}
			}		
			
			// create sequences
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
			printComment("*********************************** Create Sequences ***********************************", databasePlatform, writer);
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);	
			this.printCreateSequences(writer);	

		} catch (IOException | NullPointerException e) {			
			e.printStackTrace();
		} finally {
			writer.close();	
		}

		// Drop Database Schema for ADE...
		try {
			File dropDbFile = new File(PathResolver.get_drop_ade_db_filepath(outputPath, databaseType));
			writer = new PrintWriter(dropDbFile);
			SqlBuilder sqlBuilder = new SqlBuilder(databasePlatform) {};		
			sqlBuilder.setWriter(writer);
			printComment(headerText, databasePlatform, writer);	
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
			printComment("*********************************** Drop foreign keys **********************************", databasePlatform, writer);
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);		
			Iterator<Table> iterator = adeTables.values().iterator();
			while (iterator.hasNext()) {
				Table table = iterator.next();
				if (!isMappedFromforeignClass(table.getName()) && table.getForeignKeyCount() > 0) {
					printComment("--------------------------------------------------------------------", databasePlatform, writer);						
					printComment(table.getName(), databasePlatform, writer);
					printComment("--------------------------------------------------------------------", databasePlatform, writer);
					sqlBuilder.dropExternalForeignKeys(table);
				}
			}
			
			// drop tables
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
			printComment("*********************************** Drop tables ***************************************", databasePlatform, writer);
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);	
			iterator = adeTables.values().iterator();
			while (iterator.hasNext()) {
				Table table = iterator.next();
				if (!isMappedFromforeignClass(table.getName())) {
					printComment("--------------------------------------------------------------------", databasePlatform, writer);						
					printComment(table.getName(), databasePlatform, writer);
					printComment("--------------------------------------------------------------------", databasePlatform, writer);
					sqlBuilder.dropTable(table);
				}
			}
			
			// drop sequences
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
			printComment("*********************************** Drop Sequences *************************************", databasePlatform, writer);
			printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);	
			this.printDropSequences(writer);

			// Recycle Bin for Oracle
			if (databasePlatform instanceof Oracle10Platform)
				printRecycleBinForOracle(writer);
		} catch (IOException | NullPointerException e) {			
			e.printStackTrace();
		} finally {
			writer.close();
		}
		
		// create versioning scripts which are only available for Oracle
		if (databaseType == DatabaseType.ORACLE) {
			// create enable-Versioning script
			try {
				File enableVersioningFile = new File(PathResolver.get_enable_ade_versioning_filepath(outputPath, databaseType));
				writer = new PrintWriter(enableVersioningFile);
				printComment(headerText, databasePlatform, writer);	
				printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
				printComment("*********************************** Enable Versioning **********************************", databasePlatform, writer);
				printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);	
				writer.println();
				Iterator<Table> iterator = adeTables.values().iterator();
				StringBuilder commandStr = new StringBuilder().append("DBMS_WM.EnableVersioning('"); 
				while (iterator.hasNext()) {
					Table table = iterator.next();
					String tableName = table.getName();
					if (!isMappedFromforeignClass(tableName)) 
						commandStr.append(tableName).append(iterator.hasNext()?",":"");										
				}	
				commandStr.append("','VIEW_WO_OVERWRITE');"); 

				writer.println("exec " + commandStr.toString());
			} catch (IOException | NullPointerException e) {			
				e.printStackTrace();
			} finally {
				writer.close();	
			}
			
			// create disable-Versioning script
			try {
				File disableVersioningFile = new File(PathResolver.get_disable_ade_versioning_filepath(outputPath, databaseType));				
				writer = new PrintWriter(disableVersioningFile);
				printComment(headerText, databasePlatform, writer);	
				printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);						
				printComment("*********************************** Disable Versioning *********************************", databasePlatform, writer);
				printComment("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++", databasePlatform, writer);	
				writer.println();
				Iterator<Table> iterator = adeTables.values().iterator();
				StringBuilder commandStr = new StringBuilder().append("DBMS_WM.DisableVersioning('"); 
				while (iterator.hasNext()) {
					Table table = iterator.next();
					String tableName = table.getName();
					if (!isMappedFromforeignClass(tableName)) 
						commandStr.append(tableName).append(iterator.hasNext()?",":"");										
				}	
				commandStr.append("',true, true);"); 

				writer.println("exec " + commandStr.toString());
			} catch (IOException | NullPointerException e) {			
				e.printStackTrace();
			} finally {
				writer.close();	
			}		
		}  
	}
	
	private void printGetSridScript(PrintWriter writer) {
		writer.println();
		writer.println("SET SERVEROUTPUT ON");
		writer.println("SET FEEDBACK ON");
		writer.println("SET VER OFF");
		writer.println("VARIABLE SRID NUMBER;");		
		writer.print("BEGIN");
		writer.println();
		writer.print("  SELECT SRID INTO :SRID FROM DATABASE_SRS;");
		writer.println();
		writer.println("END;");
		writer.println("/");
		writer.println();
		writer.println("column mc new_value SRSNO print");
		writer.println("select :SRID mc from dual;");
		writer.println();
		writer.println("prompt Used SRID for spatial indexes: &SRSNO; ");
		writer.println();
	}
	
	private void printForeignkeyConstraints(Table table, PrintWriter writer) throws IOException {
		String tablenName = table.getName();
		boolean flag = false;
		for (int idx = 0; idx < table.getForeignKeyCount(); idx++)
        {
			RestrictableForeignKey fk = (RestrictableForeignKey) table.getForeignKey(idx);
			String fkName = fk.getName();
			String fkColumnName = fk.getFirstReference().getLocalColumnName();
			String refTableName = fk.getForeignTableName();
			String refColumnName = fk.getFirstReference().getForeignColumnName();
			String ondelete = fk.getOndelete();
           
            if (!flag) {
        		printComment("--------------------------------------------------------------------", databasePlatform, writer);						
				printComment(table.getName(), databasePlatform, writer);
				printComment("--------------------------------------------------------------------", databasePlatform, writer);
				flag = true;
        	}            
			writer.print("ALTER TABLE ");
			writer.print(tablenName);
			writer.print(" ADD CONSTRAINT ");
			writer.print(fkName);
			writer.print(" FOREIGN KEY (");
			writer.print(fkColumnName);
			writer.print(")");
			writer.println();
			writer.print("REFERENCES ");
			writer.print(refTableName);
			writer.print(" (");
			writer.print(refColumnName);
			writer.print(")");
			if (ondelete != null) {
				writer.println();
				writer.print("ON DELETE ");
				writer.print(ondelete);				
			}
			writer.print(";");			 
        	writer.println();
        	writer.println();
        } 
	}
	
	private void printIndexes(Table table, PrintWriter writer) throws IOException {
		String tablenName = table.getName();
		boolean flag = false;
		for (int idx = 0; idx < table.getColumnCount(); idx++)
        {
            Column column = table.getColumn(idx);
            if (column instanceof IndexedColumn && !column.getName().equalsIgnoreCase("id")) {
            	if (!flag) {
            		printComment("--------------------------------------------------------------------", databasePlatform, writer);						
					printComment(table.getName(), databasePlatform, writer);
					printComment("--------------------------------------------------------------------", databasePlatform, writer);
					flag = true;
            	}
            	String indexName = ((IndexedColumn) column).getIndexName();
            	String columnName = column.getName();
            	if (column instanceof SpatialColumn) {
            		if (databasePlatform instanceof PostgreSqlPlatform) {
            			 writer.print("CREATE");
                         writer.print(" INDEX ");
                         writer.print(indexName);
                         writer.print(" ON ");
                         writer.print(tablenName);
	                   	 writer.println();
	                   	 writer.print(indentStr);
	                   	 writer.print("USING gist");
	                   	 writer.println();
	                   	 writer.print(indentStr);
	                   	 writer.print("(");
	                   	 writer.println();
	                   	 writer.print(indentStr + "  ");
	                   	 writer.print(columnName);
	                   	 writer.println();
	                   	 writer.print(indentStr);
	                   	 writer.print(");");
	                   	 writer.println();
                   }
                   else {
                	   writer.print("DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME='");
                	   writer.print(tablenName.toUpperCase());
                	   writer.print("' AND COLUMN_NAME='");
                	   writer.print(columnName.toUpperCase());
                	   writer.print("';");
                	   writer.println();
                	   writer.print("INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID)");
                	   writer.println();
                	   writer.print("VALUES ('");
                	   writer.print(tablenName.toUpperCase());
                	   writer.print("','");
                	   writer.print(columnName.toUpperCase());
                	   writer.print("',");
                	   writer.println();
                	   writer.print("MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X', 0.000, 10000000.000, 0.0005), MDSYS.SDO_DIM_ELEMENT('Y', 0.000, 10000000.000, 0.0005),MDSYS.SDO_DIM_ELEMENT('Z', -1000, 10000, 0.0005)), &SRSNO);");
                	   writer.println();
                	   writer.print("CREATE");
                       writer.print(" INDEX ");
                       writer.print(indexName);
                       writer.print(" ON ");
                       writer.print(tablenName);
                       writer.print(" (");
                       writer.print(columnName);
                       writer.print(")");    
                       writer.print(" INDEXTYPE IS MDSYS.SPATIAL_INDEX;");
                       writer.println();
                   }
                }
            	else {            		
                    writer.print("CREATE");
                    writer.print(" INDEX ");
                    writer.print(indexName);
                    writer.print(" ON ");
                    writer.print(tablenName);
                    if (databasePlatform instanceof PostgreSqlPlatform) {
                    	 writer.println();
                    	 writer.print(indentStr);
                    	 writer.print("USING btree");
                    	 writer.println();
                    	 writer.print(indentStr);
                    	 writer.print("(");
                    	 writer.println();
                    	 writer.print(indentStr + "  ");
                    	 writer.print(columnName);
                    	 writer.print(" ASC NULLS LAST");
                    	 writer.println();
                    	 writer.print(indentStr);
                    	 writer.print(")");
                    	 writer.print("   WITH (FILLFACTOR = 90);");
                    	 writer.println();
                    }
                    else {
                    	writer.print(" (");
                        writer.print(columnName);
                        writer.print(");");
                        writer.println();                   
                    }                 
            	} 
            	writer.println();
            }          
        }
	}
	
	private void printCreateSequences(PrintWriter writer) {
		Enumeration<Type> e = this.graphGrammar.getTypes();
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.Sequence)) {
				List<Node> nodes = this.graphGrammar.getGraph().getNodes(nodeType);
				if (nodes == null)
					return;
				Iterator<Node> iter = nodes.iterator();
				while (iter.hasNext()) {
					Node sequenceNode = iter.next();
					String squenceName = (String) sequenceNode.getAttribute().getValueAt("name");
					writer.println();
					writer.print("CREATE SEQUENCE ");
					writer.print(squenceName);
					if (databasePlatform instanceof PostgreSqlPlatform) {	
						writer.println();
						writer.println("INCREMENT BY 1");
						writer.println("MINVALUE 0");
						writer.println("MAXVALUE 2147483647");
						writer.println("START WITH 1");
						writer.println("CACHE 1");
						writer.println("NO CYCLE");
						writer.println("OWNED BY NONE;");						
					}
                   else {
						writer.print(" INCREMENT BY 1 START WITH 1 MINVALUE 1 CACHE 10000;");
                   }   
					writer.println();
				}
			};
		}
		writer.println();
	}
	
	private void printDropSequences(PrintWriter writer) {
		Enumeration<Type> e = this.graphGrammar.getTypes();
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.Sequence)) {
				List<Node> nodes = this.graphGrammar.getGraph().getNodes(nodeType);
				if (nodes == null)
					return;
				Iterator<Node> iter = nodes.iterator();
				while (iter.hasNext()) {
					Node sequenceNode = iter.next();
					String squenceName = (String) sequenceNode.getAttribute().getValueAt("name");
					writer.println();
					writer.print("DROP SEQUENCE ");
					writer.print(squenceName + ";");
					writer.println();
				}
			};
		}
	}
	
	private void printRecycleBinForOracle(PrintWriter writer) {
		writer.println();
		writer.print("PURGE RECYCLEBIN;");
		writer.println();
	}
	
	private boolean checkExistenceOfNodeOrArc(String nodeOrArcTypeName) {
		Enumeration<Type> e = this.graphGrammar.getTypes();
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(nodeOrArcTypeName)) {
				List<Node> nodes = this.graphGrammar.getGraph().getNodes(nodeType);
				if (nodes == null)
					return false;
				if (nodes.size() > 0)
					return true;
			};
		}
		return false;
	}

}
