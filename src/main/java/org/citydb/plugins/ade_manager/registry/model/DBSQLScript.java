package org.citydb.plugins.ade_manager.registry.model;

import java.util.ArrayList;
import java.util.List;

public class DBSQLScript {
	private String headerText = "";
	private List<String> sqlBlocks;
	
	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}
	
	public List<String> getSQLBlocks() {
		if (sqlBlocks == null)
			sqlBlocks = new ArrayList<String>();	
		
		return sqlBlocks;
	}
	
	public void setSqlBlocks(List<String> sqlBlocks) {
		this.sqlBlocks = sqlBlocks;
	}
	
	public void addSQLBlock(String sqlBlock) {
		this.getSQLBlocks().add(sqlBlock);
	}
	
	@Override
	public String toString() {
		String br = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		builder.append(headerText);
		
		for (String sqlBlock: getSQLBlocks()) {
			builder.append(br).append("------------------------------------------").append(br)
				.append(sqlBlock).append(br);
		}		
		return builder.toString();
	}

}
