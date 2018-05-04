package org.citydb.plugins.ade_manager.concurrent;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.DefaultWorker;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;

import oracle.jdbc.OracleTypes;

public class DBDeleteWorker extends DefaultWorker<DBSplittingResult> {
	private Connection connection;
	private DatabaseType databaseType;
	private String dbSchema;
	private CallableStatement deleteCall;
	
	public DBDeleteWorker() throws SQLException {
		connection = DatabaseConnectionPool.getInstance().getConnection();
		connection.setAutoCommit(false);	
		
		databaseType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();
		dbSchema = DatabaseConnectionPool.getInstance().getConnection().getSchema();
		
		if (databaseType == DatabaseType.ORACLE) {
			deleteCall = connection.prepareCall("{? = call citydb_delete.del_cityobject(ID_ARRAY(?))}");
		} 
		else if(databaseType == DatabaseType.POSTGIS) {
			deleteCall = connection.prepareCall("{? = call " + dbSchema + ".del_cityobject(?)}");
		} 
		else
			throw new SQLException("Failed to start DBDeleteWorker due to unsupported databasetype: " + databaseType);
	}
	
	@Override
	public void doWork(DBSplittingResult work) {
		long objectId = work.getId();

		try {
			callDeleteObject(objectId);
		} catch (SQLException e) {
			e.printStackTrace();
			
			try {
				connection.rollback();
			} catch (SQLException e1) {
				//
			}
			
			interrupt();
		}
	}

	@Override
	public void shutdown() {
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		finally {
			if (deleteCall != null) {
				try {
					deleteCall.close();
				} catch (SQLException e2) {
					//
				}
			}			
		}
	}
	
	private void callDeleteObject(long objectId) throws SQLException {
		if (databaseType == DatabaseType.ORACLE) {
			deleteCall.registerOutParameter(1, OracleTypes.ARRAY, "ID_ARRAY");
			deleteCall.setInt(2, (int)objectId);
		}			
		else if (databaseType == DatabaseType.POSTGIS) {
			deleteCall.registerOutParameter(1, Types.INTEGER);
			Object[] inputArray = new Object[1];
			inputArray[0] = objectId;
			Array array = connection.createArrayOf("INTEGER", inputArray);
			deleteCall.setArray(2, array);
		}
		else {
			//
		}

		deleteCall.executeUpdate();		
	}

}
