package org.citydb.plugins.ade_manager.concurrent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.DefaultWorker;
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

public abstract class DBDeleteWorker extends DefaultWorker<DBSplittingResult> implements EventHandler {
	protected final EventDispatcher eventDispatcher;	
	protected Connection connection;
	protected String dbSchema;
	protected int deleteCounter = 0;
	protected boolean AbortedDueToError = false;
	protected final Logger LOG = Logger.getInstance();
	
	public DBDeleteWorker(EventDispatcher eventDispatcher) throws SQLException {
		this.eventDispatcher = eventDispatcher;
		this.eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
		connection = DatabaseConnectionPool.getInstance().getConnection();
		connection.setAutoCommit(false);	
		dbSchema = connection.getSchema();		
	}
	
	@Override
	public void doWork(DBSplittingResult work) {
		long objectId = work.getId();
		int objectclassId = work.getObjectType().getObjectClassId();
		LOG.debug("ADE Object (" + objectId + ") deleted");
		try {
			deleteCityObject(objectId);
			updateDeleteContext(objectclassId);
		} catch (SQLException e) {
			AbortedDueToError = true;
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
			if (!AbortedDueToError) {
				postCommit();
				connection.commit();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		finally {
			try {
				closeDBStatement();
			} catch (SQLException e2) {
				e2.printStackTrace();
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
	
	protected abstract void deleteCityObject(long objectId) throws SQLException;
	protected abstract void closeDBStatement() throws SQLException;
	protected abstract void postCommit() throws SQLException;
	
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
