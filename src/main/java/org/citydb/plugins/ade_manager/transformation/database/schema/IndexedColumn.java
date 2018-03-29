package org.citydb.plugins.ade_manager.transformation.database.schema;

import org.apache.ddlutils.model.Column;

@SuppressWarnings("serial")

public class IndexedColumn extends Column{
	
	private String indexName;
	
	public IndexedColumn() {
		super();
	}
	
	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
	
}
