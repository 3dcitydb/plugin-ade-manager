package org.citydb.plugins.ade_manager.registry.pkg.delete;

import org.citydb.plugins.ade_manager.registry.pkg.model.DBStoredFunction;

public class DBDeleteFunction extends DBStoredFunction {
	private String targetTable;

	public DBDeleteFunction(String name, String schema) {
		super(name, schema);
	}
	
	public DBDeleteFunction(String targetTable, String name, String schema) {
		super(name, schema);
		this.targetTable = targetTable;
	}

	public String getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(String targetTable) {
		this.targetTable = targetTable;
	}

}
