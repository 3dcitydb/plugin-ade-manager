package org.citydb.plugins.ade_manager.transformation.database.delete;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.transformation.TransformationController;

public interface IDeleteScriptGenerator {
	public void doProcess(TransformationController manager, DatabaseConnectionPool dbPool, ConfigImpl config) throws DsgException;
}
