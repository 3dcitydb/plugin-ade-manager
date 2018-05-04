package org.citydb.plugins.ade_manager.concurrent;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.DefaultWorker;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.config.project.global.LogLevel;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.Event;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.event.global.EventType;
import org.citydb.event.global.InterruptEvent;
import org.citydb.event.global.ObjectCounterEvent;
import org.citydb.event.global.ProgressBarEventType;
import org.citydb.event.global.StatusDialogProgressBar;

import oracle.jdbc.OracleTypes;

public class DBDeleteWorker extends DefaultWorker<DBSplittingResult>  implements EventHandler {
	private final EventDispatcher eventDispatcher;	
	private Connection connection;
	private DatabaseType databaseType;
	private String dbSchema;
	private CallableStatement deleteCall;
	
	public DBDeleteWorker(EventDispatcher eventDispatcher) throws SQLException {
		this.eventDispatcher = eventDispatcher;
		this.eventDispatcher.addEventHandler(EventType.INTERRUPT, this);

		connection = DatabaseConnectionPool.getInstance().getConnection();
		connection.setAutoCommit(false);	
		dbSchema = connection.getSchema();		
		databaseType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();		
		
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
		int objectclassId = work.getObjectType().getObjectClassId();
		
		try {
			callDeleteObject(objectId);
			updateDeleteContext(objectclassId);
		} catch (SQLException e) {
			eventDispatcher.triggerEvent(new InterruptEvent("Aborting delete due to errors.", LogLevel.WARN, e, eventChannel, this));
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
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
					e2.printStackTrace();
				}
			}	
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			connection = null;			
			eventDispatcher.removeEventHandler(this);
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
		else {}
		
		deleteCall.executeUpdate();		
	}
	
	private void updateDeleteContext(int objectclassId) {
		HashMap<Integer, Long> objectCounter = new HashMap<>();
		objectCounter.put(objectclassId, (long) 1);
		eventDispatcher.triggerEvent(new ObjectCounterEvent(objectCounter, this));
		eventDispatcher.triggerEvent(new StatusDialogProgressBar(ProgressBarEventType.UPDATE, 1, this));
	}

	@Override
	public void handleEvent(Event event) throws Exception {
		if (event.getChannel() == eventChannel)
			interrupt();
	}

}
