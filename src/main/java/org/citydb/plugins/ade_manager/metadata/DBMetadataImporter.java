package org.citydb.plugins.ade_manager.metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.AbstractExtension;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.database.schema.mapping.AppSchema;
import org.citydb.database.schema.mapping.Namespace;
import org.citydb.database.schema.mapping.ObjectType;
import org.citydb.database.schema.mapping.SchemaMapping;

public class DBMetadataImporter {	
	private final DatabaseConnectionPool dbPool;
	
	private PreparedStatement psInsertADE;
	private PreparedStatement psInsertSchema;
	private PreparedStatement psInsertSchemaToObjectclass;
	private PreparedStatement psInsertSchemaReferencing;
	private PreparedStatement psInsertObjectclass;
	
	private MessageDigest md5;
	private SchemaMapping adeSchemaMapping;
	
	public DBMetadataImporter(DatabaseConnectionPool dbPool) throws SQLException {
		this.dbPool = dbPool;

		String insertADEQueryStr = "INSERT INTO ADE"
				+ "(ID, ADEID, NAME, DESCRIPTION, VERSION, DB_PREFIX, XML_SCHEMAMAPPING_FILE, DROP_DB_SCRIPT, CREATION_DATE, CREATION_PERSON) VALUES"
				+ "(?,?,?,?,?,?,?,?,?,?)";
		psInsertADE = dbPool.getConnection().prepareStatement(insertADEQueryStr);
		
		String insertSchemaQueryStr = "INSERT INTO SCHEMA"
				+ "(ID, IS_ADE_ROOT, CITYGML_VERSION, XML_NAMESPACE_URI, XML_NAMESPACE_PREFIX, XML_SCHEMA_LOCATION, XML_SCHEMAFILE, XML_SCHEMAFILE_TYPE, ADE_ID) VALUES"
				+ "(?,?,?,?,?,?,?,?,?)";
		psInsertSchema = dbPool.getConnection().prepareStatement(insertSchemaQueryStr);
		
		String insertSchemaReferencingQueryString = "INSERT INTO schema_referencing" + "(REFERENCED_ID, REFERENCING_ID) VALUES" + "(?,?)";
		psInsertSchemaReferencing = dbPool.getConnection().prepareStatement(insertSchemaReferencingQueryString);
				
		String insertObjectclassQueryString = "INSERT INTO OBJECTCLASS" + "(ID, IS_ADE_CLASS, CLASSNAME, TABLENAME, SUPERCLASS_ID, BASECLASS_ID, ADE_ID) VALUES" + "(?,?,?,?,?,?,?)";
		psInsertObjectclass = dbPool.getConnection().prepareStatement(insertObjectclassQueryString);
		
		String insertSchemaToObjectclassQueryString = "INSERT INTO schema_to_objectclass" + "(SCHEMA_ID, OBJECTCLASS_ID) VALUES" + "(?,?)";
		psInsertSchemaToObjectclass = dbPool.getConnection().prepareStatement(insertSchemaToObjectclassQueryString);
		
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		}
	}
	
	public void doImport(SchemaMapping adeSchemaMapping, Path schemaMappingFile) throws DBMetadataImportException {
		this.adeSchemaMapping = adeSchemaMapping;

		try {
			DBUtil.validateSchemaMapping(dbPool, adeSchemaMapping);
		} catch (SQLException e) {
			throw new DBMetadataImportException("Failed to read and process objectclass Ids, Aborting.", e);
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
			throw new DBMetadataImportException("Failed to import metadata, Cause: An ADE must have a root schema");

		long insertedADERowId;
		try {
			insertedADERowId = insertADE(schemaMappingFile);
			psInsertADE.close();
		} catch (SQLException e) {
			throw new DBMetadataImportException("Failed to import metadata into 'ADE' table.", e);
		}
		
		Map<String, List<Long>> insertedSchemas = null;
		try {
			insertedSchemas = insertSchemas(insertedADERowId, adeSchemaIds);
			psInsertSchema.close();
		} catch (SQLException e) {
			throw new DBMetadataImportException("Failed to import metadata into 'SCHEMA' table.", e);
		}
		
		try {
			insertSchemaReferencing(insertedSchemas, adeRootSchemaId);
			psInsertSchemaReferencing.close();
		} catch (SQLException e) {
			throw new DBMetadataImportException("Failed to import metadata into 'SCHEMA_REFERENCING' table.", e);
		}
		
		Map<Long, String> objectObjectclassIds;
		try {			
			objectObjectclassIds = insertObjectclasses(insertedADERowId);
			psInsertObjectclass.close();
		} catch (SQLException e) {
			throw new DBMetadataImportException("Failed to import metadata into 'OBJECTCLASS' table.", e);
		}
		
		try {
			insertSchemaToObjectclass(objectObjectclassIds, insertedSchemas);
			psInsertSchemaToObjectclass.close();
		} catch (SQLException e) {
			throw new DBMetadataImportException("Failed to import metadata into 'SCHEMA_TO_OBJECTCLASS' table.", e);
		}	
	}

	private long insertADE(Path schemaMappingFile) throws SQLException {				
		long seqId = DBUtil.getSequenceID(dbPool, DBSequenceType.ade_seq);
			
		int index = 1;
		psInsertADE.setLong(index++, seqId);
		psInsertADE.setString(index++, createMD5Fingerprint(schemaMappingFile));
		psInsertADE.setString(index++, adeSchemaMapping.getMetadata().getName());
		psInsertADE.setString(index++, adeSchemaMapping.getMetadata().getDescription());
		psInsertADE.setString(index++, adeSchemaMapping.getMetadata().getVersion());
		psInsertADE.setString(index++, adeSchemaMapping.getMetadata().getDBPrefix());
		
		String mappingText = getSchemaMappingAsString(schemaMappingFile);
		if (mappingText != null)
			psInsertADE.setString(index++, mappingText);
		else {
			// instead of inserting NULL, an exception should be raised
			// because xml_schemamapping_file should be non null
			psInsertADE.setNull(index++, Types.CLOB);
		}
		
		psInsertADE.setNull(index++, Types.CLOB);
		psInsertADE.setNull(index++, Types.DATE);
		psInsertADE.setNull(index++, Types.VARCHAR);
		
		psInsertADE.executeUpdate();
		return seqId;
	}
	
	private Map<String, List<Long>> insertSchemas(long adeRowId, List<String> adeSchemaIds) throws SQLException {				
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
	
				long seqId = DBUtil.getSequenceID(dbPool, DBSequenceType.schema_seq);
				insertedIds.add(seqId);

				int index = 1;
				psInsertSchema.setLong(index++, seqId);
				psInsertSchema.setInt(index++, adeSchema.isADERoot()?1:0);
				psInsertSchema.setString(index++, adeNamespace.getContext().name());
				psInsertSchema.setString(index++, adeNamespace.getURI());
				psInsertSchema.setString(index++, adeSchema.getId());
				psInsertSchema.setNull(index++, Types.VARCHAR);
				psInsertSchema.setObject(index++, null);
				psInsertSchema.setNull(index++, Types.VARCHAR);
				psInsertSchema.setLong(index++, adeRowId);
				
				psInsertSchema.executeUpdate();
			}					
			insertedSchemas.put(adeSchema.getId(), insertedIds);			
		}	
		
		return insertedSchemas;
	}
	
	private void insertSchemaToObjectclass(Map<Long, String> objectObjectclassIds, Map<String, List<Long>> insertedSchemas) throws SQLException {
		Iterator<Long> objectclassIter = objectObjectclassIds.keySet().iterator();
	    while (objectclassIter.hasNext()) {
	        long objectclassId = objectclassIter.next();
	        String schemaId = objectObjectclassIds.get(objectclassId);
	        
	        List<Long> insertedSchemaIds = insertedSchemas.get(schemaId);
	        Iterator<Long> schemaIter = insertedSchemaIds.iterator();
	        while (schemaIter.hasNext()) {
	        	long insertedSchemaId = schemaIter.next();
		
	        	psInsertSchemaToObjectclass.setLong(1, insertedSchemaId);
	        	psInsertSchemaToObjectclass.setLong(2, objectclassId);	
	        	
	        	psInsertSchemaToObjectclass.executeUpdate();
	        }
	    }
	}
	
	private void insertSchemaReferencing(Map<String, List<Long>> insertedSchemas, String adeRootSchemaId) throws SQLException {	
		int numberOfCityGMLversions = adeSchemaMapping.getSchemas().get(0).getNamespaces().size();
		for (int i = 0; i < numberOfCityGMLversions; i++) {
			long referencingId = insertedSchemas.get(adeRootSchemaId).get(i);
			
			Iterator<String> iter = insertedSchemas.keySet().iterator();
		    while (iter.hasNext()) {
		        String schemaId = iter.next();
		        if (!schemaId.equalsIgnoreCase(adeRootSchemaId)) {
		        	long referencedId = insertedSchemas.get(schemaId).get(i);
			        	
		        	psInsertSchemaReferencing.setLong(1, referencedId);	
		        	psInsertSchemaReferencing.setLong(2, referencingId);
		        	
		        	psInsertSchemaReferencing.executeUpdate();
		        }		        
		    }
		}
	}
	
	private Map<Long, String> insertObjectclasses(long insertedADERowId) throws SQLException {
		Map<Long, String> insertedObjectclasses = new HashMap<Long, String>();

		// iterate through object and feature types
		Iterator<AbstractObjectType<?>> objectIter = adeSchemaMapping.getAbstractObjectTypes().iterator();			
		while (objectIter.hasNext()) {
			AbstractObjectType<?> objectClass = objectIter.next();	

			long objectclassId = objectClass.getObjectClassId();
			insertedObjectclasses.put(objectclassId, objectClass.getSchema().getId());
			
			int index = 1;
			psInsertObjectclass.setLong(index++, objectclassId);
			psInsertObjectclass.setInt(index++, 1);
			psInsertObjectclass.setString(index++, objectClass.getPath());
			psInsertObjectclass.setString(index++, objectClass.getTable());
			
			AbstractExtension<?> objectExtension = objectClass.getExtension();
			if (objectExtension != null) {
				AbstractObjectType<?> superType = (AbstractObjectType<?>) objectExtension.getBase();
				int superclassId = superType.getObjectClassId();	
				int baseclassId = getBaseclassId(objectClass);				
				psInsertObjectclass.setInt(index++, superclassId);
				psInsertObjectclass.setInt(index++, baseclassId);
			}	
			else {
				if (objectClass instanceof ObjectType) {
					psInsertObjectclass.setInt(index++, 1);
					psInsertObjectclass.setInt(index++, 1);
				}
				else {
					psInsertObjectclass.setInt(index++, 2);
					psInsertObjectclass.setInt(index++, 2);
				}
				
			}
			
			psInsertObjectclass.setLong(index++, insertedADERowId);

			psInsertObjectclass.executeUpdate();				
		}
		
		return insertedObjectclasses;
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
	
	private String createMD5Fingerprint(Path schemaMappingFile) {
		try {
			byte[] hash = md5.digest(Files.readAllBytes(schemaMappingFile));
			StringBuffer hex = new StringBuffer();
			for (int i = 0; i < hash.length; i++)
				hex.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
			
			return hex.toString();
		} catch (IOException e) {
			return null;
		}		
	}
	
}
