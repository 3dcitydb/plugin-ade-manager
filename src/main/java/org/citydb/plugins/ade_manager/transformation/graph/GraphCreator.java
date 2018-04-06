package org.citydb.plugins.ade_manager.transformation.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import org.citydb.database.schema.mapping.GeometryType;
import org.citydb.database.schema.mapping.SimpleType;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.graph.ADEschemaHelper.ComplexAttributeType;
import org.citydb.plugins.ade_manager.transformation.graph.ADEschemaHelper.SimpleAttribute;
import org.citygml4j.xml.schema.Schema;
import org.citygml4j.xml.schema.SchemaHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;

import agg.attribute.AttrInstance;
import agg.attribute.impl.ValueMember;
import agg.attribute.impl.ValueTuple;
import agg.xt_basis.Arc;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.Node;
import agg.xt_basis.Type;
import agg.xt_basis.TypeException;

public class GraphCreator {
	private GraGra graphGrammar;
	private SchemaHandler schemaHandler;
	private Schema schema;
	private Map<String, Node> globalClassNodes;
	private Node hostSchemaNode;
	private ConfigImpl config;
	private final Logger LOG = Logger.getInstance();

	public GraphCreator(Schema schema, SchemaHandler schemaHandler, GraGra graphGrammar, ConfigImpl config){
		this.schemaHandler = schemaHandler;			
		this.schema = schema;
		this.graphGrammar = graphGrammar;	
		this.config = config;
	}

	public void createGraph() {
		// Create HostSchema Node		
		String namespaceUri = this.schema.getNamespaceURI();
		this.hostSchemaNode = this.createNode(GraphNodeArcType.Schema);
		AttrInstance attrInstance = hostSchemaNode.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;		
		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("name");
		attr.setExprAsObject(config.getAdeName());
		attr = (ValueMember) valueTuple.getValueMemberAt("namespaceUri");		
		attr.setExprAsObject(namespaceUri);

		// Create class Nodes and put them into a internal map
		globalClassNodes = new HashMap<String, Node>();
		Map<String, XSElementDecl> elementDecls = schema.getXSSchema().getElementDecls();

		Iterator<Entry<String, XSElementDecl>> iter = elementDecls.entrySet().iterator();		
		while (iter.hasNext()) {
			Entry<String, XSElementDecl> elementDeclEntry = (Entry<String, XSElementDecl>) iter.next();
			XSElementDecl xsElementDecl = elementDeclEntry.getValue();
			this.parseGlobalClassElement(xsElementDecl);
		} 
	}

	private void parseGlobalClassElement(XSElementDecl xsElementDecl) {								
		ADEschemaElement decl = new ADEschemaElement(xsElementDecl, schema);	

		if (decl.isADEHookElement()) {
			this.parseADEHookElement(xsElementDecl);
		}
		else {
			// get or create graph node for the global class					
			Node classNode = this.getOrCreateElementTypeNode(decl);	

			// process extension
			if (!decl.isDerivedFromOtherDomains()) {				
				XSElementDecl parentXsElementDecl = xsElementDecl.getSubstAffiliation();
				if (parentXsElementDecl != null) {					
					ADEschemaElement parentDecl = new ADEschemaElement(parentXsElementDecl, schemaHandler.getSchema(parentXsElementDecl.getTargetNamespace()));				
					Node parentNode = this.getOrCreateElementTypeNode(parentDecl);					
					Node extensionNode = this.createNode(GraphNodeArcType.Extension);
					this.createArc(GraphNodeArcType.Contains, classNode, extensionNode);
					this.createArc(GraphNodeArcType.BaseType, extensionNode, parentNode);
				}		
			}
			else {
				if (decl.isAbstractGML() || decl.isFeature()) {
					XSElementDecl parentXsElementDecl = xsElementDecl.getSubstAffiliation();
					ADEschemaElement parentDecl = new ADEschemaElement(parentXsElementDecl, schemaHandler.getSchema(parentXsElementDecl.getTargetNamespace()));				
					Node parentNode = this.getOrCreateElementTypeNode(parentDecl);					
					Node extensionNode = this.createNode(GraphNodeArcType.Extension);
					this.createArc(GraphNodeArcType.Contains, classNode, extensionNode);
					this.createArc(GraphNodeArcType.BaseType, extensionNode, parentNode);
				}				
			}

			// process properties 
			if (xsElementDecl.getTargetNamespace().equalsIgnoreCase(schema.getNamespaceURI())) {
				xsElementDecl.visit(new SchemaVisitor() {
					@Override
					public void modelGroup(XSModelGroup modelGroup) {
						for (XSParticle p : modelGroup.getChildren()) {
							XSTerm pterm = p.getTerm();
							if (pterm.isElementDecl()) {
								int minOccurs = p.getMinOccurs().intValue();
								int maxOccurs = p.getMaxOccurs().intValue();
								XSElementDecl propertyDecl = pterm.asElementDecl();
								parseLocalPropertyElement(propertyDecl, classNode, minOccurs, maxOccurs);
							}
						}						
					}				
				});
			}	
		}
	}

	private void parseADEHookElement(XSElementDecl adeHookXsElementDecl) {
		XSElementDecl citygmlHookXSElementDecl = adeHookXsElementDecl.getSubstAffiliation();

		if (citygmlHookXSElementDecl != null) {
			String citygmlHookName = citygmlHookXSElementDecl.getName();
			String citygmlHookNamespace = citygmlHookXSElementDecl.getTargetNamespace();
			Schema citygmlModuleSchema = schemaHandler.getSchema(citygmlHookNamespace);
			String superCityGMLClassName = ADEschemaHelper.CityGML_Hooks.get(citygmlHookName);	

			if (superCityGMLClassName != null) {
				XSElementDecl superCityGMLClassXSElementDecl = citygmlModuleSchema.getXSSchema().getElementDecl(superCityGMLClassName);
				ADEschemaElement parentDecl = new ADEschemaElement(superCityGMLClassXSElementDecl, schemaHandler.getSchema(superCityGMLClassXSElementDecl.getTargetNamespace()));					
				Node superCityGMLClassNode = this.getOrCreateElementTypeNode(parentDecl);	

				String subClassName = "_" + ADEschemaHelper.CityDB_Tables.get(new QName(citygmlHookNamespace, superCityGMLClassXSElementDecl.getType().getName()));

				Node subCityGMLADEClassNode = this.getOrCreateADEHookClass(subClassName, parentDecl);

				// create inheritance nodes and arcs
				boolean createExtensionNode = true;
				Iterator<Arc> arcIter = subCityGMLADEClassNode.getOutgoingArcs();		
				while(arcIter.hasNext()) {
					Arc arc = arcIter.next();
					Node targetNode = (Node) arc.getTarget();							
					if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.Extension)) {
						createExtensionNode = false;
						break;
					}		
				}

				if (createExtensionNode) {
					Node extensionNode = this.createNode(GraphNodeArcType.Extension);
					this.createArc(GraphNodeArcType.Contains, subCityGMLADEClassNode, extensionNode);
					this.createArc(GraphNodeArcType.BaseType, extensionNode, superCityGMLClassNode);
				}				

				// create property node
				int minOccurs = 0;
				int maxOccurs = -1;
				
				String ADEHookMaxOccurs = getTaggedValueFromXMLAnnotation(adeHookXsElementDecl, "maxOccurs");
				if (ADEHookMaxOccurs != null) {					
					try {
						maxOccurs = Integer.parseInt(ADEHookMaxOccurs);
					} catch (NumberFormatException nfe) {
						LOG.warn("The ADE hook property '" + adeHookXsElementDecl.getName()
								+ "' has an invalid tagged value for its maxOccurs: '" + ADEHookMaxOccurs
								+ "', which will be internally set to 'unbounded'");
					}
				}

				this.parseLocalPropertyElement(adeHookXsElementDecl, subCityGMLADEClassNode, minOccurs, maxOccurs);
			}
		}
	}

	private void parseLocalPropertyElement(XSElementDecl propertyXSElementDecl, Node parentNode, int minOccurs, int maxOccurs) {
		ADEschemaElement propertyDecl = new ADEschemaElement(propertyXSElementDecl, schema);

		String nameAndPath = propertyDecl.getLocalName();
		String namespace = propertyDecl.getNamespaceURI();		
		boolean isForeign = !schema.getNamespaceURI().equalsIgnoreCase(namespace);		

		String propertyNodeType = null;
		Node propertyNode = null;
		
		// read the tagged value of "relationType" from XML annotation		
		String relationType = getTaggedValueFromXMLAnnotation(propertyXSElementDecl, "relationType");
		
		if (propertyDecl.isEnumerationProperty()) {
			propertyNodeType = GraphNodeArcType.EnumerationProperty;
			String primitiveDataType = SimpleType.STRING.value();
			propertyNode = this.createSimpleAttributeNode(propertyNodeType, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, primitiveDataType);
		} 
		else if (propertyDecl.isSimpleBasicProperty()) {
			propertyNodeType = GraphNodeArcType.SimpleAttribute;
			String primitiveDataType = ADEschemaHelper.SimpleAttributeTypes.get(propertyDecl.getXSElementDecl().getType().getName()).value();
			propertyNode = this.createSimpleAttributeNode(propertyNodeType, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, primitiveDataType);
		} 		
		else if (propertyDecl.isImplicitGeometryProperty()) {
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.ImplicitGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, GeometryType.ABSTRACT_GEOMETRY.value());
		}
		else if (propertyDecl.isBrepGeometryProperty()) {    
			String geometryType = ADEschemaHelper.BrepGeometryPropertyTypes.get(propertyDecl.getXSElementDecl().getType().getName()).value();
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.BrepGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, geometryType);
		}
		else if (propertyDecl.isPointOrLineGeometryProperty()) {    	                		
			String geometryType = ADEschemaHelper.PointOrLineGeometryPropertyTypes.get(propertyDecl.getXSElementDecl().getType().getName()).value();
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.PointOrLineGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, geometryType);
		}
		else if (propertyDecl.isHybridGeometryProperty()) {    	                			 
			String geometryType = ADEschemaHelper.HybridGeometryPropertyTypes.get(propertyDecl.getXSElementDecl().getType().getName()).value();
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.HybridGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, geometryType);
		}
		else if (propertyDecl.isCityGMLnonPorperty()) {
			propertyNode = this.createPropertyNode(GraphNodeArcType.ComplexTypeProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace);
			this.setNodeAttributeValue(propertyNode, "relationType", relationType);
			this.processCityGMLnonPropertyNode(propertyNode, propertyDecl);
		}
		else if (propertyDecl.isGMLreferenceProperty()) {
			propertyNode = this.createPropertyNode(GraphNodeArcType.ComplexTypeProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace);
			this.setNodeAttributeValue(propertyNode, "relationType", relationType);
			this.processGMLreferencePropertyNode(propertyNode, propertyDecl);
		}
		else if (propertyDecl.isFeatureOrObjectProperty() || propertyDecl.isUnionProperty() || propertyDecl.isComplexDataProperty()) {
			propertyNode = this.createPropertyNode(GraphNodeArcType.ComplexTypeProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace);
			this.setNodeAttributeValue(propertyNode, "relationType", relationType);
			this.processComplexTypePropertyNode(propertyNode, propertyDecl);                 		
		} 
		else if (propertyDecl.isComplexAttribute()) {
			propertyNode = this.createPropertyNode(GraphNodeArcType.ComplexAttribute, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace);
			this.processComplexAttributeNode(propertyNode, propertyDecl);
		}
		else {
			String propertyTypeName = propertyDecl.getXSElementDecl().getType().getName();
			propertyNode = this.createPropertyNode(GraphNodeArcType.GenericAttribute, nameAndPath, isForeign, propertyTypeName, minOccurs, maxOccurs, namespace);
			LOG.warn("Map Porperty element '" + propertyDecl.getLocalName() + "' onto CLOB column; Type name: \"" + propertyTypeName + "\"");
		}
		
		if (propertyNode != null)
			this.createArc(GraphNodeArcType.Contains, parentNode, propertyNode);   		     	      
	}

	private Node getOrCreateADEHookClass (String className, ADEschemaElement parentDecl) {	
		if (globalClassNodes.containsKey(className))
			return globalClassNodes.get(className);	

		String namespaceUri = schema.getNamespaceURI();
		boolean isAbstract = parentDecl.isAbstract();	
		boolean isForeign = false;
		String derivedFrom = "HookClass";

		Node classNode = this.createComplexTypeNode(className, isForeign, className, namespaceUri, isAbstract, derivedFrom, false);	

		globalClassNodes.put(className, classNode);

		return classNode;
	}

	private Node getOrCreateElementTypeNode (ADEschemaElement decl) {
		String className = decl.getXSElementDecl().getType().getName();
		String path = decl.getLocalName();
		if (globalClassNodes.containsKey(className))
			return globalClassNodes.get(className);	

		String namespaceUri = decl.getNamespaceURI();
		boolean isAbstract = decl.isAbstract();	
		boolean isForeign = !schema.getNamespaceURI().equalsIgnoreCase(namespaceUri);

		// read the tagged value of "topLevel" from XML annotation
		boolean topLevel = false;
		String topLevelStr = getTaggedValueFromXMLAnnotation(decl.getXSElementDecl(), "topLevel");
		if (topLevelStr != null) {
			if (topLevelStr.equalsIgnoreCase("true"))
				topLevel = true;
		}

		Node classNode = null;

		if (decl.isComplexDataType() || decl.isCityObject() || decl.isFeature() || decl.isAbstractGML() || decl.isUnion()) {	
			String derivedFrom = this.getDerivedFromClassName(decl);		
			classNode = this.createComplexTypeNode(path, isForeign, className, namespaceUri, isAbstract, derivedFrom, topLevel);							
		}
		else {
			LOG.warn("Unsupported XML/GML Type '" 
					+ decl.getXSElementDecl().getType().getName()
					+ "' for the ADE class '" + decl.getXSElementDecl().getName() + "'. (skipped).");
		}

		if (ADEschemaHelper.CityGML_Namespaces.contains(namespaceUri) || namespaceUri.equalsIgnoreCase("http://www.opengis.net/gml")) 
			createTableForCityGMLClass(namespaceUri, decl.getXSElementDecl().getType().getName(), classNode);

		globalClassNodes.put(className, classNode);

		return classNode;
	}

	private String getDerivedFromClassName(ADEschemaElement decl) {
		String derivedFrom = null;

		if (decl.isFeature() || (decl.isCityGMLClass())) {
			derivedFrom = "_Feature";
		}
		else if (decl.isAbstractGML()) {
			derivedFrom = "_GML";
		}
		else {
			derivedFrom = "_Object";
		}

		return derivedFrom;
	}

	private Node getOrCreateComplexTypeNode (String path, boolean isForeign, String className, String namespaceUri, boolean isAbstract, String derivedFrom) {	
		if (globalClassNodes.containsKey(className))
			return globalClassNodes.get(className);	

		Node classNode = this.createComplexTypeNode(path, isForeign, className, namespaceUri, isAbstract, derivedFrom, false);		
		globalClassNodes.put(className, classNode);

		return classNode;
	}

	private Node createNamedElementNode (String nodeType, String path, boolean isForeign, String name, String namespace) {
		Node elementNode = this.createNode(nodeType);
		AttrInstance attrInstance = elementNode.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;

		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("path");
		attr.setExprAsObject(path);
		attr = (ValueMember) valueTuple.getValueMemberAt("isForeign");		
		attr.setExprAsObject(isForeign);
		attr = (ValueMember) valueTuple.getValueMemberAt("name");
		attr.setExprAsObject(name);
		attr = (ValueMember) valueTuple.getValueMemberAt("namespaceUri");
		attr.setExprAsObject(namespace);

		return elementNode;
	}

	private Node createElementTypeNode (String nodeType, String path, boolean isForeign, String name, String namespace) {
		Node classNode = this.createNamedElementNode(nodeType, path, isForeign, name, namespace);

		if (!isForeign)
			this.createArc(GraphNodeArcType.Contains, hostSchemaNode, classNode);

		return classNode;
	}

	private Node createComplexTypeNode (String path, boolean isForeign, String name, String namespace, boolean isAbstract, String derivedFrom, boolean topLevel) {
		Node node = this.createElementTypeNode(GraphNodeArcType.ComplexType, path, isForeign, name, namespace);
		AttrInstance attrInstance = node.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;
		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("derivedFrom");
		attr.setExprAsObject(derivedFrom);
		attr = (ValueMember) valueTuple.getValueMemberAt("isAbstract");
		attr.setExprAsObject(isAbstract);
		attr = (ValueMember) valueTuple.getValueMemberAt("topLevel");
		attr.setExprAsObject(topLevel);
		return node;
	}

	private Node createPropertyNode (String nodeType, String path, boolean isForeign, String name, int minOccurs, int maxOccurs, String namespace) {
		Node propertyNode = this.createNamedElementNode(nodeType, path, isForeign, name, namespace);
		AttrInstance attrInstance = propertyNode.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;		
		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("minOccurs");
		attr.setExprAsObject(minOccurs);
		attr = (ValueMember) valueTuple.getValueMemberAt("maxOccurs");
		attr.setExprAsObject(maxOccurs);		
		return propertyNode;
	}

	private Node createGeometryPropertyNode (String nodeType, String path, boolean isForeign, String name, int minOccurs, int maxOccurs, String namespace, String geometryType) {
		Node propertyNode = this.createPropertyNode(nodeType, path, isForeign, name, minOccurs, maxOccurs, namespace);
		AttrInstance attrInstance = propertyNode.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;
		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("geometryType");
		attr.setExprAsObject(geometryType);
		return propertyNode;
	}

	private Node createSimpleAttributeNode (String nodeType, String path, boolean isForeign, String name, int minOccurs, int maxOccurs, String namespace, String type) {
		Node propertyNode = this.createPropertyNode(nodeType, path, isForeign, name, minOccurs, maxOccurs, namespace);
		AttrInstance attrInstance = propertyNode.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;
		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("primitiveDataType");
		attr.setExprAsObject(type);
		return propertyNode;
	}
	
	private void setNodeAttributeValue(Node node, String attrName, Object value) {
		AttrInstance attrInstance = node.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;
		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt(attrName);
		attr.setExprAsObject(value);
	}

	// CityGML _CityObject and ExternalReference which do not have globally defined property. 
	private void processCityGMLnonPropertyNode (Node propertyNode, ADEschemaElement propertyDecl) {
		XSType propertyType = propertyDecl.getXSElementDecl().getType();
		String propertyTypeNamespaceUri = propertyType.getTargetNamespace();	
		String className = propertyType.getName();

		Schema targetSchema = schemaHandler.getSchema(propertyTypeNamespaceUri);
		Iterator<Entry<String, XSElementDecl>> iter = targetSchema.getXSSchema().getElementDecls().entrySet().iterator();		
		while (iter.hasNext()) {
			Entry<String, XSElementDecl> elementDeclEntry = (Entry<String, XSElementDecl>) iter.next();
			XSElementDecl xsElementDecl = elementDeclEntry.getValue();
			if (xsElementDecl.getType().getName().equalsIgnoreCase(className)) {
				ADEschemaElement decl = new ADEschemaElement(xsElementDecl, targetSchema);	
				Node childNode = this.getOrCreateElementTypeNode(decl); 		
				this.createArc(GraphNodeArcType.TargetType, propertyNode, childNode);
				return;
			}			
		} 

		boolean isAbstract = propertyType.asComplexType().isAbstract();
		String derivedFrom = "_Object";				
		Node childNode = this.getOrCreateComplexTypeNode (className, true, className, propertyTypeNamespaceUri, isAbstract, derivedFrom);    		
		this.createArc(GraphNodeArcType.TargetType, propertyNode, childNode);
		this.createTableForCityGMLClass(propertyTypeNamespaceUri, className, childNode);
	}

	/** Example illustrating the XML annotation structure 
	    <annotation>
	      <appinfo>
	        <gml:targetElement>bldg:_AbstractBuilding</gml:targetElement>
	      </appinfo>
	    </annotation>
	 **/
	private void processGMLreferencePropertyNode(Node propertyNode, ADEschemaElement decl) {
		Element annotationElement = (Element) decl.getXSElementDecl().getAnnotation().getAnnotation();       	
		if (annotationElement != null) {   		
			org.w3c.dom.Node targetElementDOMNode = (org.w3c.dom.Node) annotationElement.getElementsByTagName("appinfo").item(0).getFirstChild().getNextSibling().getFirstChild(); 
			String[] strArray = targetElementDOMNode.getNodeValue().split(":");
			String classPrefix = strArray[0];
			String childClassName = strArray[1];
			String classNameSpace = annotationElement.lookupNamespaceURI(classPrefix);
			XSElementDecl xsChildElement = schemaHandler.getSchema(classNameSpace).getXSSchema().getElementDecl(childClassName);     		
			ADEschemaElement childDecl = new ADEschemaElement(xsChildElement, schemaHandler.getSchema(xsChildElement.getTargetNamespace()));	
			Node childNode = this.getOrCreateElementTypeNode(childDecl);    		
			this.createArc(GraphNodeArcType.TargetType, propertyNode, childNode);
		}
	}

	private void processComplexTypePropertyNode (Node propertyNode, ADEschemaElement decl) {
		XSAnnotation xsAnnotation = decl.getXSElementDecl().getAnnotation();
		String reversePropertyName = null;
		if (xsAnnotation != null) {
			Element annotationElement = (Element) xsAnnotation.getAnnotation();       	
			if (annotationElement != null) {   
				org.w3c.dom.Node appinfoItem = (org.w3c.dom.Node) annotationElement.getElementsByTagName("appinfo").item(0);
				if (appinfoItem != null) {
					NodeList nodeList = appinfoItem.getChildNodes();
					for (int i = 0; i < nodeList.getLength(); i++) {
						if ((org.w3c.dom.Node)nodeList.item(i).getNextSibling() != null) {
							org.w3c.dom.Node targetElementDOMNode = (org.w3c.dom.Node)nodeList.item(i).getNextSibling().getFirstChild();
							if (targetElementDOMNode != null && targetElementDOMNode.getParentNode().getLocalName().equalsIgnoreCase("reversePropertyName")) {
								String[] strArray = targetElementDOMNode.getNodeValue().split(":");
								reversePropertyName = strArray[1];
							};
						}	    				
					} 			    		
				}	
			}
		}		

		final String reversePropertyName2 = reversePropertyName;
		// parse target featureType of this featureProperty		
		decl.getXSElementDecl().visit(new SchemaVisitor() {
			@Override
			public void modelGroup(XSModelGroup modelGroup) {
				for (XSParticle p : modelGroup.getChildren()) {
					XSTerm pterm = p.getTerm();
					if (pterm.isElementDecl()) {							 
						XSElementDecl childElementDecl = (XSElementDecl) pterm;
						ADEschemaElement childDecl = new ADEschemaElement(childElementDecl, schemaHandler.getSchema(childElementDecl.getTargetNamespace()));	                   	                        
						Node childNode = getOrCreateElementTypeNode(childDecl);     
						createArc(GraphNodeArcType.TargetType, propertyNode, childNode);

						// searching reverse property node in the target class node		
						if (reversePropertyName2 != null) {
							if (childNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexType)) {
								Iterator<Arc> iter = childNode.getOutgoingArcs();
								while (iter.hasNext()) {
									Arc arc = iter.next();
									if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.Contains)) {
										Node reversePropertyNode = (Node) arc.getTarget();	        							
										if (reversePropertyNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexTypeProperty)) {
											String propertyName = (String)reversePropertyNode.getAttribute().getValueAt("name");
											if (propertyName.equalsIgnoreCase(reversePropertyName2)) {
												createArc(GraphNodeArcType.ReverseProperty, propertyNode, reversePropertyNode);
												createArc(GraphNodeArcType.ReverseProperty, reversePropertyNode, propertyNode);
											}
										}
									}
								}
							}
						}	        					                
					}
				}						
			}	
		});			

	}

	private void processComplexAttributeNode(Node propertyNode, ADEschemaElement decl) {
		String propertyTypeName = decl.getXSElementDecl().getType().getName();
		ComplexAttributeType complexAttributeType = ADEschemaHelper.ComplexAttributeTypes.get(propertyTypeName);			
		List<SimpleAttribute> simpleAttributeList = complexAttributeType.getSimpleAttributes();
		Iterator<SimpleAttribute> iter = simpleAttributeList.iterator();

		while (iter.hasNext()) {
			SimpleAttribute simpleAttribute = iter.next();

			String path = null;
			if (simpleAttribute.isXMLAttribute())
				path = "@" + simpleAttribute.getPropertyName();
			else
				path = "." + simpleAttribute.getPropertyName();

			String namespaceUri = simpleAttribute.getNamespaceUri();
			boolean isForeign = !schema.getNamespaceURI().equalsIgnoreCase(namespaceUri);

			String name = null;
			if (simpleAttribute.isXMLAttribute())
				name = "_" + simpleAttribute.getPropertyName();
			else
				name = simpleAttribute.getPropertyName();

			String type = simpleAttribute.getPropertyTypeName();

			int minOccurs = simpleAttribute.getMinOccurs();
			int maxOccurs = simpleAttribute.getMaxOccurs();


			Node simpleAttributeNode = this.createSimpleAttributeNode(GraphNodeArcType.SimpleAttribute, path, isForeign, name, minOccurs, maxOccurs, namespaceUri, type);

			createArc(GraphNodeArcType.Contains, propertyNode, simpleAttributeNode);	
		}
	}

	private void createTableForCityGMLClass(String citygmlNamespaceUri, String className, Node classNode) {
		// create a global class table for the respective CityGML class
		Node tableNode = this.createNode(GraphNodeArcType.DataTable);
		AttrInstance attrInstance = tableNode.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;

		ValueMember attr1 = (ValueMember) valueTuple.getValueMemberAt("name");
		attr1.setExprAsObject(ADEschemaHelper.CityDB_Tables.get(new QName(citygmlNamespaceUri, className)));

		// create a primary key column linking with the class table
		Node pkColumnNode = this.createNode(GraphNodeArcType.PrimaryKeyColumn);
		this.createArc(GraphNodeArcType.BelongsTo, pkColumnNode, tableNode);

		// bridge CityGML class and 3DCityDB table
		createArc(GraphNodeArcType.MapsTo, classNode, tableNode);

	}

	private Node createNode(String nodeTypeName) {
		Type nodeType = graphGrammar.getTypeSet().getTypeByName(nodeTypeName);
		Graph graph = graphGrammar.getGraph();
		Node node = null;    	
		try { 
			node = graph.createNode(nodeType);
		}
		catch (Exception ex) { 
			ex.printStackTrace();
		}   	
		return node;
	}

	private Arc createArc(String arcTypeName, Node fromNode, Node toNode) {
		Type arcType = graphGrammar.getTypeSet().getTypeByName(arcTypeName);
		Graph graph = graphGrammar.getGraph();
		Arc arc = null;		
		try { 
			arc = graph.createArc(arcType, fromNode, toNode);
		}
		catch (TypeException ex) { 
			ex.printStackTrace();
		}		
		return arc;
	}
	
	private String getTaggedValueFromXMLAnnotation(XSElementDecl decl, String tagName) {
		XSAnnotation annotation = decl.getAnnotation();
		if (annotation != null) {
			Element annotationElement = (Element) annotation.getAnnotation();       	
			if (annotationElement != null) {
				NodeList annotationNodeList = annotationElement.getElementsByTagName("appinfo");
				if (annotationNodeList.getLength() > 0) {
					NodeList appinfoNodeList = annotationNodeList.item(0).getChildNodes();
					for (int i = 0; i < appinfoNodeList.getLength(); i++) {
						org.w3c.dom.Node taggedValueNode = appinfoNodeList.item(0).getNextSibling();
						org.w3c.dom.Node node = taggedValueNode.getAttributes().getNamedItem("tag");
						if (node != null) {
							String taggedValueName = node.getNodeValue();
							if (taggedValueName.equalsIgnoreCase(tagName)) {
								return taggedValueNode.getFirstChild().getNodeValue();
							}	
						}						
					}	    			
				}
			}
		}
		return null;
	}

}
