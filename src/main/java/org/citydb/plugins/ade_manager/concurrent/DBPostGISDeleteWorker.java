package org.citydb.plugins.ade_manager.concurrent;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.citydb.event.EventDispatcher;

public class DBPostGISDeleteWorker extends DBDeleteWorker {
	private PreparedStatement deleteStatement;
	private int batchSize = 20;
	
	public DBPostGISDeleteWorker(EventDispatcher eventDispatcher) throws SQLException {
		super(eventDispatcher);
		deleteStatement = connection.prepareStatement("select " + dbSchema + ".del_cityobject(array_agg(?))");	
	}

	@Override
	protected void deleteCityObject(long objectId) throws SQLException {
		deleteCounter++;
		deleteStatement.setInt(1, (int)objectId);
		deleteStatement.addBatch();
		if (deleteCounter == batchSize) {
			deleteStatement.executeBatch();
			deleteCounter = 0;
		}				
	}

	@Override
	protected void closeDBStatement() throws SQLException {
		if (deleteStatement != null)
			deleteStatement.close();	
			
	}

	@Override
	protected void postCommit() throws SQLException {
		if (deleteCounter > 0)
			deleteStatement.executeBatch();		
	}

}
