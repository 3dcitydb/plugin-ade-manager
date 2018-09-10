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
package org.citydb.plugins.ade_manager.registry.schema;

import java.sql.Connection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.schema.adapter.oracle.OracleADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.adapter.postgis.PostgisADEDBSchemaManager;

public class ADEDBSchemaManagerFactory {
	private static ADEDBSchemaManagerFactory instance;
	private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();		
	
	private ADEDBSchemaManagerFactory() {}

	public static synchronized ADEDBSchemaManagerFactory getInstance() {
		if (instance == null)
			instance = new ADEDBSchemaManagerFactory();
		
		return instance;
	}
	
	public ADEDBSchemaManager createADEDatabaseSchemaManager(Connection connection, ConfigImpl config) {
		DatabaseType databaseType = dbPool.getActiveDatabaseAdapter().getDatabaseType();
		switch (databaseType) {
		case ORACLE:
			return new OracleADEDBSchemaManager(connection, config);
		case POSTGIS:
			return new PostgisADEDBSchemaManager(connection, config);
		}		
		return null;
	}
}
