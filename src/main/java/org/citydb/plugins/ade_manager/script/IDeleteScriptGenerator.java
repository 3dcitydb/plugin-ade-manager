package org.citydb.plugins.ade_manager.script;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;

public interface IDeleteScriptGenerator {
	public void doProcess(DatabaseConnectionPool dbPool, ConfigImpl config) throws DsgException;
}
