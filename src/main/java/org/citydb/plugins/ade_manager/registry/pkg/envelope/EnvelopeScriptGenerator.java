package org.citydb.plugins.ade_manager.registry.pkg.envelope;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.citydb.database.schema.mapping.AbstractExtension;
import org.citydb.database.schema.mapping.AbstractJoin;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.AbstractTypeProperty;
import org.citydb.database.schema.mapping.ComplexProperty;
import org.citydb.database.schema.mapping.FeatureProperty;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.Join;
import org.citydb.database.schema.mapping.JoinTable;
import org.citydb.database.schema.mapping.ObjectProperty;
import org.citydb.database.schema.mapping.PropertyInjection;
import org.citydb.database.schema.mapping.RelationType;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.database.schema.mapping.TableRole;
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
	
	protected CitydbSpatialTable getCitydbSpatialTable(String tableName) throws SQLException {	
		CitydbSpatialTable citydbSpatialTable = new CitydbSpatialTable(tableName);
		for (AbstractType<?> obj: schemaMapping.getAbstractTypes()) {
			if (tableName.equalsIgnoreCase(obj.getTable())) {				
				// get super table
				if (obj.getExtension() != null) {
					String superTable = obj.getExtension().getBase().getTable();
					if (!superTable.equalsIgnoreCase(tableName))
						citydbSpatialTable.setSuperTable(superTable);					
				}
				
				// get spatial properties
				List<AbstractProperty> properties = obj.getProperties();
				for (AbstractProperty property: properties) {
					if (property instanceof GeometryProperty || property instanceof ImplicitGeometryProperty) {
						citydbSpatialTable.addSpatialProperty(property);
					}
					else if (property instanceof ComplexProperty) {
						citydbSpatialTable.addSpatialObjectProperties(property);
					}
					else if (property instanceof ObjectProperty || property instanceof FeatureProperty) {
						AbstractType<?> childObj = ((AbstractTypeProperty<?>) property).getType();
						String childTable = childObj.getTable();
						String parentTable = obj.getTable();
						AbstractJoin joiner = property.getJoin();
						String join_table_or_column = null;
						if (joiner instanceof Join) {
							if (((Join) joiner).getToRole() == TableRole.CHILD)
								join_table_or_column = ((Join) joiner).getToColumn();
							else
								join_table_or_column = ((Join) joiner).getFromColumn();	
						}
						else if (joiner instanceof JoinTable)
							join_table_or_column = ((JoinTable) joiner).getTable();
							
						RelationType tableRelation = aggregationInfoCollection.getTableRelationType(childTable, parentTable, join_table_or_column);
						if (tableRelation == RelationType.COMPOSITION || tableRelation == RelationType.AGGREGATION) {
							citydbSpatialTable.addSpatialObjectProperties(property);
						} 					
					}
				}
				// get sub-tables
				citydbSpatialTable.setSubObjectclasses(adeMetadataManager.getSubObjectclassesFromSuperTable(tableName));
				citydbSpatialTable.setDirectSubTables(getSubTables(tableName));
			}				
		}
		
		for (PropertyInjection injection: schemaMapping.getPropertyInjections()) {
			String targetBaseTable = injection.getDefaultBase().getTable();	
			if (targetBaseTable.equalsIgnoreCase(tableName))
				citydbSpatialTable.addHookTable(injection.getTable());
		}
		
		return citydbSpatialTable;
	}
	
	private List<String> getSubTables(String superTable) {
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

	protected class CitydbSpatialTable {
		private String table;	
		private String superTable;		
		private List<String> hookTables = new ArrayList<String>();		
		private List<AbstractProperty> spatialProperties = new ArrayList<AbstractProperty>();
		private List<AbstractProperty> spatialObjectProperties = new ArrayList<AbstractProperty>();	
		private List<String> directSubTables = new ArrayList<String>();
		private Map<Integer, String> subObjectclasses = new HashMap<Integer, String>();

		public CitydbSpatialTable(String table) {
			this.table = table;
		}
		
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

		public String getTable() {
			return table;
		}

		public void setTable(String table) {
			this.table = table;
		}

		public String getSuperTable() {
			return superTable;
		}

		public void setSuperTable(String superTable) {
			this.superTable = superTable;
		}

		public List<String> getHookTables() {
			return hookTables;
		}

		public void addHookTable(String hookTable) {
			this.hookTables.add(hookTable);
		}

		public Map<Integer, String> getSubObjectclasses() {
			return subObjectclasses;
		}

		public void setSubObjectclasses(Map<Integer, String> subObjectclasses) {
			this.subObjectclasses = subObjectclasses;
		}

		public List<String> getDirectSubTables() {
			return directSubTables;
		}

		public void setDirectSubTables(List<String> directSubTables) {
			this.directSubTables = directSubTables;
		}		
	}
	
}
