
package org.citydb.plugins.ade_manager.gui.table;

import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataInfo;

public class ADEMetadataRow extends TableRowDefaultImpl {
	private static String[] columnNames;	
	private String adeid = "";
	private String name = "";
	private String description = "";
	private String version = "";
	private String dbPrefix = "";
	private String creationDate = "";

	public static String[] getColumnNames() {
		if (columnNames == null) {
			columnNames = new String[6];
			columnNames[0] = "ADEID";
			columnNames[1] = "Name";
			columnNames[2] = "Description";
			columnNames[3] = "Version";
			columnNames[4] = "DB_Prefix";
			columnNames[5] = "Creation_Date";
		}
		return columnNames;
	}
	
	public ADEMetadataRow(ADEMetadataInfo adeEntity) {
		this.adeid = adeEntity.getAdeid();
		this.name = adeEntity.getName();
		this.description = adeEntity.getDescription();
		this.version = adeEntity.getVersion();
		this.dbPrefix = adeEntity.getDbPrefix();
		this.creationDate = adeEntity.getCreationDate();
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
		case 5:
			return creationDate;
		default:
			return "";
		}
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
		case 5:
			creationDate = (String) obj;
			return;
		}		
	}

}
