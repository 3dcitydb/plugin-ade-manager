package org.citydb.plugins.ade_manager.registry.pkg.envelope;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.citydb.database.schema.mapping.AbstractExtension;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.DefaultDBScriptGenerator;

public abstract class EnvelopeScriptGenerator extends DefaultDBScriptGenerator {
	protected SchemaMapping schemaMapping; 
	protected Map<String, List<AbstractProperty>> spatialTables;
	
	public EnvelopeScriptGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
		try {
			this.schemaMapping = adeMetadataManager.getADESchemaMappings();
		} catch (SQLException e) {
			LOG.error("Failed to initialize schema mapping: " + e.getMessage());
		}
		initSpatialTables();
	}

	protected DBSQLScript generateScript(String schemaName) throws SQLException {
		registerEnvelopeFunction("cityobject", schemaName);	
		
		return buildEnvelopeScript();	
	}
	
	protected String createFunctionName(String tableName) {
		return "envl_" + tableName;
	}
	
	protected abstract DBSQLScript buildEnvelopeScript() throws SQLException;
	protected abstract void constructEnvelopeFunction(EnvelopeFunction deleteFunction) throws SQLException;

	protected void registerEnvelopeFunction(String tableName, String schemaName) throws SQLException {
		String funcName = createFunctionName(tableName);
		if (!functionCollection.containsKey(funcName)) {	
			EnvelopeFunction envelopeFunction = new EnvelopeFunction(tableName, funcName, schemaName);
			functionCollection.put(funcName, envelopeFunction); 
			constructEnvelopeFunction(envelopeFunction);
			LOG.info("Envelope-function '" + funcName + "' created.");
		}			
	}
	
	protected void initSpatialTables() {
		spatialTables = new HashMap<>();
		for (AbstractType<?> obj: schemaMapping.getAbstractTypes()) {
			String tableName = obj.getTable();	
			List<AbstractProperty> properties = obj.getProperties();
			for (AbstractProperty property: properties) {
				if (property instanceof GeometryProperty || property instanceof ImplicitGeometryProperty) {
					if (!spatialTables.containsKey(tableName)) 
						spatialTables.put(tableName, new ArrayList<AbstractProperty>());
					spatialTables.get(tableName).add(property);
				}
			}
		}
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
	
}
