package org.citydb.plugins.ade_manager.concurrent;

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
	private volatile boolean shouldRun = true;
	private CallableStatement deleteCall;	
	private String dbSchema;
	
	public DBDeleteWorker(EventDispatcher eventDispatcher, Connection connection) throws SQLException {
		this.eventDispatcher = eventDispatcher;
		this.eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
		dbSchema = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getConnectionDetails().getSchema();	
		DatabaseType databaseType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();
		deleteCall = connection.prepareCall("{? = call " + dbSchema + "."
				+ (databaseType == DatabaseType.ORACLE ? "citydb_delete." : "") + "del_cityobject(?)}");
	}
	
	@Override
	public void interrupt() {
		shouldRun = false;
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
		try {
			deleteCall.registerOutParameter(1, Types.INTEGER);
			deleteCall.setInt(2, (int)objectId);
			deleteCall.executeUpdate();	
			int deletedObjectId = deleteCall.getInt(1);
			if (deletedObjectId == objectId) {
				LOG.debug(objectclassName + " (RowID = " + objectId + ") deleted");			
			}
			else {
				LOG.warn("Failed to delete " + objectclassName + " (RowID = " + objectId + ")");
			}
			updateDeleteContext(objectclassId);
		} catch (SQLException e) {
			eventDispatcher.triggerEvent(new InterruptEvent("Aborting delete due to errors.", LogLevel.WARN, e, eventChannel, this));			
		}
	}

	public void shutdown() {
		try {
			if (deleteCall != null)
				deleteCall.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		finally {
			eventDispatcher.removeEventHandler(this);
		}
	}

	private void updateDeleteContext(int objectclassId) {
		HashMap<Integer, Long> objectCounter = new HashMap<>();
		objectCounter.put(objectclassId, (long) 1);
		eventDispatcher.triggerEvent(new ObjectCounterEvent(objectCounter, this));
		eventDispatcher.triggerEvent(new StatusDialogProgressBar(ProgressBarEventType.UPDATE, 1, this));
	}

	@Override
	public void handleEvent(Event event) throws Exception {
		if (event.getChannel() == eventChannel) {
			shouldRun = false;
		} 			
	}

}
