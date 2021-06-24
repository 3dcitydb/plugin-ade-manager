/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
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
package org.citydb.plugins.ade_manager.transformation.graph;

import agg.attribute.AttrInstance;
import agg.attribute.impl.ValueMember;
import agg.attribute.impl.ValueTuple;
import agg.editor.impl.EdGraGra;
import agg.util.XMLHelper;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.GraGra;
import agg.xt_basis.GraTra;
import agg.xt_basis.LayeredGraTraImpl;
import agg.xt_basis.Node;
import agg.xt_basis.Type;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.util.GlobalConstants;
import org.citygml4j.xml.schema.SchemaHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

public class GraphTransformationManager {
	private List<String> namespaces;
	private SchemaHandler schemaHandler;	
	
	private EdGraGra edGraphGrammar; 	
	private ConfigImpl config;

	public GraphTransformationManager(SchemaHandler schemaHandler, List<String> namespaces, ConfigImpl config) {		
		this.namespaces = namespaces;
		this.schemaHandler = schemaHandler;	
		this.config = config;
	}

	public GraGra executeGraphTransformation() {
		// map XML Schema to Graph structure
		createGraphFromXMLSchema();
		
    	// prepare for transformation
		GraTra graTra = new LayeredGraTraImpl();
    	GraGra aggGraphGrammar = edGraphGrammar.getBasisGraGra();
    	graTra.setGraGra(aggGraphGrammar);
    	graTra.setHostGraph(aggGraphGrammar.getGraph());  
    	
    	// add listeners
		GraphTransformationEventListener eventListener = new GraphTransformationEventListener(graTra);   	
    	graTra.addGraTraListener(eventListener);	

    	// execute graph transformation
    	graTra.transform();
    	
    	// post process for converting every database object's name to lower case
    	convertDbObjectNameToLowercase();
    	
    	// write to file
    	marshallingEdGraphGrammer(config.getTmpGraphDirPath() + File.separator + GlobalConstants.TMP_OUTPUT_GRAPH_FILE_NAME);
    	
    	return aggGraphGrammar;
	}
	
	private void createGraphFromXMLSchema() {		
		// loaded predefined edGraph grammar
		XMLHelper xmlh = new XMLHelper();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream(GlobalConstants.INPUT_GRAPH_PATH)));

		File tmpFile = new File(config.getTmpGraphDirPath() + File.separator + GlobalConstants.TMP_INPUT_GRAPH_FILE_NAME);
	    OutputStream outStream;
		try {
			outStream = new FileOutputStream(tmpFile);
			 String line = null;	
				while ((line = in.readLine()) != null) {
					outStream.write(line.getBytes());
				}
		} catch (IOException e) {
			//
		}
	
		if (xmlh.read_from_xml(tmpFile.getAbsolutePath())) {
			GraGra graphGrammar = BaseFactory.theFactory().createGraGra(false);				
			xmlh.getTopObject(graphGrammar);			
			this.edGraphGrammar = new EdGraGra(graphGrammar);
			xmlh.enrichObject(this.edGraphGrammar);			
		}
		
		GraphCreator aggGraphCreator = new GraphCreator(namespaces, schemaHandler, edGraphGrammar.getBasisGraGra());
		aggGraphCreator.createGraph();	
	}
	
	private void marshallingEdGraphGrammer(String exportPathString){		
		XMLHelper xmlh = new XMLHelper();
		xmlh.addTopObject(this.edGraphGrammar);
		xmlh.save_to_xml(exportPathString);
	}	
	
	private void convertDbObjectNameToLowercase() {
		Enumeration<Type> e = edGraphGrammar.getBasisGraGra().getTypes();		
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.DatabaseObject)) {
				List<Node> nodes = edGraphGrammar.getBasisGraGra().getGraph().getNodes(nodeType);
				if (nodes == null)
					continue;
				
				for (Node node: nodes) {					
					AttrInstance attrInstance = node.getAttribute();
					String name = (String)attrInstance.getValueAt("name");
					ValueTuple valueTuple = (ValueTuple) attrInstance;
					ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("name");
					attr.setExprAsObject(name.toLowerCase());
				}				
			};
		}
	}
		
}
