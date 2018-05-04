package org.citydb.plugins.ade_manager.concurrent;

import java.sql.SQLException;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.Worker;
import org.citydb.concurrent.WorkerFactory;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.EventDispatcher;
import org.citydb.log.Logger;

public class DBDeleteWorkerFactory implements WorkerFactory<DBSplittingResult>{
	private final Logger LOG = Logger.getInstance();
	private final EventDispatcher eventDispatcher;

	public DBDeleteWorkerFactory(EventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}
	
	@Override
	public Worker<DBSplittingResult> createWorker() {
		DBDeleteWorker dbWorker = null;
		DatabaseType databaseType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();		
		
		try {
			if (databaseType == DatabaseType.ORACLE) {
				dbWorker = new DBOracleDeleteWorker(eventDispatcher);
			} 
			else if (databaseType == DatabaseType.POSTGIS) {
				dbWorker = new DBPostGISDeleteWorker(eventDispatcher);
			} 			
		} catch (SQLException e) {
			LOG.error("Failed to create delete worker: " + e.getMessage());
		} 

		return dbWorker;
	}
}
