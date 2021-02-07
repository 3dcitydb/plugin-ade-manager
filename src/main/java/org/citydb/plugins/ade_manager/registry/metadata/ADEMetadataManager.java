/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2020
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
package org.citydb.plugins.ade_manager.registry.metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.AbstractExtension;
import org.citydb.database.schema.mapping.AbstractJoin;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.AbstractTypeProperty;
import org.citydb.database.schema.mapping.AppSchema;
import org.citydb.database.schema.mapping.ComplexType;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.InjectedProperty;
import org.citydb.database.schema.mapping.Join;
import org.citydb.database.schema.mapping.JoinTable;
import org.citydb.database.schema.mapping.Namespace;
import org.citydb.database.schema.mapping.ObjectType;
import org.citydb.database.schema.mapping.PropertyInjection;
import org.citydb.database.schema.mapping.RelationType;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.database.schema.mapping.SchemaMappingException;
import org.citydb.database.schema.mapping.SchemaMappingValidationException;
import org.citydb.database.schema.mapping.TableRole;
import org.citydb.database.schema.mapping.TreeHierarchy;
import org.citydb.database.schema.util.SchemaMappingUtil;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.util.GlobalConstants;
import org.citydb.plugins.ade_manager.util.PathResolver;
import org.citydb.util.CoreConstants;

public class ADEMetadataManager {	
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	private final Connection connection;
	private final ConfigImpl config;
	private final String schema;
	private SchemaMapping mergedSchemaMapping;	
	private final AggregationInfoCollection aggregationInfoCollection;
	
	public ADEMetadataManager(Connection connection, ConfigImpl config) throws SQLException {
		this.connection = connection;
		this.config = config;
		this.schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		this.mergedSchemaMapping = queryMergedADESchemaMapping();
		this.aggregationInfoCollection = queryAggregationInfoCollection();
	}
	
	public void importADEMetadata() throws SQLException {				
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		Path adeSchemaMappingFilePath = Paths.get(PathResolver.get_schemaMapping_filepath(adeRegistryInputpath));
		Path adeDropDBFilePath = Paths.get(PathResolver.get_drop_ade_db_filepath(adeRegistryInputpath, databaseType));
		
		SchemaMapping inputADESchemaMapping = null;
		try {			
			SchemaMapping citydbSchemaMapping = SchemaMappingUtil.getInstance().unmarshal(CoreConstants.CITYDB_SCHEMA_MAPPING_FILE);
			inputADESchemaMapping = SchemaMappingUtil.getInstance().unmarshal(citydbSchemaMapping, adeSchemaMappingFilePath.toFile());	
		} catch (JAXBException e) {
			throw new SQLException(e);
		} catch (SchemaMappingException | SchemaMappingValidationException e) {
			throw new SQLException("The 3DCityDB schema mapping is invalid.", e);
		} 
	
		try {
			validateSchemaMapping(inputADESchemaMapping);
		} catch (SQLException e) {
			throw new SQLException("Failed to read and process object class IDs. Aborting.", e);
		}
		
		List<String> adeSchemaIds = new ArrayList<String>();	
		String adeRootSchemaId = null;
		Iterator<AppSchema> adeSchemas = inputADESchemaMapping.getSchemas().iterator();
		while (adeSchemas.hasNext()) {
			AppSchema adeSchema = adeSchemas.next();
			adeSchemaIds.add(adeSchema.getId());
			if (adeSchema.isADERoot())
				adeRootSchemaId = adeSchema.getId();
		}		
		
		if (adeRootSchemaId == null)
			throw new SQLException("Failed to import metadata. Cause: An ADE must have a root schema.");
	
		long insertedADERowId;
		String insertADEQueryStr = "INSERT INTO " + schema + ".ADE"
				+ "(ID, ADEID, NAME, DESCRIPTION, VERSION, DB_PREFIX, XML_SCHEMAMAPPING_FILE, DROP_DB_SCRIPT, CREATION_DATE, CREATION_PERSON) VALUES"
				+ "(?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement psInsertADE = connection.prepareStatement(insertADEQueryStr);
		try {
			insertedADERowId = insertADE(inputADESchemaMapping, adeSchemaMappingFilePath, adeDropDBFilePath, psInsertADE);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'ADE' table.", e);
		} finally {
			psInsertADE.close();			
		}
				
		Map<String, List<Long>> insertedSchemas = null;
		String insertSchemaQueryStr = "INSERT INTO "  + schema + ".SCHEMA"
				+ "(ID, IS_ADE_ROOT, CITYGML_VERSION, XML_NAMESPACE_URI, XML_NAMESPACE_PREFIX, XML_SCHEMA_LOCATION, XML_SCHEMAFILE, XML_SCHEMAFILE_TYPE, ADE_ID) VALUES"
				+ "(?,?,?,?,?,?,?,?,?)";
		PreparedStatement psInsertSchema = connection.prepareStatement(insertSchemaQueryStr);
		try {
			insertedSchemas = insertSchemas(inputADESchemaMapping, insertedADERowId, adeSchemaIds, psInsertSchema);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA' table.", e);
		} finally {
			psInsertSchema.close();			
		}
		
		String insertSchemaReferencingQueryString = "INSERT INTO "  + schema + ".schema_referencing(REFERENCED_ID, REFERENCING_ID) VALUES(?,?)";
		PreparedStatement psInsertSchemaReferencing = connection.prepareStatement(insertSchemaReferencingQueryString);
		try {
			insertSchemaReferencing(inputADESchemaMapping, insertedSchemas, adeRootSchemaId, psInsertSchemaReferencing);		
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA_REFERENCING' table.", e);
		} finally {
			psInsertSchemaReferencing.close();			
		}
		
		Map<Long, String> objectObjectclassIds;
		String insertObjectclassQueryString = "INSERT INTO "  + schema + ".OBJECTCLASS (ID, IS_ADE_CLASS, IS_TOPLEVEL, CLASSNAME, TABLENAME, SUPERCLASS_ID, BASECLASS_ID, ADE_ID) VALUES(?,?,?,?,?,?,?,?)";
		PreparedStatement ps = connection.prepareStatement(insertObjectclassQueryString);
		try {			
			objectObjectclassIds = insertObjectclasses(inputADESchemaMapping, insertedADERowId, ps);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'OBJECTCLASS' table.", e);
		} finally {
			ps.close();			
		}
		
		String insertSchemaToObjectclassQueryString = "INSERT INTO "  + schema + ".schema_to_objectclass(SCHEMA_ID, OBJECTCLASS_ID) VALUES(?,?)";
		PreparedStatement psInsertSchemaToObjectclass = connection.prepareStatement(insertSchemaToObjectclassQueryString);
		try {
			insertSchemaToObjectclass(objectObjectclassIds, insertedSchemas, psInsertSchemaToObjectclass);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA_TO_OBJECTCLASS' table.", e);
		} finally {
			psInsertSchemaToObjectclass.close();
		}
		
		String insertAggregationInfoQueryString = "INSERT INTO " + schema + ".aggregation_info(CHILD_ID, PARENT_ID, MIN_OCCURS, MAX_OCCURS, IS_COMPOSITE, JOIN_TABLE_OR_COLUMN_NAME) VALUES(?,?,?,?,?,?)";
		PreparedStatement psInsertAggregationInfo = connection.prepareStatement(insertAggregationInfoQueryString);
		try {
			insertAggregationInfo(inputADESchemaMapping, psInsertAggregationInfo);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA_TO_OBJECTCLASS' table.", e);
		} finally {
			psInsertSchemaToObjectclass.close();
		}
	}

	public AggregationInfoCollection getAggregationInfoCollection() {
		return aggregationInfoCollection;
	}

	public SchemaMapping getMergedSchemaMapping() {
		return mergedSchemaMapping;
	}

	public List<ADEMetadataInfo> getADEMetadata() throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		ArrayList<ADEMetadataInfo> ades = new ArrayList<ADEMetadataInfo>();
	
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select * from " + schema + "." + "ade order by id");
			
			while (rs.next()) {
				String adeid = rs.getString("adeid");
				String name = rs.getString("name");
				String description = rs.getString("description");
				String version = rs.getString("version");
				String dbPrefix = rs.getString("db_prefix");
				String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(rs.getTimestamp("creation_date"));
				ades.add(new ADEMetadataInfo(adeid, name, description, version, dbPrefix, creationDate.toString()));					
			}
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}
	
		return ades;
	}
	
	public Map<Integer, String> getSubObjectclassesFromSuperTable(String superTable) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		Map<Integer, String> result = new TreeMap<Integer, String>();
	
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("SELECT o1.id, o1.tablename "
					+ "FROM " + schema + ".objectclass o1 "
					+ "JOIN " + schema + ".objectclass o2 "
					+ "ON o1.superclass_id = o2.id "
					+ "WHERE o2.tablename = '" + superTable + "'");
			
			while (rs.next()) {
				int objectclassId = rs.getInt(1);
				String tableName = rs.getString(2).toLowerCase();	
				if (!tableName.equalsIgnoreCase(superTable)) {
					Map<Integer, String> nestedSub = getSubObjectclassesFromSuperTable(tableName);
					for (Entry<Integer, String> entry: nestedSub.entrySet()) {
						result.put(entry.getKey(), entry.getValue());
					}					
				}
				result.put(objectclassId, tableName);
			}
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}
	
		return result;
	}

	public List<String> getADEHookTables(String baseTableName) throws SQLException {
		List<String> result = new ArrayList<String>();
		List<ADEMetadataInfo> ades = getADEMetadata();
		for (ADEMetadataInfo ade: ades) {
			String dbPrefix = ade.getDbPrefix();
			String schemaMappingStr = this.getSchemaMappingbyDbPrefix(dbPrefix);
			if (schemaMappingStr != null) {
				InputStream stream = new ByteArrayInputStream(schemaMappingStr.getBytes());
				try {			
					SchemaMapping citydbSchemaMapping = SchemaMappingUtil.getInstance().unmarshal(CoreConstants.CITYDB_SCHEMA_MAPPING_FILE);
					SchemaMapping targetADESchemaMapping = SchemaMappingUtil.getInstance().unmarshal(citydbSchemaMapping, stream);	
					for (PropertyInjection injection: targetADESchemaMapping.getPropertyInjections()) {
						String targetBaseTable = injection.getDefaultBase().getTable();	
						if (targetBaseTable.equalsIgnoreCase(baseTableName))
							result.add(injection.getTable());
					}
				} catch (JAXBException e) {
					throw new SQLException(e);
				} catch (SchemaMappingException | SchemaMappingValidationException e) {
					throw new SQLException("The 3DCityDB schema mapping is invalid", e);
				} 
			}
		}
		return result;
	}
	
	public void deleteADEMetadata(String adeId) throws SQLException {
		Statement stmt = null;	
		try {					
			stmt = connection.createStatement();
			stmt.executeUpdate("Delete from " + schema + ".ade where adeid = '" + adeId + "'");
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}
	
	public String getDropDBScript(String adeId) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		String dropDBScript = null;		
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select drop_db_script from " + schema + ".ade where adeid = '" + adeId + "'");		
			if (rs.next()) 
				dropDBScript = rs.getString(1);	
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}
		
		if (dropDBScript == null) 
			throw new SQLException("The script for dropping ADE database schema is not available.");
		
		return dropDBScript;
	}
	
	public List<Integer> getObjectClassIdsByTable(String tableName) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		List<Integer> objectclassIds = new ArrayList<Integer>();	
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select id from " + schema + ".objectclass where tablename = '" + tableName + "'");		
			while (rs.next()) 
				objectclassIds.add(rs.getInt(1));	
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}
		
		for (PropertyInjection injection: mergedSchemaMapping.getPropertyInjections()) {
			FeatureType baseType = injection.getDefaultBase();
			if (tableName.equalsIgnoreCase(injection.getTable())) 
				objectclassIds.add(baseType.getObjectClassId());
		}	
		
		return objectclassIds;
	}

	public boolean checkTableExists(String tableName) throws SQLException {
		StringBuilder query = new StringBuilder();
		DatabaseType dbType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();
						
		if (dbType == DatabaseType.ORACLE) {
			query.append("select * from all_tables where table_name = upper('")
			.append(tableName.toLowerCase()).append("') and owner = upper('").append(schema).append("')");
		}
		else if (dbType == DatabaseType.POSTGIS){
			query.append("select * from information_schema.tables where table_name = '")
			.append(tableName.toLowerCase()).append("' and table_schema  = '").append(schema).append("'");
		}
		
		Statement stmt = null;
		ResultSet rs = null;
		boolean tableExists = false;
	
		try {						
			stmt = connection.createStatement();
			rs = stmt.executeQuery(query.toString());		
			if (rs.next())
				tableExists = true;
		} finally {
			if (rs != null) 
				rs.close();
			
			if (stmt != null) 
				stmt.close();
		}
		
		return tableExists;
	}
		
	public List<String> getAggregationJoinColumns(String tableName) throws SQLException {
		StringBuilder query = new StringBuilder();
		List<String> columnList = new ArrayList<String>();
		
		query.append("select join_table_or_column_name from ").append(schema).append(".aggregation_info left join ")
		.append(schema).append(".objectclass on id = child_id where upper(tablename) = upper('").append(tableName).append("')")
		.append(" and upper(join_table_or_column_name) like '%_ID' and max_occurs is null");
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {						
			stmt = connection.createStatement();
			rs = stmt.executeQuery(query.toString());		
			while (rs.next()) 
				columnList.add(rs.getString(1));
		} finally {
			if (rs != null) 
				rs.close();
			
			if (stmt != null) 
				stmt.close();
		}
		
		return columnList;
	}

	private AggregationInfoCollection queryAggregationInfoCollection() throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		AggregationInfoCollection aggrInfos = new AggregationInfoCollection(this);
		
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("select ")
				      .append("child_id, ")
				      .append("parent_id, ")
				      .append("min_occurs, ")
				      .append("max_occurs, ")
				      .append("is_composite, ")
				      .append("join_table_or_column_name ")
				  .append("FROM ")
				      .append(schema).append(".aggregation_info a ");
	
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();						
			while (rs.next()) {
				int childClassId = rs.getInt(1);
				int parentClassId = rs.getInt(2);
				int minOccurs = rs.getInt(3);
				if (rs.wasNull())
					minOccurs = 0;			
				int maxOccurs = rs.getInt(4);				
				if (rs.wasNull())
					maxOccurs = -1;
				boolean isComposite = (rs.getInt(5) == 1);
				String joinTableOrColumnName = rs.getString(6);
				AggregationInfo aggrInfo = new AggregationInfo(childClassId, parentClassId, minOccurs, maxOccurs, isComposite, joinTableOrColumnName);
				aggrInfos.addAggregationInfo(aggrInfo);
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
		
		return aggrInfos;
	}

	private SchemaMapping queryMergedADESchemaMapping() throws SQLException {
		SchemaMapping schemaMapping = null;
		try {
			schemaMapping = SchemaMappingUtil.getInstance().unmarshal(CoreConstants.CITYDB_SCHEMA_MAPPING_FILE);
		} catch (SchemaMappingException | SchemaMappingValidationException | JAXBException e) {
			throw new SQLException("Failed to read the default 3DCityDB schema mapping file.", e);
		};
		
		List<ADEMetadataInfo> ades = getADEMetadata();
		for (ADEMetadataInfo ade: ades) {
			String dbPrefix = ade.getDbPrefix();
			String schemaMappingStr = this.getSchemaMappingbyDbPrefix(dbPrefix);
			if (schemaMappingStr != null) {
				InputStream stream = new ByteArrayInputStream(schemaMappingStr.getBytes());
				try {			
					SchemaMapping adeSchemaMapping = SchemaMappingUtil.getInstance().unmarshal(schemaMapping, stream);	
					schemaMapping.merge(adeSchemaMapping);
				} catch (SchemaMappingException | SchemaMappingValidationException | JAXBException e) {
					throw new SQLException("Failed to read the schema mapping file of the ADE "+ ade.getName() + " from 3DCityDB.", e);
				}
			}
		}
		
		return schemaMapping;
	}

	private long insertADE(SchemaMapping inputADESchemaMapping, Path schemaMappingFile, Path adeDropDBFilePath, PreparedStatement ps) throws SQLException {				
		long seqId = getSequenceID(ADEMetadataSequence.ade_seq);
			
		int index = 1;
		ps.setLong(index++, seqId);
		ps.setString(index++, createMD5Fingerprint(schemaMappingFile));
		ps.setString(index++, inputADESchemaMapping.getMetadata().getName());
		ps.setString(index++, inputADESchemaMapping.getMetadata().getDescription());
		ps.setString(index++, inputADESchemaMapping.getMetadata().getVersion());
		ps.setString(index++, inputADESchemaMapping.getMetadata().getDBPrefix());
		
		String schemaMappingText = getStringFromFile(schemaMappingFile);
		if (schemaMappingText != null)
			ps.setString(index++, schemaMappingText);
		else {
			ps.setNull(index++, Types.CLOB);
		}
		
		String dropDBText = getStringFromFile(adeDropDBFilePath);
		if (dropDBText != null)
			ps.setString(index++, dropDBText);
		else {
			ps.setNull(index++, Types.CLOB);
		}
		
		Date creationDate = new Date();
		ps.setTimestamp(index++, new Timestamp(creationDate.getTime()));

		String databaseUser = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getConnectionDetails().getUser();
		ps.setString(index++, databaseUser);
		
		ps.executeUpdate();
		
		return seqId;
	}
	
	private Map<String, List<Long>> insertSchemas(SchemaMapping inputADESchemaMapping, long adeRowId, List<String> adeSchemaIds, PreparedStatement ps) throws SQLException {				
		Map<String, List<Long>> insertedSchemas = new HashMap<String, List<Long>>();

		Iterator<AppSchema> schemaIter = inputADESchemaMapping.getSchemas().iterator();		
		while (schemaIter.hasNext()) {
			AppSchema adeSchema = schemaIter.next();			
	
			if (!adeSchemaIds.contains(adeSchema.getId()))
				continue;
			
			List<Long> insertedIds = new ArrayList<Long>();
			Iterator<Namespace> namespaceIter = adeSchema.getNamespaces().iterator();
			while (namespaceIter.hasNext()) {
				Namespace adeNamespace = namespaceIter.next();
	
				long seqId = getSequenceID(ADEMetadataSequence.schema_seq);
				insertedIds.add(seqId);

				int index = 1;
				ps.setLong(index++, seqId);
				ps.setInt(index++, adeSchema.isADERoot()?1:0);
				ps.setString(index++, adeNamespace.getContext().name());
				ps.setString(index++, adeNamespace.getURI());
				ps.setString(index++, adeSchema.getId());
				ps.setNull(index++, Types.VARCHAR);
				ps.setObject(index++, null);
				ps.setNull(index++, Types.VARCHAR);
				ps.setLong(index++, adeRowId);
				
				ps.executeUpdate();
			}					
			insertedSchemas.put(adeSchema.getId(), insertedIds);			
		}	
		
		return insertedSchemas;
	}
	
	private void insertSchemaReferencing(SchemaMapping inputADESchemaMapping, Map<String, List<Long>> insertedSchemas, String adeRootSchemaId, PreparedStatement ps) throws SQLException {	
		int numberOfCityGMLversions = inputADESchemaMapping.getSchemas().get(0).getNamespaces().size();
		for (int i = 0; i < numberOfCityGMLversions; i++) {
			long referencingId = insertedSchemas.get(adeRootSchemaId).get(i);
			
			Iterator<String> iter = insertedSchemas.keySet().iterator();
		    while (iter.hasNext()) {
		        String schemaId = iter.next();
		        if (!schemaId.equalsIgnoreCase(adeRootSchemaId)) {
		        	long referencedId = insertedSchemas.get(schemaId).get(i);			        	
		        	ps.setLong(1, referencedId);	
		        	ps.setLong(2, referencingId);		        	
		        	ps.executeUpdate();
		        }		        
		    }
		}
	}

	private void insertSchemaToObjectclass(Map<Long, String> objectObjectclassIds, Map<String, List<Long>> insertedSchemas, PreparedStatement ps) throws SQLException {
		Iterator<Long> objectclassIter = objectObjectclassIds.keySet().iterator();
	    while (objectclassIter.hasNext()) {
	        long objectclassId = objectclassIter.next();
	        String schemaId = objectObjectclassIds.get(objectclassId);
	        
	        List<Long> insertedSchemaIds = insertedSchemas.get(schemaId);
	        Iterator<Long> schemaIter = insertedSchemaIds.iterator();
	        while (schemaIter.hasNext()) {
	        	long insertedSchemaId = schemaIter.next();		
	        	ps.setLong(1, insertedSchemaId);
	        	ps.setLong(2, objectclassId);	
	            ps.executeUpdate();
	        }
	    }
	}
	
	private Map<Long, String> insertObjectclasses(SchemaMapping inputADESchemaMapping, long insertedADERowId, PreparedStatement ps) throws SQLException {		
		Map<Long, String> insertedObjectclasses = new HashMap<Long, String>();
		
		Iterator<AbstractType<?>> objectIter = inputADESchemaMapping.getAbstractTypes().iterator();		
		while (objectIter.hasNext()) {
			AbstractType<?> objectClass = objectIter.next();	
			if (!insertedObjectclasses.containsKey((long)objectClass.getObjectClassId()))
				insertSingleObjectclass(objectClass, insertedObjectclasses, insertedADERowId, ps);										
		}

		return insertedObjectclasses;
	}
	
	private void insertSingleObjectclass(AbstractType<?> objectClass, Map<Long, String> insertedObjectclasses, long insertedADERowId, PreparedStatement ps) throws SQLException {
		long objectclassId = objectClass.getObjectClassId();	
		Integer superclassId = null;	
		
		AbstractExtension<?> objectExtension = objectClass.getExtension();
		if (objectExtension != null) {
			AbstractType<?> superType = (AbstractType<?>) objectExtension.getBase();
			superclassId = superType.getObjectClassId();	
			if (superclassId >= GlobalConstants.MIN_ADE_OBJECTCLASSID && !insertedObjectclasses.containsKey((long)superclassId))
				insertSingleObjectclass(superType, insertedObjectclasses, insertedADERowId, ps);
		}
		
		int topLevel = 0;
		if (objectClass instanceof FeatureType) {
			if (((FeatureType) objectClass).isTopLevel())
				topLevel = 1;
		}

		int index = 1;		
		ps.setLong(index++, objectclassId);
		ps.setInt(index++, 1);
		ps.setInt(index++, topLevel);
		ps.setString(index++, objectClass.getPath());
		ps.setString(index++, objectClass.getTable());		
		
		if (superclassId != null) {	
			int baseclassId = getBaseclassId(objectClass);	
			ps.setInt(index++, superclassId);
			ps.setInt(index++, baseclassId);
		}	
		else {
			if (objectClass instanceof ComplexType) {
				ps.setInt(index++, 0);
				ps.setInt(index++, 0);
			}
			else if (objectClass instanceof ObjectType) {
				ps.setInt(index++, 1);
				ps.setInt(index++, 1);
			}
			else {
				ps.setInt(index++, 2);
				ps.setInt(index++, 2);
			}			
		}	
				
		ps.setLong(index++, insertedADERowId);
		ps.executeUpdate();			

		insertedObjectclasses.put(objectclassId, objectClass.getSchema().getId());	
	}
	
	private void insertAggregationInfo(SchemaMapping inputADESchemaMapping, PreparedStatement ps) throws SQLException {		
		AggregationInfoCollection insertedAggregationinfo = new AggregationInfoCollection(this);		
		for (AbstractType<?> objectclass: inputADESchemaMapping.getAbstractTypes()) {
			int parentClassId = objectclass.getObjectClassId();	
			for (AbstractProperty property: objectclass.getProperties()) 
				insertSingleAggregationInfo(parentClassId, property, insertedAggregationinfo, ps);
		}	
		for (PropertyInjection injection: inputADESchemaMapping.getPropertyInjections()) {
			int parentClassId = injection.getDefaultBase().getObjectClassId();
			for (InjectedProperty property: injection.getProperties()) 
				insertSingleAggregationInfo(parentClassId, (AbstractProperty)property, insertedAggregationinfo, ps);
		}		
	}
	
	private void insertSingleAggregationInfo(int parentClassId, AbstractProperty property, 
			AggregationInfoCollection insertedAggregationinfo, PreparedStatement ps) throws SQLException {	
		int childClassId = 0;
		RelationType relationType = null;
		String join_table_or_column = null;
		String treeHierarchyRootColumn = null;
		int minOccurs = property.getMinOccurs();
		Integer maxOccurs = property.getMaxOccurs();
		if (property instanceof AbstractTypeProperty) {
			AbstractJoin joiner = property.getJoin();
			if (joiner != null) {
				AbstractType<?> objectclass = ((AbstractTypeProperty<?>) property).getType();
				childClassId = objectclass.getObjectClassId();
				if (objectclass instanceof ComplexType)				
					relationType = RelationType.COMPOSITION;
				else
					relationType = ((AbstractTypeProperty<?>) property).getRelationType();

				if (joiner instanceof Join) {
					if (((Join) joiner).getToRole() == TableRole.CHILD)
						join_table_or_column = ((Join) joiner).getToColumn();
					else
						join_table_or_column = ((Join) joiner).getFromColumn();	
					
					TreeHierarchy treeHierarchy = ((Join) joiner).getTreeHierarchy();
					if (treeHierarchy != null)
						treeHierarchyRootColumn = treeHierarchy.getRootColumn();
				}
				else if (joiner instanceof JoinTable)
					join_table_or_column = ((JoinTable) joiner).getTable();
			}			
		}
		else if (property instanceof GeometryProperty) {
			String refColumn = ((GeometryProperty) property).getRefColumn();
			if (refColumn != null) {
				childClassId = GlobalConstants.SURFACE_GEOMETRY_OBJECTCLASSID; // surface geometry
				relationType = RelationType.COMPOSITION;
				join_table_or_column = refColumn;
			}
		}
		else if (property instanceof ImplicitGeometryProperty) {
			childClassId = GlobalConstants.IMPLICIT_GEOMETRY_OBJECTCLASSID; // implicit geometry
			relationType = RelationType.AGGREGATION;
			int lod = ((ImplicitGeometryProperty) property).getLod();
			join_table_or_column = "lod" + lod + "_implicit_rep_id";
		}
		if (relationType != null && relationType != RelationType.ASSOCIATION && insertedAggregationinfo.get(childClassId, parentClassId, join_table_or_column) == null) {
			insertSingleAggregationInfo(
					childClassId, 
					parentClassId, 
					minOccurs, 
					maxOccurs, 
					relationType, 
					join_table_or_column,
					insertedAggregationinfo,
					ps);
			if (treeHierarchyRootColumn != null) {
				insertSingleAggregationInfo(
						childClassId, 
						parentClassId, 
						minOccurs, 
						maxOccurs, 
						relationType, 
						treeHierarchyRootColumn,
						insertedAggregationinfo,
						ps);
			}
		}

		
	}
	
	private void insertSingleAggregationInfo(
			int childClassId, 
			int parentClassId, 
			int minOccurs, 
			Integer maxOccurs,
			RelationType relationType, 
			String join_table_or_column,
			AggregationInfoCollection insertedAggregationinfo,
			PreparedStatement ps) throws SQLException {
		int index = 1;	
		if (childClassId < GlobalConstants.MIN_ADE_OBJECTCLASSID && parentClassId < GlobalConstants.MIN_ADE_OBJECTCLASSID)
			return;
		
		ps.setInt(index++, childClassId);
		ps.setInt(index++, parentClassId);
		ps.setInt(index++, minOccurs);
		if (maxOccurs != null)
			ps.setInt(index++, maxOccurs);
		else
			ps.setNull(index++, Types.INTEGER);
		if (relationType == RelationType.COMPOSITION)
			ps.setInt(index++, 1);
		else
			ps.setInt(index++, 0);		
		ps.setString(index++, join_table_or_column);
		
		ps.executeUpdate();	
		
		insertedAggregationinfo.addAggregationInfo(
			new AggregationInfo(
					childClassId, 
					parentClassId, 
					minOccurs, 
					maxOccurs, 
					relationType == RelationType.COMPOSITION, 
					join_table_or_column)
		);				
	
	}

	private long getSequenceID(ADEMetadataSequence seqType) throws SQLException {
		StringBuilder query = new StringBuilder();
		DatabaseType dbType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();
						
		if (dbType == DatabaseType.ORACLE) {
			query.append("select ").append(schema).append(".").append(seqType).append(".nextval from dual");
		}
		else if (dbType == DatabaseType.POSTGIS){
			query.append("select nextval('").append(schema).append(".").append(seqType).append("')");
		}
		
		long id = 0;
		PreparedStatement pstsmt = null;
		ResultSet rs = null;

		try {
			pstsmt = connection.prepareStatement(query.toString());
			rs = pstsmt.executeQuery();
			
			if (rs.next())
				id = rs.getLong(1);						
		} finally {			
			if (rs != null) 
				rs.close();
			
			if (pstsmt != null) 
				pstsmt.close(); 
		}
	
		return id;
	}
	
	private int getBaseclassId(AbstractType<?> objectType) {
		int objectclassId = objectType.getObjectClassId();
		
		if (objectclassId <= 3)
			return objectclassId;
		else {
			if (objectType.getExtension() != null) {
				return getBaseclassId((AbstractType<?>)objectType.getExtension().getBase());
			}
			else
				return objectclassId;
		}		
	}
	
	private String getStringFromFile(Path schemaMappingFile) {
		try {
			return new String(Files.readAllBytes(schemaMappingFile));
		} catch (IOException e) {
			return null;
		}	
	}
	
	private String getSchemaMappingbyDbPrefix(String dbPrefix) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		String schemaMappingStr = null;
	
		try {						
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select xml_schemamapping_file from " + schema + ".ade where db_prefix = '" + dbPrefix + "'");		
			if (rs.next())
				schemaMappingStr = rs.getString(1);
		} finally {
			if (rs != null) 
				rs.close();
			
			if (stmt != null) 
				stmt.close();
		}
	
		return schemaMappingStr;
	}

	private boolean validateObjectclassId (int objectclassId) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		boolean isValid = true;
	
		try {						
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select * from " + schema + ".objectclass where id = " + objectclassId);		
			if (rs.next())
				isValid = false;
		} finally {
			if (rs != null) 
				rs.close();
			
			if (stmt != null) 
				stmt.close();
		}
		
		return isValid;
	}

	private boolean validateDBPrefix (String dbPrefix) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		boolean isValid = true;
	
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select * from " + schema + ".ade where db_prefix = '" + dbPrefix + "'");
			
			if (rs.next())
				isValid = false;
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}
		
		return isValid;
	}

	private void validateSchemaMapping(SchemaMapping schemaMapping) throws SQLException {
		String dbPrefix = schemaMapping.getMetadata().getDBPrefix();
		if (!validateDBPrefix(dbPrefix))
			throw new SQLException("The database prefix '" + dbPrefix + "'" + " is invalid because it is already used by another registered ADE.");
		
		Iterator<AbstractObjectType<?>> iter = schemaMapping.getAbstractObjectTypes().iterator();
		while (iter.hasNext()) {
			AbstractObjectType<?> objectclass = iter.next();
			int objectclassId = objectclass.getObjectClassId();
			
			if (!validateObjectclassId(objectclassId))
				throw new SQLException("The object class ID '" + objectclassId + "'" + " is invalid because it is already used by another class.");
		}
	}

	private String createMD5Fingerprint(Path schemaMappingFile) throws SQLException {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] hash = md5.digest(Files.readAllBytes(schemaMappingFile));
			StringBuffer hex = new StringBuffer();
			for (int i = 0; i < hash.length; i++)
				hex.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
			
			return hex.toString();
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new SQLException("Failed to create fingerpint for ADE schema-mapping file.", e);
		} 	
	}

}
