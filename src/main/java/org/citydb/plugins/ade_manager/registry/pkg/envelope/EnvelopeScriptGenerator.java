package org.citydb.plugins.ade_manager.registry.pkg.envelope;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.citydb.database.schema.mapping.AbstractExtension;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.AbstractRefTypeProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.ComplexProperty;
import org.citydb.database.schema.mapping.FeatureProperty;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.ObjectProperty;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.DefaultDBScriptGenerator;

public abstract class EnvelopeScriptGenerator extends DefaultDBScriptGenerator {
	protected SchemaMapping schemaMapping; 

	public EnvelopeScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
		this.schemaMapping = adeMetadataManager.getMergedSchemaMapping();
	}

	protected DBSQLScript generateScript(String schemaName) throws SQLException {
		registerEnvelopeFunction("cityobject", schemaName);	
		
		return buildEnvelopeScript();	
	}
	
	protected String getFunctionName(String tableName) {
		return "envl_" + tableName;
	}
	
	protected abstract DBSQLScript buildEnvelopeScript() throws SQLException;
	protected abstract void constructEnvelopeFunction(EnvelopeFunction deleteFunction) throws SQLException;

	protected void registerEnvelopeFunction(String tableName, String schemaName) throws SQLException {
		String funcName = getFunctionName(tableName);
		if (!functionCollection.containsKey(funcName)) {	
			EnvelopeFunction envelopeFunction = new EnvelopeFunction(tableName, funcName, schemaName);
			functionCollection.put(funcName, envelopeFunction); 
			constructEnvelopeFunction(envelopeFunction);
			LOG.info("Envelope-function '" + funcName + "' created.");
		}			
	}
	
	protected SpatialCollection getSpatialCollection(String tableName) {
		SpatialCollection spatialCollection = new SpatialCollection();
		for (AbstractType<?> obj: schemaMapping.getAbstractTypes()) {
			if (tableName.equalsIgnoreCase(obj.getTable())) 
				spatialCollection.append(getSpatialCollection(obj));
		}
		return spatialCollection;
	}
	
	protected List<String> getSubTables(String superTable) {
		List<String> subTables = new ArrayList<String>();
		for (AbstractType<?> obj: schemaMapping.getAbstractTypes()) {
			AbstractExtension<?> extension = obj.getExtension();
			if (extension != null) {
				AbstractType<?> base = extension.getBase();
				if (base != null) {
					if (base.getTable().equalsIgnoreCase(superTable)) {
						String objTable = obj.getTable();
						if (!subTables.contains(objTable))
							subTables.add(objTable);					
					}
				}
			}
		}		
		return subTables;
	}

	private SpatialCollection getSpatialCollection(AbstractType<?> obj) {
		SpatialCollection spatialCollection = new SpatialCollection();
		List<AbstractProperty> properties = obj.getProperties();
		for (AbstractProperty property: properties) {
			if (property instanceof GeometryProperty || property instanceof ImplicitGeometryProperty) {
				spatialCollection.addSpatialProperty(property);
			}
			else if (property instanceof ComplexProperty) {
				spatialCollection.addSpatialObjectProperties(property);
			}
			else if (property instanceof ObjectProperty || property instanceof FeatureProperty) {
				AbstractType<?> childObj = ((AbstractRefTypeProperty<?>) property).getType();
				String childTable = childObj.getTable();
/*				RelationType tableRelation = checkTableRelationType(childTable, obj.getTable());
				if (tableRelation == RelationType.composition || tableRelation == RelationType.aggregation) {
					spatialCollection.addSpatialObjectProperties(property);
				}*/ 					
			}
		}
		
		return spatialCollection;
	}
	
	protected class SpatialCollection {
		private List<AbstractProperty> spatialProperties = new ArrayList<AbstractProperty>();
		private List<AbstractProperty> spatialObjectProperties = new ArrayList<AbstractProperty>();
		private List<AbstractProperty> superSpatialObject = new ArrayList<AbstractProperty>();
		private List<AbstractProperty> childSpatialObjects = new ArrayList<AbstractProperty>();
		private List<AbstractProperty> spatialHookProperties = new ArrayList<AbstractProperty>();

		public List<AbstractProperty> getSpatialProperties() {
			return spatialProperties;
		}

		public void addSpatialProperty(AbstractProperty property) {
			this.spatialProperties.add(property);
		}

		public List<AbstractProperty> getSpatialObjectProperties() {
			return spatialObjectProperties;
		}

		public void addSpatialObjectProperties(AbstractProperty property) {
			this.spatialObjectProperties.add(property);
		}

		public List<AbstractProperty> getSuperSpatialObject() {
			return superSpatialObject;
		}

		public void setSuperSpatialObject(List<AbstractProperty> superSpatialObject) {
			this.superSpatialObject = superSpatialObject;
		}

		public List<AbstractProperty> getChildSpatialObjects() {
			return childSpatialObjects;
		}

		public void setChildSpatialObjects(List<AbstractProperty> childSpatialObjects) {
			this.childSpatialObjects = childSpatialObjects;
		}

		public List<AbstractProperty> getSpatialHookProperties() {
			return spatialHookProperties;
		}

		public void setSpatialHookProperties(List<AbstractProperty> spatialHookProperties) {
			this.spatialHookProperties = spatialHookProperties;
		}
		
		public SpatialCollection append(SpatialCollection collection) {
			spatialProperties.addAll(collection.getSpatialProperties());
			spatialObjectProperties.addAll(collection.getSpatialObjectProperties());
			superSpatialObject.addAll(collection.getSuperSpatialObject());
			childSpatialObjects.addAll(collection.getChildSpatialObjects());
			spatialHookProperties.addAll(collection.getSpatialHookProperties());			
			return this;
		}
		
		public boolean isEmpty() {
			if (!spatialProperties.isEmpty() 
					|| !spatialObjectProperties.isEmpty()
					|| !superSpatialObject.isEmpty()
					|| !childSpatialObjects.isEmpty()
					|| !spatialHookProperties.isEmpty()) {
				return false;
			}
			return true;
		}
	}
	
}
