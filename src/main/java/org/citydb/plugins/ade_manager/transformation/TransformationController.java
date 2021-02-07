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
package org.citydb.plugins.ade_manager.transformation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.ddlutils.model.Database;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.event.Event;
import org.citydb.event.EventHandler;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.database.DBScriptGenerator;
import org.citydb.plugins.ade_manager.transformation.graph.GraphTransformationManager;
import org.citydb.plugins.ade_manager.transformation.schemaMapping.SchemaMappingCreator;
import org.citygml4j.xml.schema.SchemaHandler;
import org.xml.sax.SAXException;

import com.sun.xml.xsom.util.DomAnnotationParserFactory;

import agg.xt_basis.GraGra;

public class TransformationController implements EventHandler {
	private final Logger LOG = Logger.getInstance();
	private ConfigImpl config;
	private GraGra adeGraph;
	private Database adeDatabaseSchema;
	private SchemaMapping adeSchemaMapping;
	private SchemaHandler schemaHandler;
	
	public TransformationController(ConfigImpl config) {		
		this.config = config;
    }
	
	public void doProcess(List<String> adeNamespaces) throws TransformationException { 	
		if (schemaHandler == null)
			throw new TransformationException("Schema handler has failed to initialize. ADE transformation cannot be started.");
				
		LOG.info("Transforming ADE XML schema to relational database schema...");
		GraphTransformationManager aggGraphTransformationManager = new GraphTransformationManager(schemaHandler, adeNamespaces, config);
		adeGraph = aggGraphTransformationManager.executeGraphTransformation();

    	LOG.info("Generating SQL-DDL for the database schema...");
		DBScriptGenerator databaseScriptCreator = new DBScriptGenerator(adeGraph, config);
		adeDatabaseSchema = databaseScriptCreator.createDatabaseScripts(); 
		
		LOG.info("Generating 3DCityDB schema mapping file...");
		SchemaMappingCreator schemaMappingCreator = new SchemaMappingCreator(adeGraph, config);
    	try {
    		adeSchemaMapping = schemaMappingCreator.createSchemaMapping();
		} catch (Exception e) {
			throw new TransformationException("An error occurred while creating the 3DCityDB schema mapping file.", e);
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
