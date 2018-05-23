package org.citydb.plugins.ade_manager.registry.pkg.delete;

import org.citydb.plugins.ade_manager.registry.model.DBStoredFunction;

public class DeleteFunction extends DBStoredFunction {
	private String targetTable;

	public DeleteFunction(String name, String schema) {
		super(name, schema);
	}
	
	public DeleteFunction(String targetTable, String name, String schema) {
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
