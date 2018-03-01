package org.citydb.plugins.ade_manager.gui.table;

public interface TableRow {
	public String getValue(int col);
	public void setValue(int col, Object obj);
	public int getRowNumber();
	public void setRowNumber(int rownum);
}
