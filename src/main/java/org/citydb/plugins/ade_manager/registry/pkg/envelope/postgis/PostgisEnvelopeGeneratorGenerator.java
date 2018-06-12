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
					brDent1 + "class_id INTEGER DEFAULT 0;" +
					brDent1 + var_return_bbox + " GEOMETRY;";
		
		List<String> bboxList = new ArrayList<String>();
		bboxList.add(var_return_bbox);
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
			local_geom_block += brDent1 + "SELECT " + wrapSchemaName(box2envelope_funcname, schemaName) + "(ST_3DExtent(geom)) INTO " + varBbox + " FROM (" +
									union_spatialProperties_envelope(tableName, schemaName, spatialProperties) +
								brDent1 + ") g;" + br;					
		}
		
		// aggregating spatial objects
		String aggr_geom_block = "";
		List<AbstractTypeProperty<?>> spatialObjectProperties = citydbSpatialTable.getSpatialRefTypeProperties();
		if (spatialObjectProperties.size() > 0) {
			String varBbox = "bbox" + index++;
			bboxList.add(varBbox);
			var_block += brDent1 + varBbox + " GEOMETRY;";
			aggr_geom_block += brDent1 + commentPrefix + "bbox from aggregating objects";
			aggr_geom_block += brDent1 + "SELECT " + wrapSchemaName(box2envelope_funcname, schemaName) + "(ST_3DExtent(geom)) INTO " + varBbox + " FROM (" +
									union_spatialRefTypeProperties_envelope(tableName, schemaName, spatialObjectProperties) +
								brDent1 + ") g;" + br;					
		}
		
		// get bbox from sub-tables	
		String subtables_geom_block = "";
		Map<Integer, String> subObjectclasses = citydbSpatialTable.getSubObjectclasses();
		List<String> directSubTables = citydbSpatialTable.getDirectSubTables();
		if (subObjectclasses.size() > 0) {
			String varBbox = "bbox" + index++;			
			var_block += brDent1 + varBbox + " GEOMETRY;";			
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
						 + brDent3 + commentPrefix + subTableName	
						 + brDent3 + "WHEN class_id = " + subObjectclassId + " THEN"
					 	 	+ brDent4 + varBbox + " := " + wrapSchemaName(getFunctionName(subTableName), schemaName) + "(co_id, set_envelope, " + caller + ");";			
			}

			if (subtables_geom_block.length() > 0) {
				bboxList.add(varBbox);
				subtables_geom_block = brDent1 + "IF caller <> 2 THEN"
											+ brDent2 + "SELECT objectclass_id INTO class_id FROM " + wrapSchemaName("cityobject", schemaName) + " WHERE id = co_id;"
											+ brDent2 + "CASE" 
											+ subtables_geom_block
											+ brDent2 + "ELSE"										
											+ brDent2 + "END CASE;"
									 + brDent1 + "END IF;" + br;
			}		
		}		
		
		// get bbox from hook table
		String hook_geom_block = "";
		List<String> hookTables = citydbSpatialTable.getHookTables();
		if (hookTables.size() > 0) {
			for (String hookTableName : hookTables) {
				// register function
				registerEnvelopeFunction(hookTableName, schemaName);
				
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
		union_geom_block += brDent1 + var_return_bbox + " := " + wrapSchemaName(box2envelope_funcname, schemaName) + "(ST_Union(ARRAY[";
		Iterator<String> iter = bboxList.iterator();
		while(iter.hasNext()) {
			String varBbox = iter.next();
			union_geom_block += varBbox;
			if (iter.hasNext())
				union_geom_block += ", ";
		}
		union_geom_block += "]));" + br;
		
		// update envelope column of CITYOBJECT table
		String update_block = "";
		if (!citydbSpatialTable.isHookTable()) {
			update_block += brDent1 + "IF set_envelope <> 0 AND " + var_return_bbox + " IS NOT NULL THEN" + 
								brDent2 + "UPDATE cityobject SET envelope = " + var_return_bbox + " WHERE id = co_id;" +
							brDent1 + "END IF;" + br;	
		}					
		
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
	
	private String union_spatialProperties_envelope(String tableName, String schemaName,
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
									 " FROM " + wrapSchemaName("surface_geometry", schemaName) + " sg, " + wrapSchemaName(tableName, schemaName) + " t" + 
									 " WHERE sg.root_id = t." + refColumn + 
									 " AND t.id = co_id" + 
									 " AND sg.geometry IS NOT NULL";						
				}
				if (inlineColumn != null) {
					if (refColumn != null)
						geom_block += brDent3 + "UNION ALL";
					
					geom_block += 
							brDent2 + commentPrefix + spatialProperty.getPath() +
							brDent2 + "SELECT " + inlineColumn + 
									 " AS geom FROM " + wrapSchemaName(tableName, schemaName) +  
									 " WHERE id = co_id " + 
									 " AND " + inlineColumn + " IS NOT NULL";	
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
						brDent2 + "SELECT " + wrapSchemaName(implicitGeomEnvelope_funcname, schemaName) + "(" + 
								   rep_id_column + ", " + 
								   ref_point_column + ", " + 
								   transformation_column + ") AS geom" + 
								   " FROM " + wrapSchemaName(tableName, schemaName) + 
								   " WHERE id = co_id" +
								   " AND " + rep_id_column + " IS NOT NULL";			
			}	
			if (iter.hasNext())
				geom_block += brDent3 + "UNION ALL";
		}
		
		return geom_block;
	}

	private String union_spatialRefTypeProperties_envelope(String tableName, String schemaName,
			List<AbstractTypeProperty<?>> spatialRefTypeProperties) throws SQLException {
		String geom_block = "";
		
		Iterator<AbstractTypeProperty<?>> iter = spatialRefTypeProperties.iterator();
		while (iter.hasNext()) {
			AbstractTypeProperty<?> spatialRefTypeProperty = iter.next();
			AbstractType<?> spatialRefType = spatialRefTypeProperty.getType();
			AbstractJoin propertyJoin = spatialRefTypeProperty.getJoin();					
			String refTable = spatialRefType.getTable();
			
			// register function
			registerEnvelopeFunction(refTable, schemaName);
			
			if (propertyJoin instanceof Join) {
				Join join = ((Join) propertyJoin);
				TableRole toRole = join.getToRole();
				if (toRole == TableRole.PARENT) {
					String fk_column = join.getFromColumn();
					geom_block += 
							brDent2 + commentPrefix + spatialRefType.getPath() +
							brDent2 + "SELECT " + wrapSchemaName(getFunctionName(refTable), schemaName) + "(c.id, set_envelope) AS geom " +
									  "FROM " + wrapSchemaName(tableName, schemaName) + " p, " + refTable + " c " +
									  "WHERE p.id = co_id " +
									  "AND p." + fk_column + " = " + "c.id";
				}
				else if (toRole == TableRole.CHILD) {
					String fk_column = join.getToColumn();
					geom_block += 
							brDent2 + commentPrefix + spatialRefType.getPath() +
							brDent2 + "SELECT " + wrapSchemaName(getFunctionName(refTable), schemaName) + "(id, set_envelope) AS geom" +
									  " FROM " + wrapSchemaName(refTable, schemaName) +
									  " WHERE " + fk_column + " = co_id";
				}
				else {/**/}
			}
			else if (propertyJoin instanceof JoinTable) {
				String joinTable = ((JoinTable) propertyJoin).getTable();
				String p_fk_column = ((JoinTable) propertyJoin).getJoin().getFromColumn();
				String c_fk_column = ((JoinTable) propertyJoin).getInverseJoin().getFromColumn();
				geom_block += 
						brDent2 + commentPrefix + spatialRefType.getPath() +
						brDent2 + "SELECT " + wrapSchemaName(getFunctionName(refTable), schemaName) + "(id, set_envelope) AS geom " +
								  "FROM " + wrapSchemaName(refTable, schemaName) + " c, " + wrapSchemaName(joinTable, schemaName) + " p2c " +
								  "WHERE c.id = " + c_fk_column + 
								  " AND p2c." + p_fk_column + " = co_id";
			} 
			else {/**/}
			
			if (iter.hasNext())
				geom_block += brDent3 + "UNION ALL";				
		}
		
		return geom_block;
	}

	@Override
	protected void constructBox2EnvelopeFunction(EnvelopeFunction box2envelopeFunction) {
		String schemaName = box2envelopeFunction.getOwnerSchema();
		
		String box2envelope_func_ddl = "";
		box2envelope_func_ddl += 
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(box2envelopeFunction.getName(), schemaName) + 
				"(box BOX3D) RETURNS GEOMETRY AS" + br + 
				"$body$" + br +
				"DECLARE" + 
				brDent1 + "envelope GEOMETRY;" + 
				brDent1 + "db_srid INTEGER;" + 
				br +
				"BEGIN" + 
				brDent1 + commentPrefix + "get reference system of input geometry" +  
				brDent1 + "IF ST_SRID(box) = 0 THEN"+	
					brDent2 + "SELECT srid INTO db_srid FROM " + wrapSchemaName("database_srs", schemaName) + ";" + 
				brDent1 + "ELSE" + 
					brDent2 + "db_srid := ST_SRID(box);" + 
				brDent1 + "END IF;" + 
				br + 
				brDent1 + "SELECT ST_SetSRID(ST_MakePolygon(ST_MakeLine(" + 
					brDent2 + "ARRAY[" + 
						brDent3 + "ST_MakePoint(ST_XMin(box), ST_YMin(box), ST_ZMin(box))," + 
						brDent3 + "ST_MakePoint(ST_XMax(box), ST_YMin(box), ST_ZMin(box))," + 
						brDent3 + "ST_MakePoint(ST_XMax(box), ST_YMax(box), ST_ZMax(box))," + 
						brDent3 + "ST_MakePoint(ST_XMin(box), ST_YMax(box), ST_ZMax(box))," + 						
						brDent3 + "ST_MakePoint(ST_XMin(box), ST_YMin(box), ST_ZMin(box))" +
					brDent2 + "]" + 
				brDent1 + ")), db_srid) INTO envelope;" +		
				br +				
				brDent1 + "RETURN envelope;" +				
				br +
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STABLE STRICT;";		

		box2envelopeFunction.setDefinition(box2envelope_func_ddl);
	}

	@Override
	protected void constructImplicitGeomEnvelopeFunction(EnvelopeFunction implicitGeomEnvelopeFunction) {
		String schemaName = implicitGeomEnvelopeFunction.getOwnerSchema();
		
		String implict_geom_func_ddl = "";
		implict_geom_func_ddl += 
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(implicitGeomEnvelopeFunction.getName(), schemaName) + 
				"(implicit_rep_id INTEGER, ref_pt GEOMETRY, transform4x4 VARCHAR) RETURNS GEOMETRY AS" + br + 
				"$body$" + br +
				"DECLARE" + 
				brDent1 + "envelope GEOMETRY;" + 
				brDent1 + "params DOUBLE PRECISION[ ] := '{}';" + 
				br +
				"BEGIN" + 
				brDent1 + commentPrefix + "calculate bounding box for implicit geometry" + br + 
				brDent1 + "SELECT box2envelope(ST_3DExtent(geom)) INTO envelope FROM ("+	
					brDent2 + commentPrefix + "relative other geometry" + br + 
					brDent2 + "SELECT relative_other_geom AS geom " +
						brDent3 + "FROM " + wrapSchemaName("implicit_geometry", schemaName) +
							brDent4 + "WHERE id = implicit_rep_id" +
								brDent5 + "AND relative_other_geom IS NOT NULL" +
					brDent2 + "UNION ALL" + 
					brDent2 + commentPrefix + "relative brep geometry" + 
					brDent2 + "SELECT sg.implicit_geometry AS geom" +
						brDent3 + "FROM " + wrapSchemaName("surface_geometry", schemaName) + " sg, " + wrapSchemaName("implicit_geometry", schemaName) + " ig" +
							brDent4 + "WHERE sg.root_id = ig.relative_brep_id " + 
								brDent5 + "AND ig.id = implicit_rep_id " + 
								brDent5 + "AND sg.implicit_geometry IS NOT NULL" + 
				brDent1 + ") g;" +
				br + 
				brDent1 + "IF transform4x4 IS NOT NULL THEN" + 
					brDent2 + commentPrefix + "-- extract parameters of transformation matrix" + 
					brDent2 + "params := string_to_array(transform4x4, ' ')::float8[];" + 
					br + 
					brDent2 + "IF array_length(params, 1) < 12 THEN" + 
						brDent3 + "RAISE EXCEPTION 'Malformed transformation matrix: %', transform4x4 USING HINT = '16 values are required';" + 
					brDent2 + "END IF; " + 
				brDent1 + "ELSE" + 						
					brDent2 + "params := '{" +
						brDent3 + "1, 0, 0, 0," + 
						brDent3 + "0, 1, 0, 0," + 
						brDent3 + "0, 0, 1, 0," + 
						brDent3 + "0, 0, 0, 1}';" + 
				brDent1 + "END IF;" +
				br + 
				brDent1 + "IF ref_pt IS NOT NULL THEN" +
					brDent2 + "params[4] := params[4] + ST_X(ref_pt);" + 
					brDent2 + "params[8] := params[8] + ST_Y(ref_pt);" + 
					brDent2 + "params[12] := params[12] + ST_Z(ref_pt);" + 
				brDent1 + "END IF;" + 
				br +				
				brDent1 + "IF envelope IS NOT NULL THEN" +
					brDent2 + commentPrefix + "perform affine transformation against given transformation matrix" +
					brDent2 + "envelope := ST_Affine(envelope," + 
						brDent3 + "params[1], params[2], params[3]," + 
						brDent3 + "params[5], params[6], params[7]," + 
						brDent3 + "params[9], params[10], params[11]," + 
						brDent3 + "params[4], params[8], params[12]);" + 
				brDent1 + "END IF;" + 
				br + 				
				brDent1 + "RETURN envelope;" +				
				br +
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STABLE;";		

		implicitGeomEnvelopeFunction.setDefinition(implict_geom_func_ddl);		
	}

	@Override
	protected void constructSetEnvelopeIfNullFunction(EnvelopeFunction setEnvelopeIfNullFunction) {
		String schemaName = setEnvelopeIfNullFunction.getOwnerSchema();
		
		String setEnvelope_func_ddl = "";
		setEnvelope_func_ddl += 
				"CREATE OR REPLACE FUNCTION " + wrapSchemaName(setEnvelopeIfNullFunction.getName(), schemaName) + 
				"(objclass_id INTEGER DEFAULT 0) RETURNS GEOMETRY AS" + br + 
				"$body$" + br +
				"DECLARE" + 
				brDent1 + "bbox GEOMETRY;" + 
				br +
				"BEGIN" + 
				brDent1 + "IF objclass_id <> 0 THEN" + 
					brDent2 + "SELECT " + wrapSchemaName("box2envelope", schemaName) + "(ST_3DExtent(geom)) INTO bbox FROM ("+	
						brDent3 + "SELECT " + wrapSchemaName(getFunctionName("cityobject"), schemaName) + "(id, 1) AS geom" + 
							brDent4 + "FROM " + wrapSchemaName("cityobject", schemaName) + " WHERE envelope IS NULL AND objectclass_id = objclass_id" + 
					brDent2 + ") g;" + 	
				brDent1 + "ELSE" + 
					brDent2 + "SELECT " + wrapSchemaName("box2envelope", schemaName) + "(ST_3DExtent(geom)) INTO bbox FROM ("+	
						brDent3 + "SELECT " + wrapSchemaName(getFunctionName("cityobject"), schemaName) + "(id, 1) AS geom" + 
							brDent4 + "FROM " + wrapSchemaName("cityobject", schemaName) + " WHERE envelope IS NULL" + 
					brDent2 + ") g;" + 					
				brDent1 + "END IF;" + 
				br +				
				brDent1 + "RETURN bbox;" +				
				br +
 				"END;" + br + 
				"$body$" + br + 
				"LANGUAGE plpgsql STRICT;";		

		setEnvelopeIfNullFunction.setDefinition(setEnvelope_func_ddl);
	}
}