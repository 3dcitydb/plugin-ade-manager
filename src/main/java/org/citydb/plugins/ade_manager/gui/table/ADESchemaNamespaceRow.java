/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2026
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

public class ADESchemaNamespaceRow extends TableRowDefaultImpl {
	private static String[] columnNames;
	private String namespace = "";

	public static String[] getColumnNames() {
		if (columnNames == null) {
			columnNames = new String[1];
			columnNames[0] = "Namespace";
		}
		return columnNames;
	}
	
	public ADESchemaNamespaceRow(String namespace) {
		this.namespace = namespace;
	}

	public String getValue(int col) {
		switch (col) {
		case 0:
			return namespace;
		default:
			return "";
		}
	}

	public void setValues(String namespace) {
		this.namespace = namespace;
	}

	public void setValue(int col, Object obj) {
		switch (col) {
		case 0:
			namespace = (String) obj;
			return;
		}
	}
}
