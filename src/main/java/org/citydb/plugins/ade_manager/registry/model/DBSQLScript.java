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
package org.citydb.plugins.ade_manager.registry.model;

import java.util.ArrayList;
import java.util.List;

public class DBSQLScript {
	private String headerText = "";
	private List<String> sqlBlocks;
	
	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}
	
	public List<String> getSQLBlocks() {
		if (sqlBlocks == null)
			sqlBlocks = new ArrayList<String>();	
		
		return sqlBlocks;
	}
	
	public void setSqlBlocks(List<String> sqlBlocks) {
		this.sqlBlocks = sqlBlocks;
	}
	
	public void addSQLBlock(String sqlBlock) {
		this.getSQLBlocks().add(sqlBlock);
	}
	
	@Override
	public String toString() {
		String br = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		builder.append(headerText);
		
		for (String sqlBlock: getSQLBlocks()) {
			builder.append(br).append("------------------------------------------").append(br)
				.append(sqlBlock).append(br);
		}		
		return builder.toString();
	}

}
