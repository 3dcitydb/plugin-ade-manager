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
package org.citydb.plugins.ade_manager.gui.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class TableModel<T extends TableRowDefaultImpl> extends AbstractTableModel {
	private final String[] columnNames;
	private List<T> rows = new ArrayList<T>();

	public TableModel(String[] columnNames) {
		this.columnNames = columnNames;
		updateColumnsTitle();
	}

	public void updateColumnsTitle() {	
		fireTableStructureChanged();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public int getRowCount() {
		return rows.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public Object getValueAt(int row, int col) {
		return rows.get(row).getValue(col);
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public void setValueAt(Object value, int row, int col) {
		rows.get(row).setValue(col, value);
	}

	public void addNewRow(T data) {
		data.setRowNumber(rows.size());
		rows.add(data);
		fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
	}

	public void editRow(T data) {
		rows.set(data.getRowNumber(), data);
		fireTableRowsUpdated(data.getRowNumber(), data.getRowNumber());
	}

	public void removeRow(int[] index) {
		if (index == null)
			return;

		Arrays.sort(index);
		for (int j = index.length - 1; j >= 0; j--) {
			if (index[j] < rows.size()) {
				rows.remove(index[j]);
				for (int i = index[j]; i < rows.size(); i++)
					rows.get(i).setRowNumber(i);
				fireTableRowsDeleted(index[j], index[j]);
			}
		}
	}

	public void move(int index, boolean moveUp) {
		if (index < 0 || index >= rows.size())
			return;
		if (moveUp && index == 0)
			return;
		if (!moveUp && index == rows.size() - 1)
			return;
		int tmp = rows.get(index).getRowNumber();
		rows.get(index).setRowNumber(rows.get(moveUp ? (index - 1) : (index + 1)).getRowNumber());
		rows.get(moveUp ? (index - 1) : (index + 1)).setRowNumber(tmp);
		Collections.sort(rows);
		fireTableDataChanged();
	}

	public TableRow getColumn(int index) {
		if (index < 0 || index >= rows.size())
			return null;
		return rows.get(index);
	}

	public boolean isSeparatorPhraseSuitable(String phrase) {
		return false;
	}

	public List<T> getRows() {
		return rows;
	}

	public void reset() {
		int size = rows.size();
		rows = new ArrayList<T>();
		if (size != 0)
			fireTableRowsDeleted(0, size - 1);
	}

}
