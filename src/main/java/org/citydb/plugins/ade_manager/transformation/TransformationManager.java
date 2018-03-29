package org.citydb.plugins.ade_manager.transformation;

import org.apache.ddlutils.model.Database;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.database.schema.DBScriptGenerator;
import org.citydb.plugins.ade_manager.transformation.graph.GraphTransformationManager;
import org.citydb.plugins.ade_manager.transformation.schemaMapping.SchemaMappingCreator;
import org.citygml4j.xml.schema.Schema;
import org.citygml4j.xml.schema.SchemaHandler;
import agg.xt_basis.GraGra;

public class TransformationManager implements EventHandler {
	private final Logger LOG = Logger.getInstance();
	private ConfigImpl config;
	
	private SchemaHandler schemaHandler;
	private Schema adeXmlSchema;		
	private GraGra adeGraph;
	private Database adeDatabaseSchema;
	private SchemaMapping adeSchemaMapping;
	
	public TransformationManager(SchemaHandler schemaHandler, Schema schema, ConfigImpl config) {
		this.schemaHandler = schemaHandler;
		this.adeXmlSchema = schema;	
		this.config = config;
    }
	
	public void doProcess() throws TransformationException { 		
		LOG.info("Mapping XML schema elements to a graph...");
		GraphTransformationManager aggGraphTransformationManager = new GraphTransformationManager(schemaHandler, adeXmlSchema, config);
		adeGraph = aggGraphTransformationManager.executeGraphTransformation();

    	LOG.info("Generating Oracle and PostGIS database schema in SQL scripts...");
		DBScriptGenerator databaseScriptCreator = new DBScriptGenerator(adeGraph, config);
		adeDatabaseSchema = databaseScriptCreator.createDatabaseScripts(); 
		
		LOG.info("Creating 3dcitydb XML SchemaMapping file...");
		SchemaMappingCreator schemaMappingCreator = new SchemaMappingCreator(adeGraph, config);
    	try {
    		adeSchemaMapping = schemaMappingCreator.createSchemaMapping();
		} catch (Exception e) {
			throw new TransformationException("Error occurred while creating the XML schema Mapping file.", e);
		} 
	}
	
	public Schema getAdeXmlSchema() {
		return adeXmlSchema;
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
