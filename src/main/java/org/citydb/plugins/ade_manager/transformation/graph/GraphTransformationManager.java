package org.citydb.plugins.ade_manager.transformation.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citygml4j.xml.schema.Schema;
import org.citygml4j.xml.schema.SchemaHandler;

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

public class GraphTransformationManager {
	private final Logger LOG = Logger.getInstance();
	
	private Schema schema;
	private SchemaHandler schemaHandler;	
	
	private EdGraGra edGraphGrammar; 	
	private ConfigImpl config;

	public GraphTransformationManager(SchemaHandler schemaHandler, Schema schema, ConfigImpl config) {		
		this.schema = schema;
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
    	marshallingEdGraphGrammer(config.getTmpGraphDirPath() + File.separator + "Output_Graph_Tmp.ggx");
    	
    	return aggGraphGrammar;
	}
	
	private void createGraphFromXMLSchema() {		
		// loaded predefined edGraph grammar
		XMLHelper xmlh = new XMLHelper();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream(config.getInputGraphPath())));

		File tmpFile = new File(config.getTmpGraphDirPath() + File.separator + "Input_Graph_Tmp.ggx");
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
		
		GraphCreator aggGraphCreator = new GraphCreator(schema, schemaHandler, edGraphGrammar.getBasisGraGra(), config);
		aggGraphCreator.createGraph();	
	}
	
	private void marshallingEdGraphGrammer(String exportPathString){		
		XMLHelper xmlh = new XMLHelper();
		xmlh.addTopObject(this.edGraphGrammar);
		
		if (xmlh.save_to_xml(exportPathString)) {			
			LOG.info("Graph has been created.");
		}
	}	
	
	private void convertDbObjectNameToLowercase() {
		Enumeration<Type> e = edGraphGrammar.getBasisGraGra().getTypes();		
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.DatabaseObject)) {
				List<Node> nodes = edGraphGrammar.getBasisGraGra().getGraph().getNodes(nodeType);
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
