
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
