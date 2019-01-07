/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2019
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
package org.citydb.plugins.ade_manager.registry.schema.adapter.oracle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.AbstractADEDBSchemaManager;
import org.citydb.plugins.ade_manager.util.PathResolver;

public class OracleADEDBSchemaManager extends AbstractADEDBSchemaManager {

	public OracleADEDBSchemaManager(Connection connection, ConfigImpl config) {
		super(connection, config);
	}

	@Override
	protected String readCreateADEDBScript() throws IOException {
		String adeRegistryInputpath = config.getAdeRegistryInputPath();
		String createDBscriptPath = PathResolver.get_create_ade_db_filepath(adeRegistryInputpath, DatabaseType.ORACLE);	
		return new String(Files.readAllBytes(Paths.get(createDBscriptPath)));
	}

	@Override
	protected String processScript(String inputScript) throws SQLException {
		String result = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream writer = new PrintStream(os); 
		try {
			boolean skip = false;
			Scanner scanner = new Scanner(inputScript);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine(); 
				if (line.indexOf("SET SERVEROUTPUT ON") >= 0) {
					skip = true;
				}
				if (skip != true) {
					writer.println(line);
				}																
				if (line.indexOf("prompt Used SRID for spatial indexes") >= 0) {
					skip = false;
				}				
			}
			scanner.close();
			int srid = dbPool.getActiveDatabaseAdapter().getUtil().getDatabaseInfo(schema).getReferenceSystem().getSrid();
			result = os.toString().replace("&SRSNO", String.valueOf(srid));		
		} catch (SQLException e) {
			throw new SQLException("Failed to get SRID from the database", e);
		}	
		
		return result;
	}

	@Override
	protected void dropCurrentFunctions() throws SQLException {
		
	}

}
