package org.citydb.plugins.ade_manager.registry.metadata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.citydb.plugins.ade_manager.registry.query.datatype.RelationType;

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
						return RelationType.composition;
					else
						return RelationType.aggregation;			
				}			
			}
		}
		return RelationType.association;
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
