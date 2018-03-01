package org.citydb.plugins.ade_manager.script;

import java.io.File;

import org.citydb.database.connection.DatabaseConnectionPool;

public interface IDeleteScriptGenerator {
	public void doProcess(DatabaseConnectionPool dbPool, File outputFile) throws DsgException;
}
