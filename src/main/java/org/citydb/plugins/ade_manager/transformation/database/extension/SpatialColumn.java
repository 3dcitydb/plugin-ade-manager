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
package org.citydb.plugins.ade_manager.transformation.database.extension;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.platform.oracle.Oracle10Platform;
import org.apache.ddlutils.platform.postgresql.PostgreSqlPlatform;
import org.citydb.plugins.ade_manager.transformation.database.DBScriptGenerator;

@SuppressWarnings("serial")
public class SpatialColumn extends IndexedColumn {
	private DBScriptGenerator databaseDDLCreator;
	
	public SpatialColumn(DBScriptGenerator databaseDDLCreator) {		
		super();
		this.databaseDDLCreator = databaseDDLCreator;
	}
	
	@Override
	public int getTypeCode(){
		return Integer.MIN_VALUE;
	}
	
	@Override
	public String getType(){
		String columnTypeName = null;
		
		Platform databasePlatform = databaseDDLCreator.getDatabasePlatform();
		
		if (databasePlatform instanceof Oracle10Platform) {
			columnTypeName = "MDSYS.SDO_GEOMETRY";
		}
		else if (databasePlatform instanceof PostgreSqlPlatform) {
			columnTypeName = "geometry(GEOMETRYZ)";
		}
		
		return columnTypeName;
	}

}
