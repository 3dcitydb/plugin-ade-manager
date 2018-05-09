package org.citydb.plugins.ade_manager.concurrent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.Worker;
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

public abstract class DBDeleteWorker extends Worker<DBSplittingResult> implements EventHandler {
	private final ReentrantLock mainLock = new ReentrantLock();
	private volatile boolean shouldRun = true;
	
	protected final EventDispatcher eventDispatcher;	
	protected Connection connection;
	protected String dbSchema;
	protected boolean stoppedDuetoErrorOrCancel = false;
	protected final Logger LOG = Logger.getInstance();
	protected final String defaultSchema = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getSchemaManager().getDefaultSchema();
	
	public DBDeleteWorker(EventDispatcher eventDispatcher) throws SQLException {
		this.eventDispatcher = eventDispatcher;
		this.eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
		connection = DatabaseConnectionPool.getInstance().getConnection();	
		dbSchema = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getConnectionDetails().getSchema();	
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
		LOG.debug("City object (RowID = " + objectId + ") deleted");
		try {
			deleteCityObject(objectId);
			updateDeleteContext(objectclassId);
		} catch (SQLException e) {
			eventDispatcher.triggerEvent(new InterruptEvent("Aborting delete due to errors.", LogLevel.WARN, e, eventChannel, this));			
		}
	}

	public void shutdown() {
		try {
			closeDBStatement();
			if (stoppedDuetoErrorOrCancel) {
				if (!connection.getAutoCommit()) {
					connection.rollback();
				}				
			} 	
			else {
				if (!connection.getAutoCommit())
					connection.commit();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					//
				}
			}
			
			eventDispatcher.removeEventHandler(this);
		}
	}
	
	protected abstract void deleteCityObject(long objectId) throws SQLException;
	protected abstract void closeDBStatement() throws SQLException;

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
			stoppedDuetoErrorOrCancel= true;
		} 			
	}

}
