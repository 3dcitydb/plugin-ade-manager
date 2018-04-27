package org.citydb.plugins.ade_manager.registry.query.datatype;

public class ReferencingEntry {
	private String refColumn;
	private String refTable;
	
	public ReferencingEntry(String refTable, String refColumn) {
		this.refTable = refTable;
		this.refColumn = refColumn;
	}
	
	public String getRefColumn() {
		return refColumn;
	}

	public void setRefColumn(String refColumn) {
		this.refColumn = refColumn;
	}

	public String getRefTable() {
		return refTable;
	}

	public void setRefTable(String refTable) {
		this.refTable = refTable;
	}		
}