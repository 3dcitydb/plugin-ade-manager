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
package org.citydb.plugins.ade_manager.registry.schema.adapter.postgis;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;
import org.citydb.plugins.ade_manager.util.PathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class PostgisADEDBSchemaManager extends AbstractADEDBSchemaManager {

	public PostgisADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	public void createADEDatabaseSchema() throws SQLException {
		super.createADEDatabaseSchema();

		// update SRID for geometry columns of cityGML core and ADE tables
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		int srid = dbPool.getActiveDatabaseAdapter().getUtil().getDatabaseInfo(schema).getReferenceSystem().getSrid();
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select f_table_schema, f_table_name, f_geometry_column from geometry_columns where f_table_schema=? " +
						"AND f_geometry_column <> 'implicit_geometry' " +
						"AND f_geometry_column <> 'relative_other_geom'" +
						"AND f_geometry_column <> 'texture_coordinates'")) {
			preparedStatement.setString(1, schema);
			ResultSet rs = preparedStatement.executeQuery();
			while (rs.next()) {
				try (CallableStatement callableStatement = connection.prepareCall("{call UpdateGeometrySRID(?, ?, ?, ?)}")) {
					callableStatement.setString(1, rs.getString((1)));
					callableStatement.setString(2, rs.getString((2)));
					callableStatement.setString(3, rs.getString((3)));
					callableStatement.setInt(4, srid);
					callableStatement.execute();
				}
			}
		}
	}

	@Override
	protected String readCreateADEDBScript() throws IOException {
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, DatabaseType.POSTGIS);	

		return new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
	}
	
	@Override
	protected String processScript(String inputScript) {
		return inputScript;
	}

	@Override
	protected void dropCurrentFunctions() throws SQLException {
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		Map<String, String> deleteFunctions = queryFunctions(schema);
		for (String funcName: deleteFunctions.keySet()) {
			String funcDeclaration = deleteFunctions.get(funcName);
			PreparedStatement pstsmt = null;
			try {
				pstsmt = connection.prepareStatement("DROP FUNCTION IF EXISTS " + schema + "." + funcDeclaration);
				pstsmt.executeUpdate();		
				LOG.debug("DB-function '" + funcName + "' successfully dropped");
			} 
			finally {			
				if (pstsmt != null) { 
					try {
						pstsmt.close();
					} catch (SQLException e) {
						throw e;
					} 
				}	
			}
		}		
	}
	
	private Map<String, String> queryFunctions(String schema) throws SQLException{
		Map<String, String> funcNames = new HashMap<String, String>();
		PreparedStatement pstsmt = null;
		ResultSet rs = null;
				
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT routines.routine_name, routines.data_type, parameters.data_type, parameters.ordinal_position ")
				      .append("FROM information_schema.routines ")
				      .append("LEFT JOIN information_schema.parameters ON routines.specific_name=parameters.specific_name ")
				      .append("WHERE routines.specific_schema= '").append(schema).append("' AND (")
				      .append("routines.routine_name like 'del_%' OR routines.routine_name like 'env_%') ")
				      .append("ORDER BY routines.routine_name, parameters.ordinal_position");
		try {
			pstsmt = connection.prepareStatement(strBuilder.toString());
			rs = pstsmt.executeQuery();						
			while (rs.next()) {
				String funcName = rs.getString(1);
				String instinctParamType = rs.getString(2);
				String paramType = rs.getString(3);
				if (paramType.equalsIgnoreCase("array"))
					paramType = instinctParamType + "[]";
				
				String funcDeclaration = null;
				if (!funcNames.containsKey(funcName)) {
					funcDeclaration = funcName + "(" + paramType;
				} else {
					funcDeclaration = funcNames.get(funcName) + "," + paramType;					
				}
				funcNames.put(funcName, funcDeclaration);	
			}	
			for (String key: funcNames.keySet()) {
				funcNames.put(key, funcNames.get(key) + ")");
			}
		} 
		finally {			
			if (rs != null) { 
				try {
					rs.close();
				} catch (SQLException e) {
					throw e;
				}
			}	
			if (pstsmt != null) { 
				try {
					pstsmt.close();
				} catch (SQLException e) {
					throw e;
				} 
			}	
		}
		
		return funcNames;
	}

}
