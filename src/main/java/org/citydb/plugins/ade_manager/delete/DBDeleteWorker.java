package org.citydb.plugins.ade_manager.delete;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.Worker;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.config.project.global.LogLevel;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.Event;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.event.global.EventType;
import org.citydb.event.global.InterruptEvent;
import org.citydb.event.global.ObjectCounterEvent;
import org.citydb.event.global.ProgressBarEventType;
import org.citydb.event.global.StatusDialogProgressBar;
import org.citydb.log.Logger;

public class DBDeleteWorker extends Worker<DBSplittingResult> implements EventHandler {
	private final ReentrantLock mainLock = new ReentrantLock();
	private final Logger LOG = Logger.getInstance();
	private final EventDispatcher eventDispatcher;	
	private final CallableStatement deleteCall;	
	private final BundledDBConnection bundledConnection;	
	private volatile boolean shouldRun = true;
	
	public DBDeleteWorker(EventDispatcher eventDispatcher, BundledDBConnection bundledConnection) throws SQLException {
		this.eventDispatcher = eventDispatcher;
		this.eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
		this.bundledConnection = bundledConnection;
		AbstractDatabaseAdapter databaseAdapter = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter();
		String dbSchema = databaseAdapter.getConnectionDetails().getSchema();	
		DatabaseType databaseType = databaseAdapter.getDatabaseType();
		Connection connection = bundledConnection.getOrCreateConnection();
		deleteCall = connection.prepareCall("{? = call " + dbSchema + "."
				+ (databaseType == DatabaseType.ORACLE ? "citydb_delete." : "") + "del_cityobject(?)}");
	}
	
	@Override
	public void interrupt() {
		shouldRun = false;
		bundledConnection.setShouldRollback(true);
		workerThread.interrupt();
	}

	@Override
	public void interruptIfIdle() {
		final ReentrantLock runLock = this.mainLock;
		shouldRun = false;

		if (runLock.tryLock()) {
			try {
				workerThread.interrupt();
			} finally {
				runLock.unlock();
			}
		}
	}

	@Override
	public void run() {
		try {
			if (firstWork != null) {
				lockAndDoWork(firstWork);
				firstWork = null;
			}

			while (shouldRun) {
				try {
					DBSplittingResult work = workQueue.take();
					lockAndDoWork(work);					
				} catch (InterruptedException ie) {
					// re-check state
				}
			}
		} finally {
			shutdown();
		}
	}
	
	private void lockAndDoWork(DBSplittingResult work) {
		final ReentrantLock lock = this.mainLock;
		lock.lock();
		
		try {
			doWork(work);
		} finally {
			lock.unlock();
		}
	}
	
	public void doWork(DBSplittingResult work) {
		long objectId = work.getId();
		int objectclassId = work.getObjectType().getObjectClassId();
		String objectclassName = work.getObjectType().getPath(); 
		boolean accept = false;  
		
		try {
			deleteCall.registerOutParameter(1, Types.INTEGER);
			deleteCall.setInt(2, (int)objectId);
			deleteCall.executeUpdate();	
			
			int deletedObjectId = deleteCall.getInt(1);
			if (deletedObjectId == objectId) {
				LOG.debug(objectclassName + " (RowID = " + objectId + ") deleted");
				accept = true;
			} 				
			else {
				LOG.warn(objectclassName + " (RowID = " + objectId + ") has not been found in the database.");
			}
		} catch (SQLException e) {
			eventDispatcher.triggerEvent(new InterruptEvent(
					"Failed to delete " + objectclassName + " (RowID = " + objectId + "). Abort and rollback transactions.",
					LogLevel.WARN, e, eventChannel, this));
			bundledConnection.setShouldRollback(true);		
		} finally {
			updateDeleteContext(objectclassId, accept);
		}		
	}

	public void shutdown() {
		try {
			if (deleteCall != null)
				deleteCall.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		eventDispatcher.removeEventHandler(this);
	}

	private void updateDeleteContext(int objectclassId, boolean accept) {
		HashMap<Integer, Long> objectCounter = new HashMap<>();
		objectCounter.put(objectclassId, (long) 1);
		if (accept)
			eventDispatcher.triggerEvent(new ObjectCounterEvent(objectCounter, this));		
		eventDispatcher.triggerEvent(new StatusDialogProgressBar(ProgressBarEventType.UPDATE, 1, this));
	}

	@Override
	public void handleEvent(Event event) throws Exception {
		if (event.getChannel() == eventChannel) 
			shouldRun = false;		 			
	}

}
