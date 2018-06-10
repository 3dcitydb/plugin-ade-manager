package org.citydb.plugins.ade_manager.registry.pkg.envelope.postgis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.citydb.database.schema.mapping.AbstractJoin;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.AbstractTypeProperty;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.Join;
import org.citydb.database.schema.mapping.JoinTable;
import org.citydb.database.schema.mapping.TableRole;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeScriptGenerator;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeFunction;

public class PostgisEnvelopeGeneratorGenerator extends EnvelopeScriptGenerator {

	public PostgisEnvelopeGeneratorGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
	}

	@Override
	protected DBSQLScript buildEnvelopeScript() throws SQLException {
		DBSQLScript dbScript = new DBSQLScript();
		dbScript.addSQLBlock(functionCollection.printFunctionDefinitions(separatorLine));		
		return dbScript;
	}

	@Override
	protected void constructEnvelopeFunction(EnvelopeFunction envelopeFunction) throws SQLException {
		String tableName = envelopeFunction.getTargetTable();
		String funcName = envelopeFunction.getName();
		String schemaName = envelopeFunction.getOwnerSchema();

		// declaration block
		String declare_block =
					"CREATE OR REPLACE FUNCTION " + wrapSchemaName(funcName, schemaName) + 
					"(co_id INTEGER, set_envelope INTEGER DEFAULT 0, caller INTEGER DEFAULT 0) RETURNS GEOMETRY AS" + 
					br + 
					"$body$" + 
					br +
					"DECLARE";			
		int index = 0;
		String var_return_bbox = "bbox" + index++;
		String var_block =
					brDent1 + "objclass_id INTEGER DEFAULT 0," +
					brDent1 + var_return_bbox + " GEOMETRY;";
		
		List<String> bboxList = new ArrayList<String>();
		CitydbSpatialTable citydbSpatialTable = getCitydbSpatialTable(tableName);
		
		// spatial properties from super table
		String super_geom_block = "";
		String superTableName = citydbSpatialTable.getSuperTable();
		if (superTableName != null) {
			String varBbox = "bbox" + index++;
			bboxList.add(varBbox);
			var_block += brDent1 + varBbox + " GEOMETRY;";
			super_geom_block += brDent1 + commentPrefix + "bbox from parent table";
			super_geom_block += brDent1 + "IF caller <> 1 THEN" + 
									brDent2 + varBbox + " := " + wrapSchemaName(getFunctionName(superTableName), schemaName) +"(co_id, set_envelope, 2);" + 
								brDent1 + "END IF;" + br;
		}
		
		// local geometry properties
		String local_geom_block = "";
		List<AbstractProperty> spatialProperties = citydbSpatialTable.getSpatialProperties();
		if (spatialProperties.size() > 0) {
			String varBbox = "bbox" + index++;
			bboxList.add(varBbox);
			var_block += brDent1 + varBbox + " GEOMETRY;";
			local_geom_block += brDent1 + commentPrefix + "bbox from inline and referencing spatial columns";
			local_geom_block += brDent1 + "SELECT box2envelope(ST_3DExtent(geom)) INTO " + varBbox + " FROM (" +
									create_spatialProperties_block(tableName, schemaName, spatialProperties) +
								brDent1 + ") g;" + br;					
		}
		
		// aggregating spatial objects
		String aggr_geom_block = "";
		List<AbstractProperty> spatialObjectProperties = citydbSpatialTable.getSpatialObjectProperties();
		if (spatialObjectProperties.size() > 0) {
			String varBbox = "bbox" + index++;
			bboxList.add(varBbox);
			var_block += brDent1 + varBbox + " GEOMETRY;";
			aggr_geom_block += brDent1 + commentPrefix + "bbox from aggregating objects";
			aggr_geom_block += brDent1 + "SELECT box2envelope(ST_3DExtent(geom)) INTO " + varBbox + " FROM (" +
									create_spatialObjectProperties_block(tableName, schemaName, spatialObjectProperties) +
								brDent1 + ") g;" + br;					
		}
		
		// get bbox from sub-tables	
		String subtables_geom_block = "";
		Map<Integer, String> subObjectclasses = citydbSpatialTable.getSubObjectclasses();	
		if (subObjectclasses.size() > 0) {
			String varBbox = "bbox" + index++;
			bboxList.add(varBbox);
			var_block += brDent1 + varBbox + " GEOMETRY;";
			
			List<String> directSubTables = citydbSpatialTable.getDirectSubTables();
			subtables_geom_block += brDent1 + "IF caller <> 2 THEN"
										+ brDent2 + "SELECT objectclass_id INTO class_id FROM cityobject WHERE id = co_id;"
										+ brDent2 + "CASE";
			for (Entry<Integer, String> entry: subObjectclasses.entrySet()) {
				int subObjectclassId = entry.getKey();
				String subTableName = entry.getValue();
				if (tableName.equalsIgnoreCase(subTableName))
					continue;
				
				// register envelop function
				registerEnvelopeFunction(subTableName, schemaName);
				
				int caller = 0;
				if (directSubTables.contains(subTableName))
					caller = 1;
				
				subtables_geom_block += ""
						 + brDent3 + "-- " + subTableName						 
						 + brDent3 + "WHEN objectclass_id = " + subObjectclassId + " THEN"
					 	 	+ brDent4 + varBbox + " := " + wrapSchemaName(getFunctionName(subTableName), schemaName) + "(co_id, set_envelope, " + caller + ");";			
			}	
			subtables_geom_block += brDent2 + "END CASE;"
							+ brDent1 + "END IF;" + br;
		}	
		
		// get bbox from hook table
		String hook_geom_block = "";
		List<String> hookTables = citydbSpatialTable.getHookTables();
		if (hookTables.size() > 0) {
			for (String hookTableName : hookTables) {
				String varBbox = "bbox" + index++;
				bboxList.add(varBbox);
				var_block += brDent1 + varBbox + " GEOMETRY;";
				hook_geom_block += brDent1 + commentPrefix + "bbox from hook table '" + hookTableName + "'";
				hook_geom_block += brDent1 + varBbox + " := "+ wrapSchemaName(getFunctionName(hookTableName), schemaName) + "(co_id, set_envelope);" + br;
			}
		}
		
		// union all calculated bounding boxes
		var_block += br;
		String union_geom_block = "";
		union_geom_block += brDent1 + commentPrefix + "assemble all bboxes";
		union_geom_block += brDent1 + var_return_bbox + " := ST_Union(ARRAY[";
		Iterator<String> iter = bboxList.iterator();
		while(iter.hasNext()) {
			String varBbox = iter.next();
			union_geom_block += varBbox;
			if (iter.hasNext())
				union_geom_block += ", ";
		}
		union_geom_block += "]);" + br;
		
		// update envelope column of CITYOBJECT table
		String update_block = "";
		update_block += brDent1 + "IF set_envelope <> 0 AND bbox IS NOT NULL THEN" + 
							brDent2 + "UPDATE cityobject SET envelope = bbox WHERE id = co_id;" +
						brDent1 + "END IF;" + br;						
		
		String return_block = 
						brDent1 + "RETURN " + var_return_bbox + ";" + br;
									
		String func_ddl =
				declare_block +
				var_block +
				"BEGIN" +
				super_geom_block + 
				local_geom_block +
				aggr_geom_block +
				subtables_geom_block +
				hook_geom_block + 
				union_geom_block +
				update_block + 
				return_block +
				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";	
		
		envelopeFunction.setDefinition(func_ddl);
	}
	
	private String create_spatialProperties_block(String tableName, String schemaName,
			List<AbstractProperty> spatialProperties) throws SQLException {
		String geom_block = "";
		Iterator<AbstractProperty> iter = spatialProperties.iterator();
		while (iter.hasNext()) {
			AbstractProperty spatialProperty = iter.next();
			if (spatialProperty instanceof GeometryProperty) {
				String refColumn = ((GeometryProperty) spatialProperty).getRefColumn();
				String inlineColumn = ((GeometryProperty) spatialProperty).getInlineColumn();
				if (refColumn != null) {
					geom_block += 
							brDent2 + commentPrefix + spatialProperty.getPath() +
						    brDent2 + "SELECT sg.geometry AS geom" + 
									 " FROM surface_geometry sg, " + tableName + " t" + 
									 " WHERE sg.id = t." + refColumn + 
									 " AND t.id = co_id " + 
									 " AND sg.geometry IS NOT NULL";						
					if (iter.hasNext())
						geom_block += brDent3 + "UNION ALL";
				}
				if (inlineColumn != null) {
					geom_block += 
							brDent2 + commentPrefix + spatialProperty.getPath() +
							brDent2 + "SELECT " + inlineColumn + 
									 " AS geom FROM " + tableName +  
									 " WHERE id = co_id " + 
									 " AND " + inlineColumn + " IS NOT NULL";	
					if (iter.hasNext())
						geom_block += brDent3 + "UNION ALL";
				}
			}
			else if (spatialProperty instanceof ImplicitGeometryProperty) {
				int lod = ((ImplicitGeometryProperty) spatialProperty).getLod();
				String rep_id_column = 
									"lod" + lod + "_implicit_rep_id";
				String ref_point_column =
									"lod" + lod + "_implicit_ref_point";
				String transformation_column = 
									"lod" + lod + "_implicit_transformation";
				geom_block += 
						brDent2 + commentPrefix + spatialProperty.getPath() +
						brDent2 + "SELECT get_envelope_implicit_geometry(" + 
								   rep_id_column + ", " + 
								   ref_point_column + ", " + 
								   transformation_column + ") AS geom" + 
								   " FROM " + tableName + 
								   " WHERE id = co_id" +
								   " AND " + rep_id_column + " IS NOT NULL";
				
				if (iter.hasNext())
					geom_block += brDent3 + "UNION ALL";
			}		
		}
		
		return geom_block;
	}

	private String create_spatialObjectProperties_block(String tableName, String schemaName,
			List<AbstractProperty> spatialProperties) throws SQLException {
		String geom_block = "";
		
		Iterator<AbstractProperty> iter = spatialProperties.iterator();
		while (iter.hasNext()) {
			AbstractProperty spatialProperty = iter.next();
			AbstractType<?> obj = ((AbstractTypeProperty<?>) spatialProperty).getType();
			AbstractJoin propertyJoin = ((AbstractTypeProperty<?>) spatialProperty).getJoin();		
			String refTable = obj.getTable();
			
			// register a new envelope function (if not exists) for the child object
			if (!refTable.equalsIgnoreCase(tableName))
				registerEnvelopeFunction(refTable, schemaName);

			String ref_envelope_funcName = getFunctionName(refTable);							
			if (propertyJoin instanceof Join) {
				Join join = ((Join) propertyJoin);
				TableRole toRole = join.getToRole();
				if (toRole == TableRole.PARENT) {
					String fk_column = join.getFromColumn();
					geom_block += 
							brDent2 + commentPrefix + obj.getPath() +
							brDent2 + "SELECT " + ref_envelope_funcName + "(c.id, set_envelope) AS geom " +
									  "FROM " + tableName + " p, " + refTable + " c " +
									  "WHERE p.id = co_id " +
									  "AND p." + fk_column + " = " + "c.id";
				}
				else if (toRole == TableRole.CHILD) {
					String fk_column = join.getToColumn();
					geom_block += 
							brDent2 + commentPrefix + obj.getPath() +
							brDent2 + "SELECT " + ref_envelope_funcName + "(id, set_envelope) AS geom" +
									  " FROM " + refTable +
									  " WHERE " + fk_column + " = co_id";
				}

				else {/*throw exception*/}
			}
			else if (propertyJoin instanceof JoinTable) {
				String joinTable = ((JoinTable) propertyJoin).getTable();
				String p_fk_column = ((JoinTable) propertyJoin).getJoin().getFromColumn();
				String c_fk_column = ((JoinTable) propertyJoin).getInverseJoin().getFromColumn();
				geom_block += 
						brDent2 + commentPrefix + obj.getPath() +
						brDent2 + "SELECT " + ref_envelope_funcName + "(id, set_envelope) AS geom " +
								  "FROM " + refTable + " c, " + joinTable + " p2c " +
								  "WHERE c.id = " + c_fk_column + 
								  " AMD p2c." + p_fk_column + " = co_id";
			} 
			else {/*throw exception*/}
			
			if (iter.hasNext())
				geom_block += brDent3 + "UNION ALL";
				
		}
		
		return geom_block;
	}
}