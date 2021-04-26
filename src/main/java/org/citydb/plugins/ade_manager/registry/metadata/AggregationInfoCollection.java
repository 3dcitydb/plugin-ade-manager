/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.citydb.database.schema.mapping.RelationType;

public class AggregationInfoCollection {
	private List<AggregationInfo> aggrInfos; 
	private final ADEMetadataManager metadataManager;
	
	public AggregationInfoCollection(ADEMetadataManager metadataManager) {
		this.aggrInfos = new ArrayList<AggregationInfo>();;
		this.metadataManager = metadataManager;
	}

	public void addAggregationInfo(AggregationInfo aggrInfo) {
		aggrInfos.add(aggrInfo);
	}
	
	public RelationType getTableRelationType(String childTable, String parentTable, String joinTableOrColumn) {
		List<Integer> childClassIds = new ArrayList<Integer>();
		List<Integer> parentClassIds = new ArrayList<Integer>();
		try {
			childClassIds = metadataManager.getObjectClassIdsByTable(childTable);
			parentClassIds = metadataManager.getObjectClassIdsByTable(parentTable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		for (int childClassId: childClassIds) {
			for (int parentClassId: parentClassIds) {
				AggregationInfo aggrInfo = get(childClassId, parentClassId, joinTableOrColumn);
				if (aggrInfo != null) {
					if (aggrInfo.isComposite())
						return RelationType.COMPOSITION;
					else
						return RelationType.AGGREGATION;			
				}			
			}
		}
		return RelationType.ASSOCIATION;
	}

	
	public AggregationInfo get(int childClassId, int parentClassId, String joinTableOrColumn) {
		for (AggregationInfo aggrInfo: aggrInfos) {
			if (childClassId == aggrInfo.getChildClassId() && 
					parentClassId == aggrInfo.getParentClassId() &&
					joinTableOrColumn.equalsIgnoreCase(aggrInfo.getJoinTableOrColumnName())) {
				return aggrInfo;
			}
		}	
		return null;		
	}
}
