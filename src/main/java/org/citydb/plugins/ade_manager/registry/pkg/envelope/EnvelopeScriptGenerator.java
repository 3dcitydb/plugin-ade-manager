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
import org.citydb.database.schema.mapping.AbstractRefTypeProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.AbstractTypeProperty;
import org.citydb.database.schema.mapping.ComplexProperty;
import org.citydb.database.schema.mapping.FeatureProperty;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.InjectedProperty;
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
	protected final String box2envelope_funcname = "box2envelope";
	protected final String implicitGeomEnvelope_funcname = "get_envelope_implicit_geometry";
	protected final String set_envelope_ifNull_funcname = "set_envelope_cityobjects_if_null";
	
	public EnvelopeScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
		this.schemaMapping = adeMetadataManager.getMergedSchemaMapping();
	}

	protected DBSQLScript generateScript(String schemaName) throws SQLException {
		registerEnvelopeFunction("cityobject", schemaName);	
		registerExtraFunctions(schemaName);
		
		return buildEnvelopeScript();	
	}
	
	protected String getFunctionName(String tableName) {
		return "env_" + tableName;
	}
	
	protected abstract DBSQLScript buildEnvelopeScript() throws SQLException;
	protected abstract void constructEnvelopeFunction(EnvelopeFunction deleteFunction) throws SQLException;
	protected abstract void constructBox2EnvelopeFunction(EnvelopeFunction box2envelopeFunction);
	protected abstract void constructImplicitGeomEnvelopeFunction(EnvelopeFunction implicitGeomEnvelopeFunction);
	protected abstract void constructSetEnvelopeIfNullFunction(EnvelopeFunction setEnvelopeIfNullFunction);
	
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
		for (PropertyInjection injection: schemaMapping.getPropertyInjections()) {
			String hookTable = injection.getTable();	
			if (hookTable.equalsIgnoreCase(tableName)) {
				citydbSpatialTable.setHookTable(true);
				fillCitydbSpatialTable(injection.getProperties(), citydbSpatialTable);
			}				
		}
		
		if (!citydbSpatialTable.isHookTable) {
			for (AbstractType<?> obj: schemaMapping.getAbstractTypes()) {
				if (tableName.equalsIgnoreCase(obj.getTable())) {				
					// get super table
					if (obj.getExtension() != null) {
						String superTable = obj.getExtension().getBase().getTable();
						if (!superTable.equalsIgnoreCase(tableName))
							citydbSpatialTable.setSuperTable(superTable);					
					}	
					
					// get sub-tables
					citydbSpatialTable.setSubObjectclasses(adeMetadataManager.getSubObjectclassesFromSuperTable(tableName));
					citydbSpatialTable.setDirectSubTables(getSubTables(tableName));		
					
					// get spatial properties
					List<?> properties = obj.getProperties();
					fillCitydbSpatialTable(properties, citydbSpatialTable);				
				}				
			}			
			for (PropertyInjection injection: schemaMapping.getPropertyInjections()) {
				String targetBaseTable = injection.getDefaultBase().getTable();	
				if (targetBaseTable.equalsIgnoreCase(tableName))
					citydbSpatialTable.addHookTable(injection.getTable());
			}
		}

		return citydbSpatialTable;
	}
	
	private void fillCitydbSpatialTable(List<?> properties, CitydbSpatialTable citydbSpatialTable) {
		for (Object property: properties) {
			if (!citydbSpatialTable.isHookTable && property instanceof InjectedProperty)
				continue;
			
			if (property instanceof GeometryProperty || property instanceof ImplicitGeometryProperty) {
				citydbSpatialTable.addSpatialProperty((AbstractProperty) property);
			}
			else if (property instanceof ComplexProperty) {
				AbstractJoin joiner = ((ComplexProperty)property).getJoin();
				if (joiner == null) {
					// added properties of inline Type 
					fillCitydbSpatialTable(((ComplexProperty) property).getType().getProperties(), citydbSpatialTable);
				}
				else {
					citydbSpatialTable.addSpatialRefTypeProperty((AbstractTypeProperty<?>) property);
				}
			}
			else if (property instanceof ObjectProperty || property instanceof FeatureProperty) {
				AbstractType<?> childObj = ((AbstractTypeProperty<?>) property).getType();
				String childTable = childObj.getTable();
				String parentTable = citydbSpatialTable.getTable();
				AbstractJoin joiner = ((AbstractRefTypeProperty<?>)property).getJoin();
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
					citydbSpatialTable.addSpatialRefTypeProperty((AbstractTypeProperty<?>) property);
				} 					
			}
		}
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
	
	private void registerExtraFunctions(String schemaName) {
		// box2envelope function
		EnvelopeFunction box2envelopeFunction = new EnvelopeFunction(box2envelope_funcname, schemaName);
		constructBox2EnvelopeFunction(box2envelopeFunction);;
		functionCollection.put(box2envelope_funcname, box2envelopeFunction);
		LOG.info("Function '" + box2envelope_funcname + "' created." );
		
		// implicit geometry envelope function
		EnvelopeFunction implicitGeomEnvelopeFunction = new EnvelopeFunction(implicitGeomEnvelope_funcname, schemaName);
		constructImplicitGeomEnvelopeFunction(implicitGeomEnvelopeFunction);
		functionCollection.put(implicitGeomEnvelope_funcname, implicitGeomEnvelopeFunction);
		LOG.info("Function '" + implicitGeomEnvelope_funcname + "' created." );
		
		// implicit geometry envelope function
		EnvelopeFunction setEnvelopeIfNullFunction = new EnvelopeFunction(set_envelope_ifNull_funcname, schemaName);
		constructSetEnvelopeIfNullFunction(setEnvelopeIfNullFunction);
		functionCollection.put(set_envelope_ifNull_funcname, setEnvelopeIfNullFunction);
		LOG.info("Function '" + set_envelope_ifNull_funcname + "' created." );
	}
	
	protected class CitydbSpatialTable {
		private boolean isHookTable = false;
		private String table;	
		private String superTable;	
		private List<String> hookTables = new ArrayList<String>();	
		private List<String> directSubTables = new ArrayList<String>();
		private List<AbstractProperty> spatialProperties = new ArrayList<AbstractProperty>();
		private List<AbstractTypeProperty<?>> spatialRefTypeProperties = new ArrayList<AbstractTypeProperty<?>>();			
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

		public List<AbstractTypeProperty<?>> getSpatialRefTypeProperties() {
			return spatialRefTypeProperties;
		}

		public void addSpatialRefTypeProperty(AbstractTypeProperty<?> property) {
			this.spatialRefTypeProperties.add(property);
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

		public boolean isHookTable() {
			return isHookTable;
		}

		public void setHookTable(boolean isHookTable) {
			this.isHookTable = isHookTable;
		}	
	}
	
}
