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
import javax.xml.namespace.QName;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.AbstractExtension;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.AppSchema;
import org.citydb.database.schema.mapping.ComplexProperty;
import org.citydb.database.schema.mapping.ComplexType;
import org.citydb.database.schema.mapping.FeatureProperty;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.InjectedProperty;
import org.citydb.database.schema.mapping.Namespace;
import org.citydb.database.schema.mapping.ObjectProperty;
import org.citydb.database.schema.mapping.ObjectType;
import org.citydb.database.schema.mapping.PropertyInjection;
import org.citydb.database.schema.mapping.RelationType;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.database.schema.mapping.SchemaMappingException;
import org.citydb.database.schema.mapping.SchemaMappingValidationException;
import org.citydb.database.schema.util.SchemaMappingUtil;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.util.PathResolver;
import org.citydb.util.CoreConstants;

public class ADEMetadataManager {	
	public static final int MIN_ADE_OBJECTCLASSID = 10000;
	private SchemaMapping adeSchemaMapping;	
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	private final Connection connection;
	private final ConfigImpl config;
	private String schema;
	
	public ADEMetadataManager(Connection connection, ConfigImpl config) {
		this.connection = connection;
		this.config = config;
		this.schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
	}
	
	public void importADEMetadata() throws SQLException {				
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		Path adeSchemaMappingFilePath = Paths.get(PathResolver.get_schemaMapping_filepath(adeRegistryInputpath));
		Path adeDropDBFilePath = Paths.get(PathResolver.get_drop_ade_db_filepath(adeRegistryInputpath, databaseType));
		
		try {			
			SchemaMapping citydbSchemaMapping = SchemaMappingUtil.getInstance().unmarshal(CoreConstants.CITYDB_SCHEMA_MAPPING_FILE);
			this.adeSchemaMapping = SchemaMappingUtil.getInstance().unmarshal(citydbSchemaMapping, adeSchemaMappingFilePath.toFile());	
		} catch (JAXBException e) {
			throw new SQLException(e);
		} catch (SchemaMappingException | SchemaMappingValidationException e) {
			throw new SQLException("The 3DCityDB schema mapping is invalid", e);
		} 

		try {
			validateSchemaMapping(adeSchemaMapping);
		} catch (SQLException e) {
			throw new SQLException("Failed to read and process objectclass Ids, Aborting.", e);
		}
		
		List<String> adeSchemaIds = new ArrayList<String>();	
		String adeRootSchemaId = null;
		Iterator<AppSchema> adeSchemas = adeSchemaMapping.getSchemas().iterator();
		while (adeSchemas.hasNext()) {
			AppSchema adeSchema = adeSchemas.next();
			adeSchemaIds.add(adeSchema.getId());
			if (adeSchema.isADERoot())
				adeRootSchemaId = adeSchema.getId();
		}		
		
		if (adeRootSchemaId == null)
			throw new SQLException("Failed to import metadata, Cause: An ADE must have a root schema");

		long insertedADERowId;
		String insertADEQueryStr = "INSERT INTO " + schema + ".ADE"
				+ "(ID, ADEID, NAME, DESCRIPTION, VERSION, DB_PREFIX, XML_SCHEMAMAPPING_FILE, DROP_DB_SCRIPT, CREATION_DATE, CREATION_PERSON) VALUES"
				+ "(?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement psInsertADE = connection.prepareStatement(insertADEQueryStr);
		try {
			insertedADERowId = insertADE(adeSchemaMappingFilePath, adeDropDBFilePath, psInsertADE);			
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
			insertedSchemas = insertSchemas(insertedADERowId, adeSchemaIds, psInsertSchema);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA' table.", e);
		} finally {
			psInsertSchema.close();			
		}
		
		String insertSchemaReferencingQueryString = "INSERT INTO "  + schema + ".schema_referencing(REFERENCED_ID, REFERENCING_ID) VALUES(?,?)";
		PreparedStatement psInsertSchemaReferencing = connection.prepareStatement(insertSchemaReferencingQueryString);
		try {
			insertSchemaReferencing(insertedSchemas, adeRootSchemaId, psInsertSchemaReferencing);		
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA_REFERENCING' table.", e);
		} finally {
			psInsertSchemaReferencing.close();			
		}
		
		Map<Long, String> objectObjectclassIds;
		String insertObjectclassQueryString = "INSERT INTO "  + schema + ".OBJECTCLASS (ID, IS_ADE_CLASS, IS_TOPLEVEL, CLASSNAME, TABLENAME, SUPERCLASS_ID, BASECLASS_ID, ADE_ID) VALUES(?,?,?,?,?,?,?,?)";
		PreparedStatement ps = connection.prepareStatement(insertObjectclassQueryString);
		try {			
			objectObjectclassIds = insertObjectclasses(insertedADERowId, ps);			
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
		
		String insertAggregationInfoQueryString = "INSERT INTO " + schema + ".aggregation_info(CHILD_ID, PARENT_ID, MIN_OCCURS, MAX_OCCURS, IS_COMPOSITE) VALUES(?,?,?,?,?)";
		PreparedStatement psInsertAggregationInfo = connection.prepareStatement(insertAggregationInfoQueryString);
		try {
			insertAggregationInfo(psInsertAggregationInfo);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA_TO_OBJECTCLASS' table.", e);
		} finally {
			psInsertSchemaToObjectclass.close();
		}
	}

	public List<ADEMetadataInfo> queryADEMetadata() throws SQLException {
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
	
	public Map<Integer, String> querySubObjectclassesFromSuperTable(String superTable) throws SQLException {
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
					Map<Integer, String> nestedSub = querySubObjectclassesFromSuperTable(tableName);
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

	public Map<QName, AggregationInfo> queryAggregationInfo() throws SQLException {
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
		Map<QName, AggregationInfo> result = new HashMap<QName, AggregationInfo>();
	
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("select distinct ")
				      .append("c.tablename as child_table_name, ")
				      .append("p.tablename as parent_table_name, ")
				      .append("a.min_occurs as min_occurs, ")
				      .append("a.max_occurs as max_occurs, ")
				      .append("a.is_composite as is_composite ")
				  .append("FROM ")
				      .append(schema).append(".aggregation_info a ")
				  .append("JOIN ")
				  .append(schema).append(".objectclass c ")
				      .append("on a.child_id = c.id ")
				  .append("JOIN ")
				  	.append(schema).append(".objectclass p ")
				    .append("on a.parent_id = p.id ");

		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();						
			while (rs.next()) {
				String childTable = rs.getString(1);
				String parentTable = rs.getString(2);
				int minOccurs = rs.getInt(3);
				if (rs.wasNull())
					minOccurs = 0;			
				int maxOccurs = rs.getInt(4);				
				if (rs.wasNull())
					maxOccurs = -1;
				boolean isComposite = (rs.getInt(5) == 1);
				AggregationInfo aggrInfo = new AggregationInfo(childTable, parentTable, minOccurs, maxOccurs, isComposite);
				result.put(new QName(childTable, parentTable), aggrInfo);
				
				List<String> adeHookTables = this.getADEHookTables(parentTable);
				for (String adeHookTable: adeHookTables) {
					result.put(new QName(childTable, adeHookTable), aggrInfo);
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
		}
		
		return result;
	}
	
	public List<String> getADEHookTables(String baseTableName) throws SQLException {
		List<String> result = new ArrayList<String>();
		List<ADEMetadataInfo> ades = queryADEMetadata();
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
			throw new SQLException("The script for dropping ADE database schema is not available");
		
		return dropDBScript;
	}

	private long insertADE(Path schemaMappingFile, Path adeDropDBFilePath, PreparedStatement ps) throws SQLException {				
		long seqId = getSequenceID(ADEMetadataSequence.ade_seq);
			
		int index = 1;
		ps.setLong(index++, seqId);
		ps.setString(index++, createMD5Fingerprint(schemaMappingFile));
		ps.setString(index++, adeSchemaMapping.getMetadata().getName());
		ps.setString(index++, adeSchemaMapping.getMetadata().getDescription());
		ps.setString(index++, adeSchemaMapping.getMetadata().getVersion());
		ps.setString(index++, adeSchemaMapping.getMetadata().getDBPrefix());
		
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
	
	private Map<String, List<Long>> insertSchemas(long adeRowId, List<String> adeSchemaIds, PreparedStatement ps) throws SQLException {				
		Map<String, List<Long>> insertedSchemas = new HashMap<String, List<Long>>();

		Iterator<AppSchema> schemaIter = adeSchemaMapping.getSchemas().iterator();		
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
	
	private void insertSchemaReferencing(Map<String, List<Long>> insertedSchemas, String adeRootSchemaId, PreparedStatement ps) throws SQLException {	
		int numberOfCityGMLversions = adeSchemaMapping.getSchemas().get(0).getNamespaces().size();
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
	
	private Map<Long, String> insertObjectclasses(long insertedADERowId, PreparedStatement ps) throws SQLException {		
		Map<Long, String> insertedObjectclasses = new HashMap<Long, String>();
		
		Iterator<AbstractType<?>> objectIter = adeSchemaMapping.getAbstractTypes().iterator();		
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
			if (superclassId >= MIN_ADE_OBJECTCLASSID && !insertedObjectclasses.containsKey((long)superclassId))
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
	
	private void insertAggregationInfo(PreparedStatement ps) throws SQLException {		
		List<QName> insertedAggregationinfo = new ArrayList<QName>();		
		for (AbstractType<?> objectclass: adeSchemaMapping.getAbstractTypes()) {
			int parentId = objectclass.getObjectClassId();	
			for (AbstractProperty property: objectclass.getProperties()) {
				insertSingleAggregationInfo(parentId, property, insertedAggregationinfo, ps);
			}			
		}	
		for (PropertyInjection injection: adeSchemaMapping.getPropertyInjections()) {
			int parentId = injection.getDefaultBase().getObjectClassId();
			for (InjectedProperty property: injection.getProperties()) {
				insertSingleAggregationInfo(parentId, (AbstractProperty)property, insertedAggregationinfo, ps);
			}			
		}		
	}
	
	private void insertSingleAggregationInfo(int parentId, AbstractProperty property, 
			List<QName> insertedAggregationinfo, PreparedStatement ps) throws SQLException {	
		int childId = 0;
		RelationType relationType = RelationType.ASSOCIATION;
		if (property instanceof ComplexProperty) {
			childId = ((ComplexProperty) property).getType().getObjectClassId();
			relationType = RelationType.COMPOSITION;
		}
		else if (property instanceof FeatureProperty) {
			childId = ((FeatureProperty) property).getType().getObjectClassId();
			relationType = ((FeatureProperty) property).getRelationType(); 
		}
		else if (property instanceof ObjectProperty) {
			childId = ((ObjectProperty) property).getType().getObjectClassId();
			relationType = ((ObjectProperty) property).getRelationType(); 
		}
		else if (property instanceof GeometryProperty) {
			String refColumn = ((GeometryProperty) property).getRefColumn();
			if (refColumn != null) {
				childId = 106; // surface geometry
				relationType = RelationType.COMPOSITION;
			}
		}
		else if (property instanceof ImplicitGeometryProperty) {
			childId = 59; // surface geometry
			relationType = RelationType.AGGREGATION;
		}
		
		QName key = new QName(String.valueOf(childId), String.valueOf(parentId));
		if (childId != 0 && relationType != RelationType.ASSOCIATION && !insertedAggregationinfo.contains(key)) {
			int index = 1;
			int minOccurs = property.getMinOccurs();
			Integer maxOccurs = property.getMaxOccurs();
			ps.setInt(index++, childId);
			ps.setInt(index++, parentId);
			ps.setInt(index++, minOccurs);
			if (maxOccurs != null)
				ps.setInt(index++, maxOccurs);
			else
				ps.setNull(index++, Types.INTEGER);
			if (relationType == RelationType.COMPOSITION)
				ps.setInt(index++, 1);
			else
				ps.setInt(index++, 0);	
			
			insertedAggregationinfo.add(key);
			ps.executeUpdate();		
		}	
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
			throw new SQLException("The DB_Prefix '" + dbPrefix + "'" + " is invalid, because it has already been reserved by other registered ADEs");		
		
		Iterator<AbstractObjectType<?>> iter = schemaMapping.getAbstractObjectTypes().iterator();
		while (iter.hasNext()) {
			AbstractObjectType<?> objectclass = iter.next();
			int objectclassId = objectclass.getObjectClassId();
			
			if (!validateObjectclassId(objectclassId))
				throw new SQLException("The objectclass Id '" + objectclassId + "'" + " is invalid, because it has already been reserved by other class");							
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
			throw new SQLException("Failed to create fingerpint for ADE schema-mapping file", e);
		} 	
	}

}
