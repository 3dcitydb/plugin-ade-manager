package org.citydb.plugins.ade_manager.registry.pkg.envelope.postgis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.citydb.database.schema.mapping.AbstractJoin;
import org.citydb.database.schema.mapping.AbstractProperty;
import org.citydb.database.schema.mapping.AbstractType;
import org.citydb.database.schema.mapping.ComplexProperty;
import org.citydb.database.schema.mapping.Condition;
import org.citydb.database.schema.mapping.FeatureProperty;
import org.citydb.database.schema.mapping.GeometryProperty;
import org.citydb.database.schema.mapping.ImplicitGeometryProperty;
import org.citydb.database.schema.mapping.Join;
import org.citydb.database.schema.mapping.JoinTable;
import org.citydb.database.schema.mapping.ObjectProperty;
import org.citydb.database.schema.mapping.TableRole;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeScriptGenerator;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.EnvelopeFunction;

public class PostgisEnvelopeGeneratorGenerator extends EnvelopeScriptGenerator {

	public PostgisEnvelopeGeneratorGenerator(Connection connection, ConfigImpl config) {
		super(connection, config);
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
		
		List<String> subTables = getSubTables(tableName);
		SpatialCollection spatialCollection = getSpatialCollection(tableName);
		for (String subTable: subTables) {
			if (!getSpatialCollection(subTable).isEmpty())
				registerEnvelopeFunction(subTable, schemaName);
		}

		// declaration block
		String declare_block =
					"CREATE OR REPLACE FUNCTION " + wrapSchemaName(funcName, schemaName) + 
					"(co_id INTEGER, set_envelope INTEGER DEFAULT 0) RETURNS GEOMETRY AS" + 
					br + 
					"$body$" + 
					br +
					"DECLARE";			
		int index = 0;
		String var_return_bbox = "bbox" + index++;
		String var_block = brDent1 + var_return_bbox + " GEOMETRY;";
		List<String> bboxList = new ArrayList<String>();
		
		// local geometry properties
		String local_geom_block = "";
		List<AbstractProperty> spatialProperties = spatialCollection.getSpatialProperties();
		if (spatialProperties.size() > 0) {
			String varBbox = "bbox" + index++;
			bboxList.add(varBbox);
			var_block += brDent1 + varBbox + " GEOMETRY;";
			local_geom_block += brDent1 + "SELECT box2envelope(ST_3DExtent(geom)) INTO " + varBbox + " FROM (" +
									create_spatialProperties_block(tableName, schemaName, spatialProperties) +
								brDent1 + ") g;" + br;					
		}
		
		// aggregating spatial objects
		String aggr_geom_block = "";
		List<AbstractProperty> spatialObjectProperties = spatialCollection.getSpatialObjectProperties();
		if (spatialObjectProperties.size() > 0) {
			String varBbox = "bbox" + index++;
			bboxList.add(varBbox);
			var_block += brDent1 + varBbox + "  GEOMETRY;";
			aggr_geom_block += brDent1 + "SELECT box2envelope(ST_3DExtent(geom)) INTO " + varBbox + " FROM (" +
									create_spatialObjectProperties_block(tableName, schemaName, spatialObjectProperties) +
								brDent1 + ") g;" + br;					
		}
		
		var_block += br;
		String union_geom_block = brDent1 + "SELECT ST_Union(ARRAY[";
		Iterator<String> iter = bboxList.iterator();
		while(iter.hasNext()) {
			String varBbox = iter.next();
			union_geom_block += varBbox;
			if (iter.hasNext())
				union_geom_block += ", ";
		}
		union_geom_block += "]) INTO " + var_return_bbox + ";" + br;
		
		// update block
		String update_block = "";
		update_block += brDent1 + "IF set_envelope <> 0 AND bbox IS NOT NULL THEN" + 
							brDent2 + "UPDATE cityobject SET envelope = bbox WHERE id = co_id;" +
						brDent1 + "END IF;" + br;
						
		
		String return_block = 
						brDent1 + "RETURN " + var_return_bbox + ";" + br;
		
		String exception_block = 
						brDent1 + "EXCEPTION" + 
							brDent2 + "WHEN OTHERS THEN" + 
								brDent3 + "RETURN NULL;" + br;
									
		String func_ddl =
				declare_block +
				var_block +
				"BEGIN" +
				local_geom_block +
				aggr_geom_block +
				union_geom_block +
				update_block + 
				return_block +  
				exception_block + 
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
			AbstractType<?> obj = null;
			AbstractJoin propertyJoin = null;
			if (spatialProperty instanceof ComplexProperty) {
				obj = ((ComplexProperty) spatialProperty).getType();
				propertyJoin = ((ComplexProperty)spatialProperty).getJoin();
			} 				
			else if (spatialProperty instanceof ObjectProperty) {
				obj = ((ObjectProperty) spatialProperty).getType();
				propertyJoin = ((ObjectProperty)spatialProperty).getJoin();
			}				
			else if (spatialProperty instanceof FeatureProperty) {
				obj = ((FeatureProperty) spatialProperty).getType();
				propertyJoin = ((FeatureProperty)spatialProperty).getJoin();
			}					
			else {/*throw exception*/}

			String refTable = obj.getTable();
			if (!refTable.equalsIgnoreCase(tableName)) {
				registerEnvelopeFunction(refTable, schemaName);
			}

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
				List<Condition> conditions = join.getConditions();
				if (conditions.size() > 0) {
					String conditionColumn = conditions.get(0).getColumn();
					if (conditionColumn.equalsIgnoreCase("objectclass_id")) {
						geom_block += " AND objectclass_id = " + obj.getObjectClassId();
					}
				}
				
				else {/*throw exception*/}
			}
			else if (propertyJoin instanceof JoinTable) {
				// TODO
			} 
			else {/*throw exception*/}
			
			if (iter.hasNext())
				geom_block += brDent3 + "UNION ALL";
		
		
		}
		
		return geom_block;
	}
}