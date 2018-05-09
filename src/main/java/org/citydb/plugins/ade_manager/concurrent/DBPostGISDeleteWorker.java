package org.citydb.plugins.ade_manager.concurrent;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.citydb.event.EventDispatcher;

public class DBPostGISDeleteWorker extends DBDeleteWorker {
	private PreparedStatement deleteStatement;

	public DBPostGISDeleteWorker(EventDispatcher eventDispatcher) throws SQLException {
		super(eventDispatcher);
		deleteStatement = connection.prepareStatement("select " + dbSchema + ".del_cityobject(array_agg(?))");	
	}

	@Override
	protected void deleteCityObject(long objectId) throws SQLException {
		deleteStatement.setInt(1, (int)objectId);
		deleteStatement.executeQuery();
			
	}

	@Override
	protected void closeDBStatement() throws SQLException {
		if (deleteStatement != null)
			deleteStatement.close();			
	}

}
