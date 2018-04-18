package org.citydb.plugins.ade_manager.registry.datatype;

public class ReferencedEntry {
	private String refTable;
	private String refColumn;
	private String[] fkColumns;

	public ReferencedEntry(String refTable, String refColumn, String[] fkColumns) {
		this.refTable = refTable;
		this.refColumn = refColumn;
		this.fkColumns = fkColumns;
	}
	
	public String getRefTable() {
		return refTable;
	}

	public void setRefTable(String refTable) {
		this.refTable = refTable;
	}

	public String getRefColumn() {
		return refColumn;
	}

	public void setRefColumn(String refColumn) {
		this.refColumn = refColumn;
	}

	public String[] getFkColumns() {
		return fkColumns;
	}

	public void setFkColumns(String[] fkColumns) {
		this.fkColumns = fkColumns;
	}
}

