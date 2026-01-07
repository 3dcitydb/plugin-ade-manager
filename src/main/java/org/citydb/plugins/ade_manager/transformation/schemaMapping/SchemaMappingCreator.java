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
package org.citydb.plugins.ade_manager.transformation.schemaMapping;

import agg.attribute.AttrInstance;
import agg.xt_basis.Arc;
import agg.xt_basis.GraGra;
import agg.xt_basis.Node;
import agg.xt_basis.Type;
import org.citydb.core.database.schema.mapping.*;
import org.citydb.core.database.schema.util.SchemaMappingUtil;
import org.citydb.core.util.CoreConstants;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.graph.GraphNodeArcType;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.citydb.plugins.ade_manager.transformation.graph.GraphNodeArcType.Join;

public class SchemaMappingCreator {	
	private final String schemaMappingFoldername = "schema-mapping";
	private final String schemaMappingFilename = "schema-mapping.xml";
	
	private GraGra graphGrammar;	
	private SchemaMapping citygmlSchemaMapping;
	private SchemaMapping adeSchemaMapping;
	
	private ConfigImpl config;
	private int initialObjectclassId;
	
	public SchemaMappingCreator(GraGra graphGrammar, ConfigImpl config) {
		this.graphGrammar = graphGrammar;
		this.config = config;				
	}
	
	public SchemaMapping createSchemaMapping() throws Exception {		
		initialObjectclassId = config.getInitialObjectclassId();
		
		citygmlSchemaMapping = SchemaMappingUtil.getInstance().unmarshal(CoreConstants.CITYDB_SCHEMA_MAPPING_FILE);

		adeSchemaMapping = new SchemaMapping();
		adeSchemaMapping.setMetadata(this.generateMetadata());
		generateApplicationSchema(adeSchemaMapping);	
		for (AppSchema appSchema : adeSchemaMapping.getSchemas()) {
			adeSchemaMapping.addSchema(appSchema);
			this.generateComplexTypeMapping(adeSchemaMapping, appSchema);
		}

		processTopLevelFeatures(adeSchemaMapping);
		
		String outputFolderPath = config.getTransformationOutputPath();
		File schemaMappingRootDirectory = new File(outputFolderPath, schemaMappingFoldername);
		if (!schemaMappingRootDirectory.exists()) 
			schemaMappingRootDirectory.mkdir();
		
		File mappingSchemaFile = new File(schemaMappingRootDirectory, schemaMappingFilename);	
		FileWriter writer = new FileWriter(mappingSchemaFile);
		SchemaMappingUtil.getInstance().marshal(adeSchemaMapping, writer);
		writer.close();
		
		return adeSchemaMapping;
	} 
	
	private Metadata generateMetadata() {
		Metadata metadata = new Metadata(config.getAdeName(), config.getAdeDbPrefix());
		metadata.setDescription(config.getAdeDescription());
		metadata.setVersion(config.getAdeVersion());
		
		return metadata;
	}
	
	private void generateApplicationSchema(SchemaMapping adeSchemaMapping) throws SchemaMappingException {
		List<Node> schemaNodes = this.getSchemaNode();			
		for (int i = 0; i < schemaNodes.size(); i++) {
			Node schemaNode = schemaNodes.get(i);
			AttrInstance attrInstance = schemaNode.getAttribute();		
			String namespaceUri = (String) attrInstance.getValueAt("namespaceUri");		
			String dbPrefix = config.getAdeDbPrefix();	
			String schemaId = null;
			if (schemaNodes.size() > 1)
				schemaId = dbPrefix + "_" + i;
			else
				schemaId = dbPrefix; // legacy
			AppSchema appSchema = new AppSchema(schemaId, adeSchemaMapping);		
			Namespace namespace = new Namespace(namespaceUri, CityGMLContext.CITYGML_2_0);
			appSchema.addNamespace(namespace);	
			appSchema.setIsADERoot(true);
			adeSchemaMapping.addSchema(appSchema);
		}
	}
	
	private List<Node> getSchemaNode() {
		Enumeration<Type> e = this.graphGrammar.getTypes();
		
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.Schema)) {
				return this.graphGrammar.getGraph().getNodes(nodeType);			
			};
		}
		
		return new ArrayList<>();
	}
	
	private void generateComplexTypeMapping(SchemaMapping schemaMapping, AppSchema appSchema) throws SchemaMappingException{
		List<Node> nodeList = getFeautreOrObjectOrComplexTypeNodes(appSchema);
		Iterator<Node> iter = nodeList.iterator();

		while (iter.hasNext()) {
			Node objectNode = iter.next();
			AbstractType<?> featureOrObjectOrComplexType = this.getOrCreateFeatureOrObjectOrComplexType(objectNode, schemaMapping);
			
			if (featureOrObjectOrComplexType == null)
				continue;
			
			processFeatureOrObjectOrComplexType(objectNode, featureOrObjectOrComplexType, schemaMapping);
			
			if (checkADEHookClass(objectNode)){ 
				AbstractExtension<?> extension = featureOrObjectOrComplexType.getExtension();
				String table = featureOrObjectOrComplexType.getTable();
				Join join = new Join(table, "ID", "ID", TableRole.CHILD);
				PropertyInjection propertyInjection = new PropertyInjection(table, join);
				propertyInjection.setDefaultBase((FeatureType) extension.getBase());
				schemaMapping.addPropertyInjection(propertyInjection);
				
				List<AbstractProperty> properties = featureOrObjectOrComplexType.getProperties();
				Iterator<AbstractProperty> propertyIter = properties.iterator();
				while(propertyIter.hasNext()) {
					AbstractProperty property = propertyIter.next();
					InjectedProperty injectedProperty = convertPropertyToInjectedProperty(property);
					if (injectedProperty != null)
						propertyInjection.addProperty(injectedProperty);	
				}
			}				
		}		
	}
	
	private void processFeatureOrObjectOrComplexType(Node objectNode, AbstractType<?> featureOrObjectOrComplexType, SchemaMapping schemaMapping) throws SchemaMappingException {
		Iterator<Arc> arcIter = objectNode.getOutgoingArcs();
		
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node targetNode = (Node) arc.getTarget();				
			AbstractProperty property = null;
			// process extension
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.Extension)) {
				this.generateExtension(featureOrObjectOrComplexType, targetNode, schemaMapping);
			}	
			// process featureOrObjectOrDataProperty
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexTypeProperty)) {
				property = this.generateFeatureOrObjectOrComplexTypeProperty(featureOrObjectOrComplexType, targetNode, schemaMapping);
			}	
			// process simple attribute
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.GenericAttribute) || 
					targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.SimpleAttribute) || 
					targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.EnumerationProperty)) {
				property = this.generateSimpleAttribute(targetNode);
				featureOrObjectOrComplexType.addProperty(property);
			}	
			// process complex basic data property
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexAttribute)) {
				property = this.generateComplexAttribute(featureOrObjectOrComplexType, targetNode);
			}	
			// process geometry property
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.BrepGeometryProperty) 
					|| targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.PointOrLineGeometryProperty) 
						|| targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.HybridGeometryProperty)) {
				property = this.generateGeometryProperty(featureOrObjectOrComplexType, targetNode);
			}
			// process implicit geometry property
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ImplicitGeometryProperty)) {
				property = this.generateImplicitGeometryProperty(featureOrObjectOrComplexType, targetNode);
			}
			
			if (property != null) {
				int minOccurs = (int) targetNode.getAttribute().getValueAt("minOccurs");
				property.setMinOccurs(minOccurs);
				int maxOccurs = (int) targetNode.getAttribute().getValueAt("maxOccurs");
				if (maxOccurs != -1)
					property.setMaxOccurs(maxOccurs);
			}
		}		
	}
	
	private AbstractType<?> getOrCreateFeatureOrObjectOrComplexType(Node featureObjectNode, SchemaMapping schemaMapping) throws SchemaMappingException{
		String path = (String) featureObjectNode.getAttribute().getValueAt("path");	
		String namespaceUri = (String) featureObjectNode.getAttribute().getValueAt("namespaceUri");
		AppSchema appSchema = getAppSchema(namespaceUri);
		
		if (checkInlineType(featureObjectNode)) 
			return new ComplexType(path, getAppSchema(namespaceUri), schemaMapping);
					
		AbstractType<?> featureOrObjectOrComplexType = citygmlSchemaMapping.getAbstractObjectType(new QName(namespaceUri, path));
		
		if (featureOrObjectOrComplexType != null) {
			return featureOrObjectOrComplexType;
		}
		else {
			featureOrObjectOrComplexType = citygmlSchemaMapping.getComplexType(new QName(namespaceUri, path));
			if (featureOrObjectOrComplexType != null)
				return featureOrObjectOrComplexType;
		}			
			
		featureOrObjectOrComplexType = schemaMapping.getAbstractObjectType(new QName(namespaceUri, path));
		if (featureOrObjectOrComplexType != null) {
			return featureOrObjectOrComplexType;
		}
		else {
			featureOrObjectOrComplexType = schemaMapping.getComplexType(new QName(namespaceUri, path));
			if (featureOrObjectOrComplexType != null)
				return featureOrObjectOrComplexType;
		}	
		
		if (this.getCityGMLComplexAttributeType(path) != null)
			return null;
		 		
		String tableName = null;
		Iterator<Arc> arcIter = featureObjectNode.getOutgoingArcs();		
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node targetNode = (Node) arc.getTarget();							
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.DataTable)) {
				tableName = (String) targetNode.getAttribute().getValueAt("name");
			}		
		}		
		
		String featureOrObjectId = appSchema.getId() + "_" + (String) featureObjectNode.getAttribute().getValueAt("name");
		boolean isAbstract = (boolean) featureObjectNode.getAttribute().getValueAt("isAbstract");			
		String derivedFrom = (String) featureObjectNode.getAttribute().getValueAt("derivedFrom");			
						
		if (derivedFrom.equalsIgnoreCase("_Feature") || derivedFrom.equalsIgnoreCase("_CityObject")) {
			featureOrObjectOrComplexType = new FeatureType(featureOrObjectId, path, tableName, initialObjectclassId++, appSchema, schemaMapping);
			boolean topLevel = (boolean) featureObjectNode.getAttribute().getValueAt("topLevel");
			if (topLevel)
				((FeatureType) featureOrObjectOrComplexType).setTopLevel(topLevel);			
			schemaMapping.addFeatureType((FeatureType) featureOrObjectOrComplexType);
		}
		else if (derivedFrom.equalsIgnoreCase("_GML")) {
			featureOrObjectOrComplexType = new ObjectType(featureOrObjectId, path, tableName, initialObjectclassId++, appSchema, schemaMapping);
			schemaMapping.addObjectType((ObjectType) featureOrObjectOrComplexType);
		}
		else if (derivedFrom.equalsIgnoreCase("_Object")) {
			featureOrObjectOrComplexType = new ComplexType(path, appSchema, schemaMapping);
			featureOrObjectOrComplexType.setObjectClassId(initialObjectclassId++);
			featureOrObjectOrComplexType.setId(featureOrObjectId);
			featureOrObjectOrComplexType.setTable(tableName);
			schemaMapping.addComplexType((ComplexType) featureOrObjectOrComplexType);			
		} else if (derivedFrom.equalsIgnoreCase("HookClass")) {
			featureOrObjectOrComplexType = new FeatureType(featureOrObjectId, path, tableName, 0, appSchema, schemaMapping);
		}
				
		if (featureOrObjectOrComplexType != null)
			featureOrObjectOrComplexType.setAbstract(isAbstract);	

		return featureOrObjectOrComplexType;
	}
	
	private List<Node> getFeautreOrObjectOrComplexTypeNodes(AppSchema appSchema) {
		List<Node> result = new ArrayList<>();
		Enumeration<Type> e = this.graphGrammar.getTypes();
		String namespace = appSchema.getNamespaces().get(0).getURI();
		
		while(e.hasMoreElements()){
			Type nodeType = e.nextElement();				
			if (nodeType.getName().equalsIgnoreCase(GraphNodeArcType.ComplexType)) {
				List<Node> nodes = this.graphGrammar.getGraph().getNodes(nodeType);
				result = nodes.stream()
						.filter(node -> namespace.equalsIgnoreCase((String) node.getAttribute().getValueAt("namespaceUri")))
						.collect(Collectors.toList()); 
				return result;
			};
		}
		
		return result;
	}
	
	private void generateExtension(AbstractType<?> subType, Node extensionNode, SchemaMapping schemaMapping) throws SchemaMappingException{
		Iterator<Arc> arcIter = extensionNode.getOutgoingArcs();
		AbstractExtension<?> extension = null;
		
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node targetNode = (Node) arc.getTarget();
	
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexType)) {
				AbstractType<?> superType = this.getOrCreateFeatureOrObjectOrComplexType(targetNode, schemaMapping);
				if (subType instanceof FeatureType) {
					FeatureTypeExtension featureTypeExtension = new FeatureTypeExtension((FeatureType) superType);
					((FeatureType) subType).setExtension(featureTypeExtension);
					extension = featureTypeExtension;
				}
				else if (subType instanceof ObjectType) {
					ObjectTypeExtension objectTypeExtension = new ObjectTypeExtension((ObjectType) superType);
					((ObjectType) subType).setExtension(objectTypeExtension);
					extension = objectTypeExtension;
				}
				else if (subType instanceof ComplexType) {
					ComplexTypeExtension complexTypeExtension = new ComplexTypeExtension((ComplexType) superType);
					((ComplexType) subType).setExtension((ComplexTypeExtension) complexTypeExtension);
					extension = complexTypeExtension;
				}
			}
			
			if (targetNode.getType().getName().equalsIgnoreCase(Join) && extension != null) {
				Join extensionJoin = this.createJoin(subType.getTable(), targetNode);
				extension.setJoin(extensionJoin);
			}
		}
	}
	
	private Join createJoin(String localTableName, Node joinNode) {
		Iterator<Arc> iter = joinNode.getOutgoingArcs();

		String fkColumnName = null;
		String pkColumnName = null;		
		String fkTableName = null;
		String pkTableName = null;
		Node fkTableNode = null;
		Node pkTableNode = null;
		TreeHierarchy treeHierarchy = null;

		while (iter.hasNext()) {
			Arc arc = iter.next();
			if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.JoinFrom)) {
				Node joinFromColumnNode = (Node) arc.getTarget();
				fkColumnName = (String)joinFromColumnNode.getAttribute().getValueAt("name");
				fkTableNode = (Node)joinFromColumnNode.getOutgoingArcs().next().getTarget();				
				fkTableName= (String)fkTableNode.getAttribute().getValueAt("name");
			}
			else if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.JoinTo)) {
				Node joinToColumnNode = (Node) arc.getTarget();
				pkColumnName = (String)joinToColumnNode.getAttribute().getValueAt("name");
				pkTableNode = (Node)joinToColumnNode.getOutgoingArcs().next().getTarget();				
				pkTableName= (String)pkTableNode.getAttribute().getValueAt("name");
			}
			else if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.TreeHierarchy)) {
				Node rootJoinNode = (Node) arc.getTarget();
				Join rootJoin = createJoin(localTableName, rootJoinNode);
				String rootColumnName = rootJoin.getFromColumn();
				treeHierarchy = new TreeHierarchy(rootColumnName);
			}
		}

		Join join = null;
		Node joinToTableNode = null;
		if (!fkTableName.equalsIgnoreCase(pkTableName)) {
			if (fkTableName.equalsIgnoreCase(localTableName)) {
				join = new Join(pkTableName, fkColumnName, pkColumnName, TableRole.PARENT);				
				joinToTableNode = pkTableNode;
			} 
			else {
				join = new Join(fkTableName, pkColumnName, fkColumnName, TableRole.CHILD);
				joinToTableNode = fkTableNode;
			}
		}
		else {
			join = new Join(fkTableName, fkColumnName, pkColumnName, TableRole.PARENT);
			joinToTableNode = fkTableNode;
			
			iter = joinNode.getIncomingArcs();
			while (iter.hasNext()) {
				Arc arc = iter.next();
				if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.MapsTo)) {
					Node propertyNode = (Node) arc.getSource();
					if (propertyNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexTypeProperty)) {
						int maxOccurs = (int)propertyNode.getAttribute().getValueAt("maxOccurs");
						if (maxOccurs > 1 || maxOccurs == -1) {
							join = new Join(fkTableName, pkColumnName, fkColumnName, TableRole.CHILD);
							joinToTableNode = fkTableNode;
						}						
					}				
				}
			}			
		}
		
		boolean hasObjectclassIdColumn = false;
		iter = joinToTableNode.getIncomingArcs();
		while (iter.hasNext()) {
			Arc arc = iter.next();
			if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.BelongsTo)) {
				Node columnNode = (Node) arc.getSource();
				if (columnNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ObjectClassIDColumn)) {
					hasObjectclassIdColumn = true;
					break;					
				}	
			}
		}
		
		boolean joinIsMappedFromInheritance = false;
		iter = joinNode.getIncomingArcs();
		while (iter.hasNext()) {
			Arc arc = iter.next();
			if (arc.getType().getName().equalsIgnoreCase(GraphNodeArcType.MapsTo)) {
				Node propertyNode = (Node) arc.getSource();
				if (propertyNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.Extension)) {
					joinIsMappedFromInheritance = true;				
				}				
			}
		}	
		
		if (hasObjectclassIdColumn && !joinIsMappedFromInheritance) {
			join.addCondition(new Condition("objectclass_id", "${target.objectclass_id}", SimpleType.INTEGER));
		}	

		// add treeHierarchy
		if (treeHierarchy != null)
			join.setTreeHierarchy(treeHierarchy);
		
		return join;
	}

	private ComplexAttributeType getCityGMLComplexAttributeType(String id) {
		for (ComplexAttributeType attributeType : citygmlSchemaMapping.getComplexAttributeTypes()) {
			if (attributeType.getId().equals(id))
				return attributeType;
		}

		return null;
	} 
	
	private AbstractProperty generateFeatureOrObjectOrComplexTypeProperty(AbstractType<?> localType, Node featureOrObjectOrComplexTypePropertyNode, 
			SchemaMapping schemaMapping) throws SchemaMappingException {

		AbstractProperty property = null;
		Iterator<Arc> arcIter = featureOrObjectOrComplexTypePropertyNode.getOutgoingArcs();
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();

			Node targetNode = (Node) arc.getTarget();
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.ComplexType)) {
								
				String propertyPath = (String) featureOrObjectOrComplexTypePropertyNode.getAttribute().getValueAt("path");
				String namespaceUri = (String) featureOrObjectOrComplexTypePropertyNode.getAttribute().getValueAt("namespaceUri");
				AppSchema appSchema = getAppSchema(namespaceUri);

				String targetTypeName = (String) targetNode.getAttribute().getValueAt("name");
				ComplexAttributeType targetAttributeType = this.getCityGMLComplexAttributeType(targetTypeName);
				
				if (targetAttributeType != null) {
					ComplexAttribute complexAttribute = new ComplexAttribute(propertyPath, appSchema);
					complexAttribute.setRefType(targetAttributeType);
					if (targetNode.getType().getName().equalsIgnoreCase(Join)) {
						Join propertyJoin = this.createJoin(localType.getTable(), targetNode);
						complexAttribute.setJoin(propertyJoin);
					}
					property = complexAttribute;
				}
				else {
					AbstractType<?> targetType = this.getOrCreateFeatureOrObjectOrComplexType(targetNode, schemaMapping);
													
					if (targetType instanceof FeatureType) {
						property = new FeatureProperty(propertyPath, (FeatureType) targetType, appSchema);
					}
					else if (targetType instanceof ObjectType) {
						property = new ObjectProperty(propertyPath, (ObjectType) targetType, appSchema);
					}
					else if (targetType instanceof ComplexType) {
						property = new ComplexProperty(propertyPath, appSchema);
						if (checkInlineType(targetNode)) {
							((ComplexProperty)property).setInlineType((ComplexType) targetType);
							this.processFeatureOrObjectOrComplexType(targetNode, targetType, schemaMapping);
						}						
						else {
							((ComplexProperty)property).setRefType((ComplexType) targetType);
						}						
					}	
					
					if (property instanceof AbstractRefTypeProperty)
						setRelationTypeForRefTypeProperty(featureOrObjectOrComplexTypePropertyNode, (AbstractRefTypeProperty<?>) property);
				}

				localType.addProperty(property);				
			}
			
			if (targetNode.getType().getName().equalsIgnoreCase(Join)) {
				Join propertyJoin = this.createJoin(localType.getTable(), targetNode);
				if (property instanceof ComplexAttribute)
					((ComplexAttribute) property).setJoin(propertyJoin);
				else
					((AbstractTypeProperty<?>) property).setJoin(propertyJoin);
			}

			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.JoinTable)) {
				JoinTable propertyJoinTable = this.createJoinTable(targetNode, localType.getTable());
				if (property instanceof AbstractTypeProperty<?>) {
					((AbstractTypeProperty<?>) property).setJoin(propertyJoinTable);
				}									
			}
		}	
		
		return property;
	}
	
	private JoinTable createJoinTable(Node joinTableNode, String parentTableName) {
		String tableName = (String)joinTableNode.getAttribute().getValueAt("name");
		JoinTable joinTable = new JoinTable(tableName);

		Iterator<Arc> arcIter = joinTableNode.getIncomingArcs();
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node primarykeyNode = (Node) arc.getSource();
			if (primarykeyNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.PrimaryKeyColumn)) {
				Iterator<Arc> arcIter2 = primarykeyNode.getIncomingArcs();
				while (arcIter2.hasNext()) {
					Arc arc2 = arcIter2.next();
					Node joinNode = (Node) arc2.getSource();
					if (joinNode.getType().getName().equalsIgnoreCase(Join)) {
						Join join = this.createJoin(tableName, joinNode);
						if (join.getTable().equalsIgnoreCase(parentTableName))
							joinTable.setJoin(join);
						else
							joinTable.setInverseJoin(join);
					}					
				}				
			}
		}
		
		return joinTable;
	}
	
	private boolean checkInlineType(Node featureOrObjectOrComplexTypeNode) {
		Object isInline = featureOrObjectOrComplexTypeNode.getAttribute().getValueAt("isInline");
		
		if (isInline != null) {
			if ((boolean)isInline)
				return true;
		}
		
		return false;
	}
	
	private boolean checkADEHookClass(Node featureOrObjectOrComplexTypeNode) {
		String derivedFrom = (String) featureOrObjectOrComplexTypeNode.getAttribute().getValueAt("derivedFrom");
		
		return derivedFrom.equalsIgnoreCase("HookClass");
	}
	
	private SimpleAttribute generateSimpleAttribute(Node simpleAttributeNode) {
		String path = (String) simpleAttributeNode.getAttribute().getValueAt("path");
		String typeName = (String) simpleAttributeNode.getAttribute().getValueAt("primitiveDataType");
		String namespaceUri = (String) simpleAttributeNode.getAttribute().getValueAt("namespaceUri");
		AppSchema appSchema = getAppSchema(namespaceUri);

		Iterator<Arc> arcIter = simpleAttributeNode.getOutgoingArcs();
		
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node targetNode = (Node) arc.getTarget();
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.NormalDataColumn)) {
				String columnName = (String) targetNode.getAttribute().getValueAt("name");
				SimpleAttribute attribute = new SimpleAttribute(path, columnName, SimpleType.fromValue(typeName), appSchema);
				return attribute;
			}
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.GenericDataColumn)) {
				String columnName = (String) targetNode.getAttribute().getValueAt("name");
				SimpleAttribute attribute = new SimpleAttribute(path, columnName, SimpleType.CLOB, appSchema);
				return attribute;
			}
		}		
		
		return null;
	}	
	
	private ComplexAttribute generateComplexAttribute(AbstractType<?> localType, Node complexAttributeNode) {
		String propertyPath = (String) complexAttributeNode.getAttribute().getValueAt("path");
		String namespaceUri = (String) complexAttributeNode.getAttribute().getValueAt("namespaceUri");
		ComplexAttributeType attributeType = new ComplexAttributeType(adeSchemaMapping);
		Iterator<Arc> arcIter = complexAttributeNode.getOutgoingArcs();
		
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node targetNode = (Node) arc.getTarget();
			if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.SimpleAttribute)) {
				SimpleAttribute simpleAttribute = this.generateSimpleAttribute(targetNode);
				attributeType.addAttribute(simpleAttribute);
			}
		}
		
		ComplexAttribute complexAttribute = new ComplexAttribute(propertyPath, getAppSchema(namespaceUri));
		complexAttribute.setInlineType(attributeType);
		localType.addProperty(complexAttribute);	
		
		return complexAttribute;
	}
	
	private GeometryProperty generateGeometryProperty(AbstractType<?> localType, Node geometryPropertyNode) {
		String propertyPath = (String) geometryPropertyNode.getAttribute().getValueAt("path");
		String geometryTypeName = (String) geometryPropertyNode.getAttribute().getValueAt("geometryType");
		String namespaceUri = (String) geometryPropertyNode.getAttribute().getValueAt("namespaceUri");
		GeometryType geometryType = GeometryType.fromValue(geometryTypeName);
		
		GeometryProperty geometryProperty = new GeometryProperty(propertyPath, geometryType, getAppSchema(namespaceUri));
		Iterator<Arc> arcIter = geometryPropertyNode.getOutgoingArcs();
		
		while(arcIter.hasNext()) {
			Arc arc = arcIter.next();
			Node targetNode = (Node) arc.getTarget();
			String columnName = (String) targetNode.getAttribute().getValueAt("name");
			if (columnName != null) {
				if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.RefGeometryColumn))
					geometryProperty.setRefColumn(columnName);
				if (targetNode.getType().getName().equalsIgnoreCase(GraphNodeArcType.InlineGeometryColumn))
					geometryProperty.setInlineColumn(columnName);
			}			
		}		
		
		localType.addProperty(geometryProperty);

		int lod = getLodFromPropertyName(propertyPath);
		Object lodObject = geometryPropertyNode.getAttribute().getValueAt("lod");
		if (lodObject != null) {
			lod = (int)lodObject;
		}
		if (lod >= 0 && lod <= 4)
			geometryProperty.setLod(lod);

		return geometryProperty;
	}
	
	private ImplicitGeometryProperty generateImplicitGeometryProperty(AbstractType<?> localType, Node geometryPropertyNode) {
		String propertyPath = (String) geometryPropertyNode.getAttribute().getValueAt("path");
		String namespaceUri = (String) geometryPropertyNode.getAttribute().getValueAt("namespaceUri");
		int lod = getLodFromPropertyName(propertyPath);
		ImplicitGeometryProperty implicitGeometryProperty = new ImplicitGeometryProperty(propertyPath, lod, getAppSchema(namespaceUri));
		localType.addProperty(implicitGeometryProperty);

		return implicitGeometryProperty;
	}
	
	private void processTopLevelFeatures(SchemaMapping schemaMapping) {
		List<FeatureType> featureTypes = schemaMapping.getFeatureTypes();
		
		Iterator<FeatureType> iter = featureTypes.iterator();
		while (iter.hasNext()) {
			FeatureType featureType = iter.next();
		
			if (isTopLevel(featureType))
				featureType.setTopLevel(true);
		}
	}
	
	private boolean isTopLevel(FeatureType featureType) {
		FeatureType tempFeatureType = featureType;
		boolean hasTopLevel = false;
		while (tempFeatureType.getExtension() != null) {
			FeatureTypeExtension extension = (FeatureTypeExtension) tempFeatureType.getExtension();
			FeatureType superClass = extension.getBase();
			if (superClass.isTopLevel())
				hasTopLevel = true;
			
			if (hasTopLevel && !featureType.isAbstract()) {
				return true;
			}
			else {
				tempFeatureType = superClass;
			}
		}
		return false;
	}
	
	private void setRelationTypeForRefTypeProperty(Node propertyNode, AbstractRefTypeProperty<?> property) {
		String relationType = (String)propertyNode.getAttribute().getValueAt("relationType");
		if (relationType != null) {
			if (relationType.equalsIgnoreCase("composition"))
				((AbstractRefTypeProperty<?>) property).setRelationType(RelationType.COMPOSITION);
			else if (relationType.equalsIgnoreCase("aggregation"))
				((AbstractRefTypeProperty<?>) property).setRelationType(RelationType.AGGREGATION);
		}	
	}
	
	private InjectedProperty convertPropertyToInjectedProperty(AbstractProperty property) {
		InjectedProperty injectedProperty = null;
		
		String path = property.getPath();
		int minOccurs = property.getMinOccurs();
		Integer maxOccurs = property.getMaxOccurs();
		
		AppSchema schema = property.getSchema();
		if (property instanceof FeatureProperty) {
			FeatureType featureType = ((FeatureProperty) property).getType();
			injectedProperty = new InjectedFeatureProperty(path, featureType, schema);
			AbstractJoin abstractJoin = ((FeatureProperty) property).getJoin();
			RelationType relationType = ((FeatureProperty) property).getRelationType();
			if (abstractJoin instanceof Join)
				((InjectedFeatureProperty)injectedProperty).setJoin((Join) abstractJoin);
			else
				((InjectedFeatureProperty)injectedProperty).setJoin((JoinTable) abstractJoin);		
			((InjectedFeatureProperty)injectedProperty).setRelationType(relationType);
			
			((InjectedFeatureProperty)injectedProperty).setMinOccurs(minOccurs);
			if (maxOccurs != null)
				((InjectedFeatureProperty)injectedProperty).setMaxOccurs(maxOccurs);
			
		}
		else if (property instanceof ObjectProperty) {
			ObjectType objectType = ((ObjectProperty) property).getType();
			injectedProperty = new InjectedObjectProperty(path, objectType, schema);
			AbstractJoin abstractJoin = ((ObjectProperty) property).getJoin();
			RelationType relationType = ((ObjectProperty) property).getRelationType();
			if (abstractJoin instanceof Join)
				((InjectedObjectProperty)injectedProperty).setJoin((Join) abstractJoin);
			else
				((InjectedObjectProperty)injectedProperty).setJoin((JoinTable) abstractJoin);	
			((InjectedObjectProperty)injectedProperty).setRelationType(relationType);
			
			((InjectedObjectProperty)injectedProperty).setMinOccurs(minOccurs);
			if (maxOccurs != null)
				((InjectedObjectProperty)injectedProperty).setMaxOccurs(maxOccurs);
		}
		else if (property instanceof ComplexProperty) {
			ComplexType complexType = ((ComplexProperty) property).getType();
			injectedProperty = new InjectedComplexProperty(path, schema);
			if (complexType.isSetId()) {
				((InjectedComplexProperty)injectedProperty).setRefType(complexType);							
				AbstractJoin abstractJoin = ((ComplexProperty) property).getJoin();
				if (abstractJoin instanceof Join)
					((InjectedComplexProperty)injectedProperty).setJoin((Join) abstractJoin);
				else
					((InjectedComplexProperty)injectedProperty).setJoin((JoinTable) abstractJoin);		
			}
			else {
				((InjectedComplexProperty)injectedProperty).setInlineType(complexType);
			}	
			
			((InjectedComplexProperty)injectedProperty).setMinOccurs(minOccurs);
			if (maxOccurs != null)
				((InjectedComplexProperty)injectedProperty).setMaxOccurs(maxOccurs);
		}
		else if (property instanceof ComplexAttribute) {
			ComplexAttributeType complexAttributeType = ((ComplexAttribute) property).getType();
			injectedProperty = new InjectedComplexAttribute(path, schema);
			((InjectedComplexAttribute)injectedProperty).setInlineType(complexAttributeType);
			
			((InjectedComplexAttribute)injectedProperty).setMinOccurs(minOccurs);
			if (maxOccurs != null)
				((InjectedComplexAttribute)injectedProperty).setMaxOccurs(maxOccurs);
		}
		else if (property instanceof SimpleAttribute) {
			String column = ((SimpleAttribute) property).getColumn();
			SimpleType type = ((SimpleAttribute) property).getType();
			injectedProperty = new InjectedSimpleAttribute(path, column, type, schema);
			
			((InjectedSimpleAttribute)injectedProperty).setMinOccurs(minOccurs);
			if (maxOccurs != null)
				((InjectedSimpleAttribute)injectedProperty).setMaxOccurs(maxOccurs);
			
		}
		else if (property instanceof GeometryProperty) {
			GeometryType type = ((GeometryProperty) property).getType();
			injectedProperty = new InjectedGeometryProperty(path, type, schema);
			if (((GeometryProperty) property).isSetInlineColumn())
				((InjectedGeometryProperty)injectedProperty).setInlineColumn(((GeometryProperty) property).getInlineColumn());
			if (((GeometryProperty) property).isSetRefColumn())
				((InjectedGeometryProperty)injectedProperty).setRefColumn(((GeometryProperty) property).getRefColumn());
			
			((InjectedGeometryProperty)injectedProperty).setMinOccurs(minOccurs);
			if (maxOccurs != null)
				((InjectedGeometryProperty)injectedProperty).setMaxOccurs(maxOccurs);

			if (((GeometryProperty) property).isSetLod())
				((InjectedGeometryProperty)injectedProperty).setLod(((GeometryProperty) property).getLod());
		}
		
		return injectedProperty;
	}

	private AppSchema getAppSchema(String namespaceUri) {
		AppSchema appSchema = adeSchemaMapping.getSchema(namespaceUri);
		if (appSchema == null) {
			appSchema = citygmlSchemaMapping.getSchema(namespaceUri);
		}
		return appSchema;
	}

	private int getLodFromPropertyName(String propertyName) {
		for (int i = 0; i <= 4; i++) {
			if (propertyName.toLowerCase().indexOf("lod" + i) == 0) {
				return i;
			}
		}
		return -1;
	}

}
