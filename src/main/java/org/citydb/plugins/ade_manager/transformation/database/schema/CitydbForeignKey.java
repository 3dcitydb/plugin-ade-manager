package org.citydb.plugins.ade_manager.transformation.database.schema;

import org.apache.ddlutils.model.ForeignKey;

public class CitydbForeignKey extends ForeignKey{
	private String ondelete;
	
	public CitydbForeignKey () {
		super();
	}

	public String getOndelete() {
		return ondelete;
	}

	public void setOndelete(String ondelete) {
		this.ondelete = ondelete;
	}
}
