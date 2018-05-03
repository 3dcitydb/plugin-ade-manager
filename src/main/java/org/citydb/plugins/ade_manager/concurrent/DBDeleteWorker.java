package org.citydb.plugins.ade_manager.concurrent;

import java.sql.Connection;
import java.sql.SQLException;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.DefaultWorker;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.connection.DatabaseConnectionPool;

public class DBDeleteWorker extends DefaultWorker<DBSplittingResult> {
	private Connection connection;
	private int updateCounter = 0;
	private int commitAfter = 20;
	
	public DBDeleteWorker() throws SQLException {
		AbstractDatabaseAdapter databaseAdapter = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter();
		connection = DatabaseConnectionPool.getInstance().getConnection();
		connection.setAutoCommit(false);
	}
	
	@Override
	public void doWork(DBSplittingResult work) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

}
