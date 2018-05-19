package org.citydb.plugins.ade_manager.registry.pkg;

import java.sql.SQLException;

public interface DBScriptGenerator {
	public String generateScript() throws SQLException;
	public void installScript(String scriptString) throws SQLException;
}
