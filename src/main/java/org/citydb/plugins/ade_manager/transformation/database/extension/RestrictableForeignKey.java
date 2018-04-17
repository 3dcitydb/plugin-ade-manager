package org.citydb.plugins.ade_manager.transformation.database.extension;

import org.apache.ddlutils.model.ForeignKey;

public class RestrictableForeignKey extends ForeignKey {
	private String ondelete;
	
	public RestrictableForeignKey () {
		super();
	}

	public String getOndelete() {
		return ondelete;
	}

	public void setOndelete(String ondelete) {
		this.ondelete = ondelete;
	}
}
