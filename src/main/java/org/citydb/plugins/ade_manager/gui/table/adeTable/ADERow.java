
package org.citydb.plugins.ade_manager.gui.table.adeTable;

import org.citydb.plugins.ade_manager.gui.table.TableRowDefaultImpl;

public class ADERow extends TableRowDefaultImpl {

	private static String[] columnNames;	
	private String adeid = "";
	private String name = "";
	private String description = "";
	private String version = "";
	private String dbPrefix = "";

	public static String[] getColumnNames() {
		if (columnNames == null) {
			columnNames = new String[5];
			columnNames[0] = "ADEID";
			columnNames[1] = "Name";
			columnNames[2] = "Description";
			columnNames[3] = "Namespace";
			columnNames[4] = "DB_Prefix";
		}
		return columnNames;
	}
	
	public ADERow(String adeid, String name, String description, String version, String dbPrefix) {
		this.adeid = adeid;
		this.name = name;
		this.description = description;
		this.version = version;
		this.dbPrefix = dbPrefix;
	}
	
	public String getValue(int col) {
		switch (col) {
		case 0:
			return adeid;
		case 1:
			return name;
		case 2:
			return description;
		case 3:
			return version;
		case 4:
			return dbPrefix;			
		default:
			return "";
		}
	}

	public void setValues(String adeid, String name, String description, String version, String dbPrefix) {
		this.adeid = adeid;
		this.name = name;
		this.description = description;
		this.version = version;
		this.dbPrefix = dbPrefix;
	}

	public void setValue(int col, Object obj) {
		switch (col) {
		case 0:
			adeid = (String) obj;		
		case 1:
			name = (String) obj;
			return;
		case 2:
			description = (String) obj;
			return;
		case 3:
			version = (String) obj;
			return;
		case 4:
			dbPrefix = (String) obj;
			return;
		}
	}

}
