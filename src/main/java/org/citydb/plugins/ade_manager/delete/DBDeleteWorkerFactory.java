package org.citydb.plugins.ade_manager.delete;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.Worker;
import org.citydb.concurrent.WorkerFactory;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.EventDispatcher;
import org.citydb.log.Logger;

public class DBDeleteWorkerFactory implements WorkerFactory<DBSplittingResult>{
	private final Logger LOG = Logger.getInstance();
	private final EventDispatcher eventDispatcher;
	private final Connection globalConnection;
	private final List<Connection> connections;

	public DBDeleteWorkerFactory(EventDispatcher eventDispatcher, List<Connection> connections) {
		this.eventDispatcher = eventDispatcher;
		this.connections = connections;
		
		if (connections.size() > 0)
			globalConnection = connections.get(0);
		else
			globalConnection = null;
	}
	
	@Override
	public Worker<DBSplittingResult> createWorker() {		
		DBDeleteWorker dbWorker = null;
		
		try {	
			if (this.globalConnection == null) {
				Connection connection = DatabaseConnectionPool.getInstance().getConnection();
				connection.setAutoCommit(false);
				this.connections.add(connection);
				dbWorker = new DBDeleteWorker(eventDispatcher, connection);
			}
			else
				dbWorker = new DBDeleteWorker(eventDispatcher, this.globalConnection);			
		} catch (SQLException e) {
			LOG.error("Failed to create delete worker: " + e.getMessage());
		}
		
		return dbWorker;
	}
}
