package org.citydb.plugins.ade_manager.registry.query.datatype;

public class ReferencedEntry {
	private String refTable;
	private String[] fkColumns;

	public ReferencedEntry(String refTable, String[] fkColumns) {
		this.refTable = refTable;
		this.fkColumns = fkColumns;
	}
	
	public String getRefTable() {
		return refTable;
	}

	public void setRefTable(String refTable) {
		this.refTable = refTable;
	}

	public String[] getFkColumns() {
		return fkColumns;
	}

	public void setFkColumns(String[] fkColumns) {
		this.fkColumns = fkColumns;
	}
}

