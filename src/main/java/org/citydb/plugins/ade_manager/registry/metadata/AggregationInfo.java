
package org.citydb.plugins.ade_manager.registry.metadata;

public class AggregationInfo {	
	private String childTable;
	private String parentTable;
	private int minOccurs;
	private int maxOccurs;
	private boolean isComposite;

	public AggregationInfo(
			String childTable, 
			String parentTable, 
			int minOccurs, 
			int maxOccurs, 
			boolean isComposite) {
		
		this.childTable = childTable;
		this.parentTable = parentTable;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
		this.isComposite = isComposite;
	}

	public String getChildTable() {
		return childTable;
	}

	public void setChildTable(String childTable) {
		this.childTable = childTable;
	}

	public String getParentTable() {
		return parentTable;
	}

	public void setParentTable(String parentTable) {
		this.parentTable = parentTable;
	}

	public int getMinOccurs() {
		return minOccurs;
	}

	public void setMinOccurs(int minOccurs) {
		this.minOccurs = minOccurs;
	}

	public int getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(int maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	public boolean isComposite() {
		return isComposite;
	}

	public void setComposite(boolean isComposite) {
		this.isComposite = isComposite;
	}
	


}
