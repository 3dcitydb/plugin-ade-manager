package org.citydb.plugins.ade_manager.registry.metadata;

import java.io.IOException;
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.AbstractExtension;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.database.schema.mapping.AppSchema;
import org.citydb.database.schema.mapping.Namespace;
import org.citydb.database.schema.mapping.ObjectType;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.database.schema.mapping.SchemaMappingException;
import org.citydb.database.schema.mapping.SchemaMappingValidationException;
import org.citydb.database.schema.util.SchemaMappingUtil;
import org.citydb.plugins.ade_manager.registry.ADERegistrationImpl;
import org.citydb.util.CoreConstants;

public class ADEMetadataManager extends ADERegistrationImpl {	
	public static final int MIN_ADE_OBJECTCLASSID = 10000;
	private SchemaMapping adeSchemaMapping;	
	
	public ADEMetadataManager(Connection connection) {
		this.connection = connection;
	}
	
	public void importADEMetadata(String adeSchemaMappingFilepathStr) throws SQLException {		
		// read ADE's schema mapping file
		Path adeSchemaMappingFilePath = Paths.get(adeSchemaMappingFilepathStr);

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
		String insertADEQueryStr = "INSERT INTO ADE"
				+ "(ID, ADEID, NAME, DESCRIPTION, VERSION, DB_PREFIX, XML_SCHEMAMAPPING_FILE, DROP_DB_SCRIPT, CREATION_DATE, CREATION_PERSON) VALUES"
				+ "(?,?,?,?,?,?,?,?,?,?)";
		PreparedStatement psInsertADE = connection.prepareStatement(insertADEQueryStr);
		try {
			insertedADERowId = insertADE(adeSchemaMappingFilePath, psInsertADE);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'ADE' table.", e);
		} finally {
			psInsertADE.close();			
		}
				
		Map<String, List<Long>> insertedSchemas = null;
		String insertSchemaQueryStr = "INSERT INTO SCHEMA"
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
		
		String insertSchemaReferencingQueryString = "INSERT INTO schema_referencing" + "(REFERENCED_ID, REFERENCING_ID) VALUES" + "(?,?)";
		PreparedStatement psInsertSchemaReferencing = connection.prepareStatement(insertSchemaReferencingQueryString);
		try {
			insertSchemaReferencing(insertedSchemas, adeRootSchemaId, psInsertSchemaReferencing);		
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA_REFERENCING' table.", e);
		} finally {
			psInsertSchemaReferencing.close();			
		}
		
		Map<Long, String> objectObjectclassIds;
		String insertObjectclassQueryString = "INSERT INTO OBJECTCLASS" + "(ID, IS_ADE_CLASS, CLASSNAME, TABLENAME, SUPERCLASS_ID, BASECLASS_ID, ADE_ID) VALUES" + "(?,?,?,?,?,?,?)";
		PreparedStatement ps = connection.prepareStatement(insertObjectclassQueryString);
		try {			
			objectObjectclassIds = insertObjectclasses(insertedADERowId, ps);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'OBJECTCLASS' table.", e);
		} finally {
			ps.close();			
		}
		
		String insertSchemaToObjectclassQueryString = "INSERT INTO schema_to_objectclass" + "(SCHEMA_ID, OBJECTCLASS_ID) VALUES" + "(?,?)";
		PreparedStatement psInsertSchemaToObjectclass = connection.prepareStatement(insertSchemaToObjectclassQueryString);
		try {
			insertSchemaToObjectclass(objectObjectclassIds, insertedSchemas, psInsertSchemaToObjectclass);			
		} catch (SQLException e) {
			throw new SQLException("Failed to import metadata into 'SCHEMA_TO_OBJECTCLASS' table.", e);
		} finally {
			psInsertSchemaToObjectclass.close();
		}
	}

	public List<ADEMetadataEntity> queryADEMetadata() throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		ArrayList<ADEMetadataEntity> ades = new ArrayList<ADEMetadataEntity>();
	
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select * from ade order by id");
			
			while (rs.next()) {
				String adeid = rs.getString(2);
				String name = rs.getString(3);
				String description = rs.getString(4);
				String version = rs.getString(5);
				String dbPrefix = rs.getString(6);
				ades.add(new ADEMetadataEntity(adeid, name, description, version, dbPrefix));					
			}
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}
	
		return ades;
	}

	public void deleteADEMetadata(String adeId) throws SQLException {
		Statement stmt = null;	
		try {					
			stmt = connection.createStatement();
			stmt.executeUpdate("Delete from ade where adeid = '" + adeId + "'");
		} finally {
			if (stmt != null)
				stmt.close();
		}
	}

	private long insertADE(Path schemaMappingFile, PreparedStatement ps) throws SQLException {				
		long seqId = getSequenceID(ADEMetadataSequence.ade_seq);
			
		int index = 1;
		ps.setLong(index++, seqId);
		ps.setString(index++, createMD5Fingerprint(schemaMappingFile));
		ps.setString(index++, adeSchemaMapping.getMetadata().getName());
		ps.setString(index++, adeSchemaMapping.getMetadata().getDescription());
		ps.setString(index++, adeSchemaMapping.getMetadata().getVersion());
		ps.setString(index++, adeSchemaMapping.getMetadata().getDBPrefix());
		
		String mappingText = getSchemaMappingAsString(schemaMappingFile);
		if (mappingText != null)
			ps.setString(index++, mappingText);
		else {
			ps.setNull(index++, Types.CLOB);
		}
		
		ps.setNull(index++, Types.CLOB);
		ps.setNull(index++, Types.DATE);
		ps.setNull(index++, Types.VARCHAR);
		
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
		
		Iterator<AbstractObjectType<?>> objectIter = adeSchemaMapping.getAbstractObjectTypes().iterator();		
		while (objectIter.hasNext()) {
			AbstractObjectType<?> objectClass = objectIter.next();	
			if (!insertedObjectclasses.containsKey((long)objectClass.getObjectClassId()))
				insertSingleObjectclass(objectClass, insertedObjectclasses, insertedADERowId, ps);										
		}
		
		return insertedObjectclasses;
	}
	
	private void insertSingleObjectclass(AbstractObjectType<?> objectClass, Map<Long, String> insertedObjectclasses, long insertedADERowId, PreparedStatement ps) throws SQLException {
		long objectclassId = objectClass.getObjectClassId();	
		Integer superclassId = null;	
		
		AbstractExtension<?> objectExtension = objectClass.getExtension();
		if (objectExtension != null) {
			AbstractObjectType<?> superType = (AbstractObjectType<?>) objectExtension.getBase();
			superclassId = superType.getObjectClassId();	
			if (superclassId >= MIN_ADE_OBJECTCLASSID && !insertedObjectclasses.containsKey((long)superclassId))
				insertSingleObjectclass(superType, insertedObjectclasses, insertedADERowId, ps);
		}

		int index = 1;		
		ps.setLong(index++, objectclassId);
		ps.setInt(index++, 1);
		ps.setString(index++, objectClass.getPath());
		ps.setString(index++, objectClass.getTable());		
		
		if (superclassId != null) {	
			int baseclassId = getBaseclassId(objectClass);	
			ps.setInt(index++, superclassId);
			ps.setInt(index++, baseclassId);
		}	
		else {
			if (objectClass instanceof ObjectType) {
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
	
	private long getSequenceID(ADEMetadataSequence seqType) throws SQLException {
		StringBuilder query = new StringBuilder();
		DatabaseType dbType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();
						
		if (dbType == DatabaseType.ORACLE) {
			query.append("select ").append(seqType).append(".nextval from dual");
		}
		else if (dbType == DatabaseType.POSTGIS){
			query.append("select nextval('").append(seqType).append("')");
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

	private int getBaseclassId(AbstractObjectType<?> objectType) {
		int objectclassId = objectType.getObjectClassId();
		
		if (objectclassId <= 3)
			return objectclassId;
		else 
			return getBaseclassId((AbstractObjectType<?>)objectType.getExtension().getBase());
	}
	
	private String getSchemaMappingAsString(Path schemaMappingFile) {
		try {
			return new String(Files.readAllBytes(schemaMappingFile));
		} catch (IOException e) {
			return null;
		}	
	}
	
	private boolean validateObjectclassId (int objectclassId) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		boolean isValid = true;
	
		try {						
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select * from objectclass where id = " + objectclassId);		
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
			rs = stmt.executeQuery("select * from ade where db_prefix = '" + dbPrefix + "'");
			
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
			throw new SQLException("The DB_Prefix '" + dbPrefix + "'" + " is invalid, because it has already been reserved by other registered ADE");		
		
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
			throw new SQLException("Failed to create finerpint for the ADE schema-mapping file", e);
		} 	
	}

}
