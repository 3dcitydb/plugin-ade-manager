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

public class AggregationInfo {	
	private int childClassId;
	private int parentClassId;
	private int minOccurs;
	private Integer maxOccurs;
	private boolean isComposite;
	private String joinTableOrColumnName;
	
	public AggregationInfo(
			int childClassId, 
			int parentClassId, 
			int minOccurs, 
			Integer maxOccurs, 
			boolean isComposite,
			String joinTableOrColumnName) {
		
		this.setChildClassId(childClassId);
		this.setParentClassId(parentClassId);
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
		this.isComposite = isComposite;
		this.joinTableOrColumnName = joinTableOrColumnName;
	}

	public int getChildClassId() {
		return childClassId;
	}

	public void setChildClassId(int childClassId) {
		this.childClassId = childClassId;
	}

	public int getParentClassId() {
		return parentClassId;
	}

	public void setParentClassId(int parentClassId) {
		this.parentClassId = parentClassId;
	}

	public int getMinOccurs() {
		return minOccurs;
	}

	public void setMinOccurs(int minOccurs) {
		this.minOccurs = minOccurs;
	}

	public Integer getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(Integer maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	public boolean isComposite() {
		return isComposite;
	}

	public void setComposite(boolean isComposite) {
		this.isComposite = isComposite;
	}

	public String getJoinTableOrColumnName() {
		return joinTableOrColumnName;
	}

	public void setJoinTableOrColumnName(String joinTableOrColumnName) {
		this.joinTableOrColumnName = joinTableOrColumnName;
	}

}
