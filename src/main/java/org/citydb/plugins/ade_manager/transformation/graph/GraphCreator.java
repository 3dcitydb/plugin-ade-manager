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
import agg.xt_basis.*;
import com.sun.xml.xsom.*;
import org.citydb.core.database.schema.mapping.GeometryType;
import org.citydb.core.database.schema.mapping.SimpleType;
import org.citydb.plugins.ade_manager.transformation.graph.ADEschemaHelper.ComplexAttributeType;
import org.citydb.plugins.ade_manager.transformation.graph.ADEschemaHelper.SimpleAttribute;
import org.citydb.util.log.Logger;
import org.citygml4j.xml.schema.Schema;
import org.citygml4j.xml.schema.SchemaHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GraphCreator {
	private GraGra graphGrammar;
	private SchemaHandler schemaHandler;
	private Schema schema;
	private List<String> namespaces;
	private Map<String, Node> globalClassNodes;
	private Map<String, ADEschemaElement> xsTypeElementMap;
	private Node hostSchemaNode;
	private final Logger LOG = Logger.getInstance();

	public GraphCreator(List<String> namespaces, SchemaHandler schemaHandler, GraGra graphGrammar){
		this.schemaHandler = schemaHandler;			
		this.namespaces = namespaces;
		this.graphGrammar = graphGrammar;	
	}

	public void createGraph() {
		for (String namespace : namespaces) {
			this.schema = schemaHandler.getSchema(namespace);
			
			// Create HostSchema Node		
			String namespaceUri = this.schema.getNamespaceURI();
			this.hostSchemaNode = this.createNode(GraphNodeArcType.Schema);
			AttrInstance attrInstance = hostSchemaNode.getAttribute();
			ValueTuple valueTuple = (ValueTuple) attrInstance;		
			ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("name");
			attr.setExprAsObject(namespaceUri);
			attr = (ValueMember) valueTuple.getValueMemberAt("namespaceUri");		
			attr.setExprAsObject(namespaceUri);

			Map<String, XSElementDecl> xsElementDecls = schema.getXSSchema().getElementDecls();
			Map<String, XSComplexType> xsComplexTypes = schema.getXSSchema().getComplexTypes();
			xsTypeElementMap = new HashMap<>();
			for (XSElementDecl decl : xsElementDecls.values()) {
				XSType elementType = decl.getType();
				ADEschemaElement adeElement = new ADEschemaElement(decl, schema);
				if ((adeElement.isAbstractGML() || adeElement.isFeature() || adeElement.isComplexDataType()) && 
						xsComplexTypes.containsKey(elementType.getName()) && !adeElement.isADEHookElement()) {
					xsTypeElementMap.put(elementType.getName(), adeElement);
				}
			}
			
			// Create class Nodes and put them into a internal map
			globalClassNodes = new HashMap<String, Node>();
			Iterator<Entry<String, XSElementDecl>> iter = xsElementDecls.entrySet().iterator();		
			while (iter.hasNext()) {
				Entry<String, XSElementDecl> elementDeclEntry = (Entry<String, XSElementDecl>) iter.next();
				XSElementDecl xsElementDecl = elementDeclEntry.getValue();
				this.parseGlobalClassElement(xsElementDecl);
			} 
		}		
	}

	private void parseGlobalClassElement(XSElementDecl xsElementDecl) {								
		ADEschemaElement decl = new ADEschemaElement(xsElementDecl, schema);

		if (decl.isADEHookElement()) {
			this.parseADEHookElement(xsElementDecl);
		}
		else {
			if (decl.isAbstractGML() || decl.isFeature() || decl.isComplexDataType() || decl.isUnion()){			
				// get or create graph node for the global class					
				Node classNode = this.getOrCreateElementTypeNode(decl);	
				
				if (!decl.isDerivedFromOtherDomains() || decl.isAbstractGML() || decl.isFeature()) {
					// process extension
					XSElementDecl parentXsElementDecl = xsElementDecl.getSubstAffiliation();
					if (parentXsElementDecl != null) {					
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
									String ignoreStr = getTaggedValueFromXMLAnnotation(propertyDecl, "ignore");
									if (!"true".equalsIgnoreCase(ignoreStr)) {
										parseLocalPropertyElement(propertyDecl, classNode, minOccurs, maxOccurs);
									}
								} else if (pterm.isModelGroup() && pterm.asModelGroup().getCompositor() == XSModelGroup.CHOICE) {
									for (XSParticle particle : pterm.asModelGroup().getChildren()) {
										if (particle.getTerm().isElementDecl()) {
											XSElementDecl propertyDecl = particle.getTerm().asElementDecl();
											String ignoreStr = getTaggedValueFromXMLAnnotation(propertyDecl, "ignore");
											if (!"true".equalsIgnoreCase(ignoreStr)) {
												parseLocalPropertyElement(propertyDecl, classNode, 0, 1);
											}
										}
									}
								}
							}
						}				
					});
				}	
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

				// create property node with the following default minOccurs and maxOccurs values
				int minOccurs = 0;
				int maxOccurs = -1;
				
				String ADEHookMinOccurs = getTaggedValueFromXMLAnnotation(adeHookXsElementDecl, "minOccurs");
				String ADEHookMaxOccurs = getTaggedValueFromXMLAnnotation(adeHookXsElementDecl, "maxOccurs");
				if (ADEHookMinOccurs != null) {					
					try {
						minOccurs = Integer.parseInt(ADEHookMinOccurs);
					} catch (NumberFormatException nfe) {
						LOG.warn("The ADE hook property '" + adeHookXsElementDecl.getName()
								+ "' has an invalid tagged value for its minOccurs: '" + ADEHookMinOccurs
								+ "', which will be internally set to '0'");
					}
				}
				
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
		if (relationType == null)
			relationType = "association";
		
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
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.ImplicitGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, GeometryType.ABSTRACT_GEOMETRY.value(), propertyXSElementDecl);
		}
		else if (propertyDecl.isBrepGeometryProperty()) {    
			String geometryType = ADEschemaHelper.BrepGeometryPropertyTypes.get(propertyDecl.getXSElementDecl().getType().getName()).value();
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.BrepGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, geometryType, propertyXSElementDecl);
		}
		else if (propertyDecl.isPointOrLineGeometryProperty()) {    	                		
			String geometryType = ADEschemaHelper.PointOrLineGeometryPropertyTypes.get(propertyDecl.getXSElementDecl().getType().getName()).value();
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.PointOrLineGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, geometryType, propertyXSElementDecl);
		}
		else if (propertyDecl.isHybridGeometryProperty()) {    	                			 
			String geometryType = ADEschemaHelper.HybridGeometryPropertyTypes.get(propertyDecl.getXSElementDecl().getType().getName()).value();
			propertyNode = this.createGeometryPropertyNode(GraphNodeArcType.HybridGeometryProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace, geometryType, propertyXSElementDecl);
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
			this.processComplexTypePropertyNode(propertyNode, propertyDecl, true);                 		
		} 
		else if (propertyDecl.isComplexAttribute()) {
			propertyNode = this.createPropertyNode(GraphNodeArcType.ComplexAttribute, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace);
			this.processComplexAttributeNode(propertyNode, propertyDecl);
		}
		else if (propertyDecl.isComplexDataType()) {
			propertyNode = this.createPropertyNode(GraphNodeArcType.ComplexTypeProperty, nameAndPath, isForeign, nameAndPath, minOccurs, maxOccurs, namespace);
			this.setNodeAttributeValue(propertyNode, "relationType", relationType);
			this.processComplexTypePropertyNode(propertyNode, propertyDecl, false);    
		}
		else {
			String propertyTypeName = propertyDecl.getXSElementDecl().getType().getName();
			propertyNode = this.createPropertyNode(GraphNodeArcType.GenericAttribute, nameAndPath, isForeign, propertyTypeName, minOccurs, maxOccurs, namespace);
			LOG.debug("Map Porperty element '" + propertyDecl.getLocalName() + "' onto CLOB column; Type name: \"" + propertyTypeName + "\"");
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
			LOG.debug("Unsupported XML/GML Type '" 
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

	private Node createGeometryPropertyNode (String nodeType, String path, boolean isForeign, String name, int minOccurs, int maxOccurs, String namespace, String geometryType, XSElementDecl propertyElement) {
		Node propertyNode = this.createPropertyNode(nodeType, path, isForeign, name, minOccurs, maxOccurs, namespace);
		AttrInstance attrInstance = propertyNode.getAttribute();
		ValueTuple valueTuple = (ValueTuple) attrInstance;
		ValueMember attr = (ValueMember) valueTuple.getValueMemberAt("geometryType");
		attr.setExprAsObject(geometryType);

		String lodStr = getTaggedValueFromXMLAnnotation(propertyElement, "lod");
		if (lodStr != null) {
			try {
				attr = (ValueMember) valueTuple.getValueMemberAt("lod");
				attr.setExprAsObject(Integer.parseInt(lodStr));
			} catch (NumberFormatException nfe) {
				LOG.warn("The property '" + propertyElement.getName() + "' has an invalid tagged value for its lod: '" + lodStr + "'; It must be between 0 and 4.");
			}
		}

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
		if (childNode.getNumberOfOutgoingArcs() == 0) {
			this.createTableForCityGMLClass(propertyTypeNamespaceUri, className, childNode);
		}
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
			org.w3c.dom.Node targetElementDOMNode = (org.w3c.dom.Node) annotationElement.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "appinfo").item(0).getFirstChild().getNextSibling().getFirstChild();
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

	private void processComplexTypePropertyNode (Node propertyNode, ADEschemaElement decl, boolean isRefElement) {
		XSAnnotation xsAnnotation = decl.getXSElementDecl().getAnnotation();
		String reversePropertyName = null;
		if (xsAnnotation != null) {
			Element annotationElement = (Element) xsAnnotation.getAnnotation();       	
			if (annotationElement != null) {
				org.w3c.dom.Node appinfoItem = (org.w3c.dom.Node) annotationElement.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "appinfo").item(0);
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
		
		if (isRefElement) {
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
		} else {
			String typeName = decl.getXSElementDecl().getType().getName();
			ADEschemaElement adeElement = this.xsTypeElementMap.get(typeName);
			if (adeElement != null) {	                   	                        		
				Node childNode = this.getOrCreateElementTypeNode(adeElement);	    
				createArc(GraphNodeArcType.TargetType, propertyNode, childNode);				
			}
		}
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

			boolean isForeign = false;
			if (namespaceUri == null)
				namespaceUri = schema.getNamespaceURI();
			else
			    isForeign = !schema.getNamespaceURI().equalsIgnoreCase(namespaceUri);

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
		ValueMember attr2 = (ValueMember) valueTuple.getValueMemberAt("isADE");
		attr2.setExprAsObject(false);
		
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
				NodeList appInfoNodeList = annotationElement.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "appinfo");
				if (appInfoNodeList.getLength() > 0) {
					for (int j = 0; j < appInfoNodeList.getLength(); j++) {
						NodeList taggedValueNodeList = appInfoNodeList.item(j).getChildNodes();
						for (int i = 0; i < taggedValueNodeList.getLength(); i++) {
							org.w3c.dom.Node taggedValueNode = taggedValueNodeList.item(i).getNextSibling();
							if (taggedValueNode != null) {
								NamedNodeMap attribute = taggedValueNode.getAttributes();
								if (attribute != null) {
									org.w3c.dom.Node node = attribute.getNamedItem("tag");
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
				}
			}
		}
		return null;
	}

}
