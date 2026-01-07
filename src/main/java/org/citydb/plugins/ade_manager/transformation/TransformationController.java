/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2026
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
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

import agg.xt_basis.GraGra;
import com.sun.xml.xsom.util.DomAnnotationParserFactory;
import org.apache.ddlutils.model.Database;
import org.citydb.core.database.schema.mapping.SchemaMapping;
import org.citydb.plugins.ade_manager.ADEManagerPlugin;
import org.citydb.plugins.ade_manager.transformation.database.DBScriptGenerator;
import org.citydb.plugins.ade_manager.transformation.graph.GraphTransformationManager;
import org.citydb.plugins.ade_manager.transformation.schemaMapping.SchemaMappingCreator;
import org.citydb.util.event.Event;
import org.citydb.util.event.EventHandler;
import org.citydb.util.log.Logger;
import org.citygml4j.xml.schema.SchemaHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TransformationController implements EventHandler {
	private final Logger LOG = Logger.getInstance();
	private final ADEManagerPlugin plugin;
	private GraGra adeGraph;
	private Database adeDatabaseSchema;
	private SchemaMapping adeSchemaMapping;
	private SchemaHandler schemaHandler;

	public TransformationController(ADEManagerPlugin plugin) {
		this.plugin = plugin;
    }

	public void doProcess(List<String> adeNamespaces) throws TransformationException {
		if (schemaHandler == null)
			throw new TransformationException("Schema handler has failed to initialize. ADE transformation cannot be started.");

		LOG.info("Transforming ADE XML schema to relational database schema...");
		GraphTransformationManager aggGraphTransformationManager = new GraphTransformationManager(schemaHandler, adeNamespaces, plugin.getConfig());
		adeGraph = aggGraphTransformationManager.executeGraphTransformation();

    	LOG.info("Generating SQL-DDL for the database schema...");
		DBScriptGenerator databaseScriptCreator = new DBScriptGenerator(adeGraph, plugin.getConfig());
		adeDatabaseSchema = databaseScriptCreator.createDatabaseScripts();

		LOG.info("Generating 3DCityDB schema mapping file...");
		SchemaMappingCreator schemaMappingCreator = new SchemaMappingCreator(adeGraph, plugin.getConfig());
    	try {
    		adeSchemaMapping = schemaMappingCreator.createSchemaMapping();
		} catch (Exception e) {
			throw new TransformationException("An error occurred while creating the 3DCityDB schema mapping file.", e);
		} 
	}
	
	public List<String> getADENamespacesFromXMLSchema(String xmlSchemaPath) throws TransformationException {
		List<String> result = new ArrayList<>();
		try {
			schemaHandler = SchemaHandler.newInstance();	
			schemaHandler.reset();
			schemaHandler.setAnnotationParser(new DomAnnotationParserFactory());
			schemaHandler.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) {
					LOG.warn(format("Parser warning", exception));
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					LOG.error(format("Schema error", exception));
					throw new SAXException("The ADE XML schema is invalid.");
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					LOG.error(format("Schema error", exception));
					throw new SAXException("The ADE XML schema is invalid.");
				}

				private String format(String msg, SAXParseException exception) {
					return msg + " [" + exception.getLineNumber() + ", " + exception.getColumnNumber() + "]: " +
							exception.getMessage() + ".";
				}
			});

			schemaHandler.parseSchema(new File(xmlSchemaPath));
		} catch (SAXException e) {
			throw new TransformationException("Failed to parse ADE XML schema.", e);
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
