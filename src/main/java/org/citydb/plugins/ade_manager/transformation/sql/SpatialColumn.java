package org.citydb.plugins.ade_manager.transformation.sql;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.platform.oracle.Oracle10Platform;
import org.apache.ddlutils.platform.postgresql.PostgreSqlPlatform;

@SuppressWarnings("serial")
public class SpatialColumn extends IndexedColumn {
	private DBScriptGenerator databaseDDLCreator;
	
	public SpatialColumn(DBScriptGenerator databaseDDLCreator) {		
		super();
		this.databaseDDLCreator = databaseDDLCreator;
	}
	
	@Override
	public int getTypeCode(){
		return Integer.MIN_VALUE;
	}
	
	@Override
	public String getType(){
		String columnTypeName = null;
		
		Platform databasePlatform = databaseDDLCreator.getDatabasePlatform();
		
		if (databasePlatform instanceof Oracle10Platform) {
			columnTypeName = "MDSYS.SDO_GEOMETRY";
		}
		else if (databasePlatform instanceof PostgreSqlPlatform) {
			columnTypeName = "geometry(GEOMETRYZ)";
		}
		
		return columnTypeName;
	}

}
