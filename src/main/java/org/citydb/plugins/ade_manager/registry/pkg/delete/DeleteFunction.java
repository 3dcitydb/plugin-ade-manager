package org.citydb.plugins.ade_manager.registry.pkg.delete;

import org.citydb.plugins.ade_manager.registry.model.DBStoredFunction;

public class DeleteFunction extends DBStoredFunction {
	private String targetTable;

	public DeleteFunction(String name, String schema) {
		super(name, schema);
	}
	
	public DeleteFunction(String targetTable, String name, String schema) {
		this(name, schema);
		this.targetTable = targetTable;
	}

	public DeleteFunction(String targetTable, String name, String declareField, String schema) {
		this(targetTable, name, schema);
		super.setDeclareField(declareField);
	}
	
	public String getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(String targetTable) {
		this.targetTable = targetTable;
	}

}
