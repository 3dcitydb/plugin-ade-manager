package org.citydb.plugins.ade_manager.concurrent;

import java.sql.CallableStatement;
import java.sql.SQLException;
import org.citydb.event.EventDispatcher;

import oracle.jdbc.OracleTypes;

public class DBOracleDeleteWorker extends DBDeleteWorker {
	private CallableStatement deleteCall;
	
	public DBOracleDeleteWorker(EventDispatcher eventDispatcher) throws SQLException {
		super(eventDispatcher);
		deleteCall = connection.prepareCall("{? = call " + dbSchema + ".citydb_delete.del_cityobject(ID_ARRAY(?))}");	
	}

	@Override
	protected void deleteCityObject(long objectId) throws SQLException {
		deleteCall.registerOutParameter(1, OracleTypes.ARRAY, defaultSchema.trim().toUpperCase() + ".ID_ARRAY");
		deleteCall.setInt(2, (int)objectId);
		deleteCall.executeUpdate();		
	}

	@Override
	protected void closeDBStatement() throws SQLException {
		if (deleteCall != null)
			deleteCall.close();
	}

}
