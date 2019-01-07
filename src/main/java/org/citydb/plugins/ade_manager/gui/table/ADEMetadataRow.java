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
package org.citydb.plugins.ade_manager.gui.table;

import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;

public class ADEMetadataRow extends TableRowDefaultImpl {
	private static String[] columnNames;	
	private String adeid = "";
	private String name = "";
	private String description = "";
	private String version = "";
	private String dbPrefix = "";
	private String creationDate = "";

	public static String[] getColumnNames() {
		if (columnNames == null) {
			columnNames = new String[6];
			columnNames[0] = "ADEID";
			columnNames[1] = "Name";
			columnNames[2] = "Description";
			columnNames[3] = "Version";
			columnNames[4] = "DB_Prefix";
			columnNames[5] = "Creation_Date";
		}
		return columnNames;
	}
	
	public ADEMetadataRow(ADEMetadataInfo adeEntity) {
		this.adeid = adeEntity.getAdeid();
		this.name = adeEntity.getName();
		this.description = adeEntity.getDescription();
		this.version = adeEntity.getVersion();
		this.dbPrefix = adeEntity.getDbPrefix();
		this.creationDate = adeEntity.getCreationDate();
	}
	
	public String getValue(int col) {
		switch (col) {
		case 0:
			return adeid;
		case 1:
			return name;
		case 2:
			return description;
		case 3:
			return version;
		case 4:
			return dbPrefix;
		case 5:
			return creationDate;
		default:
			return "";
		}
	}

	public void setValue(int col, Object obj) {
		switch (col) {
		case 0:
			adeid = (String) obj;		
		case 1:
			name = (String) obj;
			return;
		case 2:
			description = (String) obj;
			return;
		case 3:
			version = (String) obj;
			return;
		case 4:
			dbPrefix = (String) obj;
			return;
		case 5:
			creationDate = (String) obj;
			return;
		}		
	}

}
