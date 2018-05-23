package org.citydb.plugins.ade_manager.registry.pkg.envelope;

import org.citydb.plugins.ade_manager.registry.model.DBStoredFunction;

public class EnvelopeFunction extends DBStoredFunction {
	private String targetTable;

	public EnvelopeFunction(String name, String schema) {
		super(name, schema);
	}
	
	public EnvelopeFunction(String targetTable, String name, String schema) {
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
