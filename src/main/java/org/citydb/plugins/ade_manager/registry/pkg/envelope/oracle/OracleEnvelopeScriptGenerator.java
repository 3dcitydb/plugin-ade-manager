/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2020
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
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
package org.citydb.plugins.ade_manager.registry.pkg.envelope.oracle;

import java.sql.Connection;
import java.sql.SQLException;
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

public class OracleEnvelopeScriptGenerator extends EnvelopeScriptGenerator {
	
	public OracleEnvelopeScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		super(connection, config, adeMetadataManager);
	}

	@Override
	protected DBSQLScript buildEnvelopeScript() throws SQLException {
		DBSQLScript dbScript = new DBSQLScript();
		
		// package header
		String packageHeader = 
					"CREATE OR REPLACE PACKAGE citydb_envelope" + br +
					"AS" + br +
					functionCollection.printFunctionDeclareFields(dent) +
					"END citydb_envelope;" + br +
					"/";		
		dbScript.addSQLBlock(packageHeader);
		
		// package body	
		String packageBody =
					"CREATE OR REPLACE PACKAGE BODY citydb_envelope" + br +
					"AS" + 	
					functionCollection.printFunctionDefinitions(dent + separatorLine) + 
					"END citydb_envelope;" + br + 
					"/";		
		dbScript.addSQLBlock(packageBody);
		
		return dbScript;
	}

	@Override
	protected void constructEnvelopeFunction(EnvelopeFunction envelopeFunction) throws SQLException {
		String tableName = envelopeFunction.getTargetTable();
		String funcName = envelopeFunction.getName();
		String schemaName = envelopeFunction.getOwnerSchema();		
		
		String declareField = "FUNCTION " + funcName + "(co_id NUMBER, set_envelope int := 0, caller int := 0) RETURN SDO_GEOMETRY";
		envelopeFunction.setDeclareField(declareField);
		
		String envelope_func_ddl =
				dent + declareField + 
				brDent1 + "IS";
		
		String declare_block = 
				brDent2 + "bbox SDO_GEOMETRY;" +
				brDent2 + "class_id NUMBER;" +
				brDent2 + "dummy_box SDO_GEOMETRY;" +			
				brDent2 + "nested_feat_cur sys_refcursor;" +
				brDent2 + "nested_feat_id NUMBER;";
	
		CitydbSpatialTable citydbSpatialTable = getCitydbSpatialTable(tableName);
		
		// spatial properties from super table
		String super_geom_block = "";
		String superTableName = citydbSpatialTable.getSuperTable();
		if (superTableName != null) {
			super_geom_block += brDent2 + commentPrefix + "bbox from parent table";
			super_geom_block += brDent2 + "IF caller <> 1 THEN" + 
									brDent3 + "bbox := " + getFunctionName(superTableName) +"(co_id, set_envelope, 2);" + 
								brDent2 + "END IF;" + br;
		}	
		
		// local geometry properties
		String local_geom_block = "";
		List<AbstractProperty> spatialProperties = citydbSpatialTable.getSpatialProperties();
		if (spatialProperties.size() > 0) {
			local_geom_block += brDent2 + commentPrefix + "bbox from inline and referencing spatial columns";
			local_geom_block += brDent2 + "WITH collect_geom AS (" +
									union_spatialProperties_envelope(tableName, schemaName, spatialProperties) +
								brDent2 + ")" +
								brDent2 + "SELECT" + 
									brDent3 + "box2envelope(SDO_AGGR_MBR(geom))" + 
								brDent2 + "INTO" + 
									brDent3 + "bbox" + 
								brDent2 + "FROM" + 
									brDent3 + "collect_geom;" + br;
		}
		
		// aggregating spatial objects
		String aggr_geom_block = "";
		List<AbstractTypeProperty<?>> spatialObjectProperties = citydbSpatialTable.getSpatialRefTypeProperties();
		if (spatialObjectProperties.size() > 0) {
			aggr_geom_block += brDent2 + commentPrefix + "bbox from aggregating objects";
			aggr_geom_block += 
					union_spatialRefTypeProperties_envelope(tableName, schemaName, spatialObjectProperties);					
		}
		
		// get bbox from sub-tables	
		String subtables_geom_block = "";
		Map<Integer, String> subObjectclasses = citydbSpatialTable.getSubObjectclasses();
		List<String> directSubTables = citydbSpatialTable.getDirectSubTables();
		if (subObjectclasses.size() > 0) {	
			for (Entry<Integer, String> entry: subObjectclasses.entrySet()) {
				int subObjectclassId = entry.getKey();
				String subTableName = entry.getValue();
				if (tableName.equalsIgnoreCase(subTableName) || !tableExists(subTableName, schemaName))
					continue;
				
				// register envelop function
				registerEnvelopeFunction(subTableName, schemaName);
				
				int caller = 0;
				if (directSubTables.contains(subTableName))
					caller = 1;
				
				subtables_geom_block +=
						  brDent3 + commentPrefix + subTableName +	
						  brDent3 + "WHEN class_id = " + subObjectclassId + " THEN" +
					 	 	 brDent4 + "bbox := update_bounds(bbox, " + getFunctionName(subTableName)  + "(co_id, set_envelope, " + caller + "));";			
			}

			if (subtables_geom_block.length() > 0) {
				subtables_geom_block = brDent2 + "IF caller <> 2 THEN" + 
											brDent3 + "SELECT objectclass_id INTO class_id FROM " + tableName + " WHERE id = co_id;" + 
											brDent3 + "CASE" + 
											subtables_geom_block +
											brDent3 + "ELSE bbox := bbox;" + 
											brDent3 + "END CASE;" + 
									   brDent2 + "END IF;" + br;
			}		
		}
		
		// get bbox from hook table
		String hook_geom_block = "";
		List<String> hookTables = citydbSpatialTable.getHookTables();
		if (hookTables.size() > 0) {
			for (String hookTableName : hookTables) {
				// register function
				registerEnvelopeFunction(hookTableName, schemaName);

				hook_geom_block += brDent2 + commentPrefix + "bbox from hook table '" + hookTableName + "'";
				hook_geom_block += brDent2 + "bbox := update_bounds(bbox, " + getFunctionName(hookTableName) + "(co_id, set_envelope));" + br;
			}
		}		
		
		// update envelope column of CITYOBJECT table
		String update_block = "";
		if (!citydbSpatialTable.isHookTable() && tableName.equalsIgnoreCase("cityobject")) {
			update_block += brDent2 + "IF set_envelope <> 0 THEN" + 
								brDent3 + "UPDATE cityobject SET envelope = bbox WHERE id = co_id;" +
							brDent2 + "END IF;" + br;	
		}					
		
		// return block
		String return_block = 
							brDent2 + "RETURN bbox;" + br;
		
		// exception block
		String exception_block = 
							brDent2 + "EXCEPTION" + 
									brDent3 + "WHEN NO_DATA_FOUND THEN" + 
										brDent4 + "RETURN bbox;" + br;

		// Putting all together
		envelope_func_ddl += 
					declare_block + 
					brDent1 + "BEGIN" + 
					super_geom_block + 
					local_geom_block +
					aggr_geom_block +
					subtables_geom_block +
					hook_geom_block +
					update_block + 
					return_block +
					exception_block +
					brDent1 + "END;";	

		envelopeFunction.setDefinition(envelope_func_ddl);				
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
							brDent3 + commentPrefix + spatialProperty.getPath() +
						    brDent3 + "SELECT sg.geometry AS geom" + 
									 " FROM surface_geometry sg, " + tableName + " t" + 
									 " WHERE sg.root_id = t." + refColumn + 
									 " AND t.id = co_id" + 
									 " AND sg.geometry IS NOT NULL";						
				}
				if (inlineColumn != null) {
					if (refColumn != null)
						geom_block += brDent3 + "UNION ALL";
					
					geom_block += 
							brDent3 + commentPrefix + spatialProperty.getPath() +
							brDent3 + "SELECT " + inlineColumn + 
									 " AS geom FROM " + tableName +  
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
						brDent3 + commentPrefix + spatialProperty.getPath() +
						brDent3 + "SELECT " + implicitGeomEnvelope_funcname+ "(" + 
								   rep_id_column + ", " + 
								   ref_point_column + ", " + 
								   transformation_column + ") AS geom" + 
								   " FROM " + tableName + 
								   " WHERE id = co_id" +
								   " AND " + rep_id_column + " IS NOT NULL";			
			}	
			if (iter.hasNext())
				geom_block += brDent4 + "UNION ALL";
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
			
			geom_block += brDent2 + "OPEN nested_feat_cur FOR" +
							brDent3 + commentPrefix + spatialRefType.getPath();
			if (propertyJoin instanceof Join) {
				Join join = ((Join) propertyJoin);
				TableRole toRole = join.getToRole();
				if (toRole == TableRole.PARENT) {
					String fk_column = join.getFromColumn();
					geom_block += 						
							brDent3 + "SELECT c.id " +
									  "FROM " + tableName + " p, " + refTable + " c " +
									  "WHERE p.id = co_id " +
									  "AND p." + fk_column + " = " + "c.id;";
				}
				else if (toRole == TableRole.CHILD) {
					String fk_column = join.getToColumn();
					geom_block += 
							brDent3 + "SELECT id"  +
									  " FROM " + refTable +
									  " WHERE " + fk_column + " = co_id;";
				}
				else {/**/}
			}
			else if (propertyJoin instanceof JoinTable) {
				String joinTable = ((JoinTable) propertyJoin).getTable();
				String p_fk_column = ((JoinTable) propertyJoin).getJoin().getFromColumn();
				String c_fk_column = ((JoinTable) propertyJoin).getInverseJoin().getFromColumn();
				geom_block += 
						brDent3 + "SELECT c.id " +
								  "FROM " + refTable + " c, " + joinTable + " p2c " +
								  "WHERE c.id = " + c_fk_column + 
								  " AND p2c." + p_fk_column + " = co_id;";
			} 
			else {/**/}
			
			geom_block += brDent2 + "LOOP" +
						  	brDent3 + "FETCH nested_feat_cur INTO nested_feat_id;" +
						  	brDent3 + "EXIT WHEN nested_feat_cur%notfound;" +
						  	brDent3 + "bbox := update_bounds(bbox, " + getFunctionName(refTable) + "(nested_feat_id, set_envelope));" +
						  brDent2 + "END LOOP;" + 
						  brDent2 + "CLOSE nested_feat_cur;" + br;
		}
		
		return geom_block;
	}
	
	protected void constructUpdateBoundsFunction(EnvelopeFunction updateBoundsFunction) {
		String funcName = updateBoundsFunction.getName();
		
		String declareField = "FUNCTION " + funcName + "(old_box SDO_GEOMETRY, new_box SDO_GEOMETRY) RETURN SDO_GEOMETRY";		
		updateBoundsFunction.setDeclareField(declareField);
		
		String envelope_func_ddl =
				dent + declareField + 
				brDent1 + "IS" +
					brDent2 + "updated_box SDO_GEOMETRY;" + 
					brDent2 + "db_srid NUMBER;" + 
				brDent1 + "BEGIN" + 
					brDent2 + "IF old_box IS NULL AND new_box IS NULL THEN" + 
						brDent3 + "RETURN NULL;" +
					brDent2 + "ELSE" +
						brDent3 + "IF old_box IS NULL THEN" +
							brDent4 + "RETURN new_box;" + 
						brDent3 + "END IF;" + 
						br + 
						brDent3 + "IF new_box IS NULL THEN" +
							brDent4 + "RETURN old_box;" + 
						brDent3 + "END IF;" + 
						br + 
						brDent3 + commentPrefix + "get reference system of input geometry" +
						brDent3 + "IF old_box.sdo_srid IS NULL THEN" + 
							brDent4 + "SELECT srid INTO db_srid FROM database_srs;" +
						brDent3 + "ELSE" + 
							brDent4 + "db_srid := old_box.sdo_srid;" +
						brDent3 + "END IF;" +
						br + 
						brDent3 + "updated_box := MDSYS.SDO_GEOMETRY(3003,db_srid,NULL,MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,1),MDSYS.SDO_ORDINATE_ARRAY(" +
							brDent4 + commentPrefix + "first point" +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(1) < new_box.sdo_ordinates(1) THEN old_box.sdo_ordinates(1) ELSE new_box.sdo_ordinates(1) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(2) < new_box.sdo_ordinates(2) THEN old_box.sdo_ordinates(2) ELSE new_box.sdo_ordinates(2) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(3) < new_box.sdo_ordinates(3) THEN old_box.sdo_ordinates(3) ELSE new_box.sdo_ordinates(3) END," +
							brDent4 + commentPrefix + "second point" +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(4) > new_box.sdo_ordinates(4) THEN old_box.sdo_ordinates(4) ELSE new_box.sdo_ordinates(4) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(5) < new_box.sdo_ordinates(5) THEN old_box.sdo_ordinates(5) ELSE new_box.sdo_ordinates(5) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(6) < new_box.sdo_ordinates(6) THEN old_box.sdo_ordinates(6) ELSE new_box.sdo_ordinates(6) END," +
							brDent4 + commentPrefix + "third point" +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(7) > new_box.sdo_ordinates(7) THEN old_box.sdo_ordinates(7) ELSE new_box.sdo_ordinates(7) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(8) > new_box.sdo_ordinates(8) THEN old_box.sdo_ordinates(8) ELSE new_box.sdo_ordinates(8) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(9) > new_box.sdo_ordinates(9) THEN old_box.sdo_ordinates(9) ELSE new_box.sdo_ordinates(9) END," +
							brDent4 + commentPrefix + "forth point" +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(10) < new_box.sdo_ordinates(10) THEN old_box.sdo_ordinates(10) ELSE new_box.sdo_ordinates(10) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(11) > new_box.sdo_ordinates(11) THEN old_box.sdo_ordinates(11) ELSE new_box.sdo_ordinates(11) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(12) > new_box.sdo_ordinates(12) THEN old_box.sdo_ordinates(12) ELSE new_box.sdo_ordinates(12) END," +
							brDent4 + commentPrefix + "fifth point" +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(13) < new_box.sdo_ordinates(13) THEN old_box.sdo_ordinates(13) ELSE new_box.sdo_ordinates(13) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(14) < new_box.sdo_ordinates(14) THEN old_box.sdo_ordinates(14) ELSE new_box.sdo_ordinates(14) END," +
							brDent4 + "CASE WHEN old_box.sdo_ordinates(15) < new_box.sdo_ordinates(15) THEN old_box.sdo_ordinates(15) ELSE new_box.sdo_ordinates(15) END" +
							brDent4 + "));" +
					brDent2 + "END IF;" +
					br + 				
					brDent2 + "RETURN updated_box;" + 
					br +
				brDent1 + "END;";
		
		updateBoundsFunction.setDefinition(envelope_func_ddl);
	}

	@Override
	protected void constructBox2EnvelopeFunction(EnvelopeFunction box2envelopeFunction) {
		String funcName = box2envelopeFunction.getName();
		
		String declareField = "FUNCTION " + funcName + "(box SDO_GEOMETRY) RETURN SDO_GEOMETRY";		
		box2envelopeFunction.setDeclareField(declareField);
		
		String envelope_func_ddl =
				dent + declareField + 
				brDent1 + "IS" +
					brDent2 + "bbox SDO_GEOMETRY;" + 
					brDent2 + "db_srid NUMBER;" + 
				brDent1 + "BEGIN" + 
					brDent2 + "IF box IS NULL THEN" + 
						brDent3 + "RETURN NULL;" +
					brDent2 + "ELSE" + 
						brDent3 + commentPrefix + "get reference system of input geometry" +
						brDent3 + "IF box.sdo_srid IS NULL THEN" +
							brDent4 + "SELECT" +
								brDent5 + "srid" +
							brDent4 + "INTO" +
								brDent5 + "db_srid" +
							brDent4 + "FROM" +
								brDent5 + "database_srs;" +
						brDent3 + "ELSE" +
							brDent4 + "db_srid := box.sdo_srid;" +
						brDent3 + "END IF;" +
						br +
						brDent3 + "bbox := MDSYS.SDO_GEOMETRY(" +
								brDent6 + "3003," +
								brDent6 + "db_srid," + 
								brDent6 + "NULL," + 
								brDent6 + "MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,1)," + 
								brDent6 + "MDSYS.SDO_ORDINATE_ARRAY(" + 
									brDent7 + "SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,1),SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,2),SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,3)," +
									brDent7 + "SDO_GEOM.SDO_MAX_MBR_ORDINATE(box,1),SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,2),SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,3)," + 
									brDent7 + "SDO_GEOM.SDO_MAX_MBR_ORDINATE(box,1),SDO_GEOM.SDO_MAX_MBR_ORDINATE(box,2),SDO_GEOM.SDO_MAX_MBR_ORDINATE(box,3)," +
									brDent7 + "SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,1),SDO_GEOM.SDO_MAX_MBR_ORDINATE(box,2),SDO_GEOM.SDO_MAX_MBR_ORDINATE(box,3)," +
									brDent7 + "SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,1),SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,2),SDO_GEOM.SDO_MIN_MBR_ORDINATE(box,3)" + 
								brDent6 + ")" +
							brDent5 + ");" +
					brDent2 + "END IF;" +
					br +					
					brDent2 + "RETURN bbox;" + 
					br +
				brDent1 + "END;";
		
		box2envelopeFunction.setDefinition(envelope_func_ddl);	
	}

	@Override
	protected void constructImplicitGeomEnvelopeFunction(EnvelopeFunction implicitGeomEnvelopeFunction) {
		String funcName = implicitGeomEnvelopeFunction.getName();
		
		String declareField = "FUNCTION " + funcName + "(implicit_rep_id NUMBER, ref_pt SDO_GEOMETRY, transform4x4 VARCHAR2) RETURN SDO_GEOMETRY";		
		implicitGeomEnvelopeFunction.setDeclareField(declareField);
		
		String envelope_func_ddl =
				dent + declareField + 
				brDent1 + "IS" +
					brDent2 + "bbox SDO_GEOMETRY;" + 
					brDent2 + "params ID_ARRAY;" + 
					brDent2 + "matrix_ex EXCEPTION;" + 
				brDent1 + "BEGIN" + 
					brDent2 + commentPrefix + "calculate bounding box for implicit geometry" +
					brDent2 + "WITH collect_geom AS (" + 
						brDent3 + commentPrefix + "relative other geometry" +
						brDent3 + "SELECT" + 
							brDent4 + "relative_other_geom AS geom" +
						brDent3 + "FROM" +
							brDent4 + "implicit_geometry" + 
						brDent3 + "WHERE" + 
							brDent4 + "id = implicit_rep_id" +
							brDent4 + "AND relative_other_geom IS NOT NULL" +
						brDent3 + "UNION ALL" +
						brDent3 + commentPrefix + "relative brep geometry" +
						brDent3 + "SELECT" +
							brDent4 + "sg.implicit_geometry AS geom" +
						brDent3 + "FROM" +
							brDent4 + "surface_geometry sg," + 
							brDent4 + "implicit_geometry ig " +
						brDent3 + "WHERE" +
							brDent4 + "sg.root_id = ig.relative_brep_id" +
							brDent4 + "AND ig.id = implicit_rep_id" + 
							brDent4 + "AND sg.implicit_geometry IS NOT NULL" +
					brDent2 + ")" +
					brDent2 + "SELECT" +
						brDent3 + "box2envelope(SDO_AGGR_MBR(geom))" +
					brDent2 + "INTO" + 
						brDent3 + "bbox" +
					brDent2 + "FROM" +
						brDent3 + "collect_geom;" +
					br + 
					brDent2 + "IF transform4x4 IS NOT NULL THEN" +
						brDent3 + commentPrefix + "extract parameters of transformation matrix" +
						brDent3 + "params := citydb_util.string2id_array(transform4x4, ' ');" + 
						br +
						brDent3 + "IF params.count < 12 THEN" +
							brDent4 + "RAISE matrix_ex;" + 
						brDent3 + "END IF;" +
					brDent2 + "ELSE" +
						brDent3 + "params := ID_ARRAY(" + 
							brDent4 + "1, 0, 0, 0," + 
							brDent4 + "0, 1, 0, 0," +
							brDent4 + "0, 0, 1, 0," +
							brDent4 + "0, 0, 0, 1);" +
					brDent2 + "END IF;" +
					br +
					brDent2 + "IF ref_pt IS NOT NULL THEN" +
						brDent3 + "params(4) := params(4) + ref_pt.sdo_point.x;" +
						brDent3 + "params(8) := params(8) + ref_pt.sdo_point.y;" +
						brDent3 + "params(12) := params(12) + ref_pt.sdo_point.z;" + 
					brDent2 + "END IF;" +
					br +
					brDent2 + "IF bbox IS NOT NULL THEN" +
						brDent3 + "bbox := citydb_util.st_affine(" +
							brDent4 + "bbox," + 
							brDent4 + "params(1), params(2), params(3)," + 
							brDent4 + "params(5), params(6), params(7)," + 
							brDent4 + "params(9), params(10), params(11)," + 
							brDent4 + "params(4), params(8), params(12));" +
					brDent2 + "END IF;" + 
					br +
					brDent2 + "RETURN bbox;" + 
					br +
				brDent1 + "END;";
		
		implicitGeomEnvelopeFunction.setDefinition(envelope_func_ddl);	
	}

	@Override
	protected void constructCityobjectsEnvelopeFunction(EnvelopeFunction cityobjectsEnvelopeFunction) {
		String funcName = cityobjectsEnvelopeFunction.getName();
		
		String declareField = "FUNCTION " + funcName + "(objclass_id NUMBER := 0, set_envelope int := 0, only_if_null int := 1) RETURN SDO_GEOMETRY";		
		cityobjectsEnvelopeFunction.setDeclareField(declareField);
		
		String envelope_func_ddl =
				dent + declareField + 
				brDent1 + "IS" +
					brDent2 + "bbox SDO_GEOMETRY;" + 
					brDent2 + "filter VARCHAR2(150);" + 
					brDent2 + "cityobject_cur sys_refcursor;" + 
					brDent2 + "cityobject_rec cityobject%rowtype;" + 
				brDent1 + "BEGIN" + 
					brDent2 + "IF only_if_null <> 0 THEN" + 
						brDent3 + "filter := ' WHERE envelope IS NULL';" +
					brDent2 + "END IF;" + 
					br +
					brDent2 + "IF objclass_id <> 0 THEN" +	
						brDent3 + "filter := CASE WHEN filter IS NULL THEN ' WHERE ' ELSE filter || ' AND ' END;" +
						brDent3 + "filter := filter || 'objectclass_id = ' || to_char(objclass_id);" +
					brDent2 + "END IF;" +
					br +
					brDent2 + "OPEN cityobject_cur FOR" + 
						brDent3 + "'SELECT * FROM cityobject' || filter;" +
					brDent2 + "LOOP" +
						brDent3 + "FETCH cityobject_cur INTO cityobject_rec;" + 
						brDent3 + "EXIT WHEN cityobject_cur%notfound;" +
						brDent3 + "bbox := update_bounds(bbox, " + getFunctionName("cityobject") + "(cityobject_rec.id, set_envelope));" +
					brDent2 + "END LOOP;" + 
					brDent2 + "CLOSE cityobject_cur;" +
					br +		
					brDent2 + "RETURN bbox;" + 
					br +
				brDent1 + "END;";
		
		cityobjectsEnvelopeFunction.setDefinition(envelope_func_ddl);			
	}

}
