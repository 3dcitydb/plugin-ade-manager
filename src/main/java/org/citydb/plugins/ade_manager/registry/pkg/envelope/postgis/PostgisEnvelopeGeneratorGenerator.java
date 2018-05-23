package org.citydb.plugins.ade_manager.registry.pkg.envelope.postgis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeScriptGenerator;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeFunction;

public class PostgisEnvelopeGeneratorGenerator extends EnvelopeScriptGenerator {

	public PostgisEnvelopeGeneratorGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	protected void constructEnvelopeFunction(EnvelopeFunction envelopeFunction) throws SQLException {
		String tableName = envelopeFunction.getTargetTable();
		String funcName = envelopeFunction.getName();
		String schemaName = envelopeFunction.getOwnerSchema();
		
		List<String> subTables = getSubTables(tableName);
		for (String subTable: subTables) 
			registerEnvelopeFunction(subTable, schemaName);
		
		String func_ddl =
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(funcName, schemaName) + 
				"(co_id INTEGER, set_envelope INTEGER DEFAULT 0) RETURNS GEOMETRY AS" + 
				br + 
				"$body$" + 
				br +
				"DECLARE" + 
				brDent1 + "bbox GEOMETRY;" + 
				br +
				"BEGIN";
				
		
		String geom_block = "";
		if (spatialTables.containsKey(tableName)) {
			geom_block += 
				brDent1 + "SELECT box2envelope(ST_3DExtent(geom)) INTO bbox FROM (";
			
			Iterator<AbstractProperty> iter = spatialTables.get(tableName).iterator();
			while (iter.hasNext()) {
				AbstractProperty spatialProperty = iter.next();
				if (spatialProperty instanceof GeometryProperty) {
					String refColumn = ((GeometryProperty) spatialProperty).getRefColumn();
					if (refColumn != null) {
						geom_block += 
								brDent2 + "SELECT sg.geometry AS geom FROM surface_geometry sg, " + tableName + " t " + 
											"WHERE sg.id = t." + refColumn + " AND t.id = co_id AND sg.geometry IS NOT NULL";
					if (iter.hasNext())
						geom_block +=
								brDent3 + "UNION ALL";
					}
				}
				// TODO
			}
		}
		
		//TODO
		
		String return_block = brDent1 + "RETURN bbox;";
		
		func_ddl += geom_block + br + 
				return_block + br +
				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";	
		
		envelopeFunction.setDefinition(func_ddl);
	}

	@Override
	protected DBSQLScript buildEnvelopeScript() throws SQLException {
		DBSQLScript dbScript = new DBSQLScript();
		dbScript.addSQLBlock(functionCollection.printFunctionDefinitions(separatorLine));		
		return dbScript;
	}

}
