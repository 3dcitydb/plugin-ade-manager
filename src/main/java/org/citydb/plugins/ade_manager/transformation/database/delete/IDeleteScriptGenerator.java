package org.citydb.plugins.ade_manager.transformation.database.delete;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.TransformationManager;

public interface IDeleteScriptGenerator {
	public void doProcess(TransformationManager manager, DatabaseConnectionPool dbPool, ConfigImpl config) throws DsgException;
}
