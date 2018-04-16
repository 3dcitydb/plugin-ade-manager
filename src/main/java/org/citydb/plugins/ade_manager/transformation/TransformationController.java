package org.citydb.plugins.ade_manager.transformation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.ddlutils.model.Database;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.database.delete.DeleteScriptGeneratorFactory;
import org.citydb.plugins.ade_manager.transformation.database.delete.DsgException;
import org.citydb.plugins.ade_manager.transformation.database.delete.IDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.transformation.database.schema.DBScriptGenerator;
import org.citydb.plugins.ade_manager.transformation.graph.GraphTransformationManager;
import org.citydb.plugins.ade_manager.transformation.schemaMapping.SchemaMappingCreator;
import org.citygml4j.xml.schema.Schema;
import org.citygml4j.xml.schema.SchemaHandler;
import org.xml.sax.SAXException;

import com.sun.xml.xsom.util.DomAnnotationParserFactory;

import agg.xt_basis.GraGra;

public class TransformationController implements EventHandler {
	private final Logger LOG = Logger.getInstance();
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
	
	private ConfigImpl config;
	private GraGra adeGraph;
	private Database adeDatabaseSchema;
	private SchemaMapping adeSchemaMapping;
	private SchemaHandler schemaHandler;
	
	public TransformationController(ConfigImpl config) {		
		this.config = config;
    }
	
	public void doProcess(String adeNamespace) throws TransformationException { 	
		if (schemaHandler == null)
			throw new TransformationException("SchemaHnadler has failed to initialize. ADE transformation cannot be started");
				
		LOG.info("Mapping XML schema elements to a graph...");
		Schema adeXmlSchema = schemaHandler.getSchema(adeNamespace);
		GraphTransformationManager aggGraphTransformationManager = new GraphTransformationManager(schemaHandler, adeXmlSchema, config);
		adeGraph = aggGraphTransformationManager.executeGraphTransformation();

    	LOG.info("Generating 3DCityDB ADE database schema...");
		DBScriptGenerator databaseScriptCreator = new DBScriptGenerator(adeGraph, config);
		adeDatabaseSchema = databaseScriptCreator.createDatabaseScripts(); 
		
		LOG.info("Generating 3DCityDB database delete-script...");
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		DeleteScriptGeneratorFactory factory = new DeleteScriptGeneratorFactory();
		IDeleteScriptGenerator cleanupScriptGenerator = factory.createDatabaseAdapter(databaseType);
		try {
			cleanupScriptGenerator.doProcess(this, dbPool, config);
		} catch (DsgException e) {
			throw new TransformationException("Failed to generate delect-scripts for the connected 3DCityDB instance", e);
		}
		
		LOG.info("Generating 3dcitydb XML SchemaMapping file...");
		SchemaMappingCreator schemaMappingCreator = new SchemaMappingCreator(adeGraph, config);
    	try {
    		adeSchemaMapping = schemaMappingCreator.createSchemaMapping();
		} catch (Exception e) {
			throw new TransformationException("Error occurred while creating the XML schema Mapping file.", e);
		} 
	}
	
	public List<String> getADENamespacesFromXMLSchema(String xmlSchemaPath) throws TransformationException {
		List<String> result = new ArrayList<String>();		
		try {
			schemaHandler = SchemaHandler.newInstance();	
			schemaHandler.reset();
			schemaHandler.setAnnotationParser(new DomAnnotationParserFactory());
			schemaHandler.parseSchema(new File(xmlSchemaPath));
		} catch (SAXException e) {
			throw new TransformationException("Failed to parse ADE XML schema", e);
		}
		
		for (String schemaNamespace : schemaHandler.getTargetNamespaces()) {
			if (!schemaNamespace.startsWith("http://www.w3.org") && 
					!schemaNamespace.startsWith("http://www.citygml.org/citygml4j") && 
					!schemaNamespace.startsWith("http://www.opengis.net") && 
					!schemaNamespace.startsWith("urn:oasis:names:tc:ciq:xsdschema:xAL:2.0"))
				result.add(schemaNamespace);
		}
		
		return result;
	}
	
	public GraGra getAdeGraph() {
		return adeGraph;
	}

	public Database getAdeDatabaseSchema() {
		return adeDatabaseSchema;
	}

	public SchemaMapping getAdeSchemaMapping() {
		return adeSchemaMapping;
	}
	
	@Override
	public void handleEvent(Event event) throws Exception {

	}

}
