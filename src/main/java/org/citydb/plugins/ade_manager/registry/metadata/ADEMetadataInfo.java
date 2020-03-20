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
package org.citydb.plugins.ade_manager.registry.metadata;

public class ADEMetadataInfo {	
	private String adeid;
	private String name;
	private String description;
	private String version;
	private String dbPrefix;
	private String creationDate;

	public ADEMetadataInfo(
			String adeid, 
			String name, 
			String description, 
			String version, 
			String dbPrefix,
			String creationDate) {
		
		this.adeid = adeid;
		this.name = name;
		this.description = description;
		this.version = version;
		this.dbPrefix = dbPrefix;
		this.creationDate = creationDate;
	}
	
	public String getAdeid() {
		return adeid;
	}

	public void setAdeid(String adeid) {
		this.adeid = adeid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDbPrefix() {
		return dbPrefix;
	}

	public void setDbPrefix(String dbPrefix) {
		this.dbPrefix = dbPrefix;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

}
