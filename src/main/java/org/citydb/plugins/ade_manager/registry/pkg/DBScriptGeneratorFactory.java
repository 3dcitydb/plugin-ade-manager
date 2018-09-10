/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * Copyright 2013 - 2018
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
package org.citydb.plugins.ade_manager.registry.pkg;

import java.sql.Connection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.pkg.delete.oracle.OracleDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.delete.postgis.PostgisDeleteGeneratorGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.oracle.OracleEnvelopeScriptGenerator;
import org.citydb.plugins.ade_manager.registry.pkg.envelope.postgis.PostgisEnvelopeGeneratorGenerator;

public class DBScriptGeneratorFactory {
	private static DBScriptGeneratorFactory instance;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	private DBScriptGeneratorFactory() {}

	public static synchronized DBScriptGeneratorFactory getInstance() {
		if (instance == null)
			instance = new DBScriptGeneratorFactory();
		
		return instance;
	}
	
	public DBScriptGenerator createDeleteScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleDeleteScriptGenerator(connection, config, adeMetadataManager);
		case POSTGIS:
			return new PostgisDeleteGeneratorGenerator(connection, config, adeMetadataManager);
		}		
		return null;
	}
	
	public DBScriptGenerator createEnvelopeScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleEnvelopeScriptGenerator(connection, config, adeMetadataManager);
		case POSTGIS:
			return new PostgisEnvelopeGeneratorGenerator(connection, config, adeMetadataManager);
		}		
		return null;
	}
}
