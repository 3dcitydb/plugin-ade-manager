package org.citydb.plugins.ade_manager.registry.pkg.delete;

import java.sql.SQLException;

public interface DeleteScriptGenerator {
	public String generateDeleteScript() throws SQLException;
	public void installDeleteScript(String scriptString) throws SQLException;
}
