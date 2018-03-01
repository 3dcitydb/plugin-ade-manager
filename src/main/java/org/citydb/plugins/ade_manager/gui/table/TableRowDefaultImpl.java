
package org.citydb.plugins.ade_manager.gui.table;

import org.citydb.plugins.ade_manager.gui.table.TableRow;

public abstract class TableRowDefaultImpl implements TableRow, Comparable<TableRowDefaultImpl> {
	private int rownum;

	@Override
	public int compareTo(TableRowDefaultImpl o) {
		if (!(o instanceof TableRowDefaultImpl))
			return 0;
		if (((TableRowDefaultImpl) o).rownum > this.rownum)
			return -1;
		if (((TableRowDefaultImpl) o).rownum < this.rownum)
			return 1;
		return 0;
	}

	@Override
	public int getRowNumber() {
		return rownum;
	}

	@Override
	public void setRowNumber(int rownum) {
		this.rownum = rownum;		
	}

}
