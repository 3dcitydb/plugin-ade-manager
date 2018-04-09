package org.citydb.plugins.ade_manager.transformation.database.schema;

import org.apache.ddlutils.model.Column;

@SuppressWarnings("serial")

public class TimestampColumn extends Column {
	
	public TimestampColumn() {
		super();
	}

	public String getType() {
		return "TIMESTAMP WITH TIME ZONE";
	}
}
