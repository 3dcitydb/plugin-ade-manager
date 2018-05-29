package org.citydb.plugins.ade_manager.deletion;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.PoolSizeAdaptationStrategy;
import org.citydb.concurrent.WorkerPool;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.event.Event;
import org.citydb.event.EventDispatcher;
import org.citydb.event.EventHandler;
import org.citydb.event.global.EventType;
import org.citydb.event.global.InterruptEvent;
import org.citydb.event.global.ObjectCounterEvent;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.concurrent.DBDeleteWorkerFactory;
import org.citydb.query.Query;
import org.citydb.query.builder.QueryBuildException;
import org.citydb.registry.ObjectRegistry;
import org.citydb.util.Util;

import oracle.jdbc.OracleTypes;

public class DBDeleteController implements EventHandler {
	private final Logger log = Logger.getInstance();
	private final SchemaMapping schemaMapping;
	private final EventDispatcher eventDispatcher;
	private DBDeleteSplitter dbSplitter;
	private volatile boolean shouldRun = true;
	private AtomicBoolean isInterrupted = new AtomicBoolean(false);
	private WorkerPool<DBSplittingResult> dbWorkerPool;
	private HashMap<Integer, Long> objectCounter;
	private Query query;
	
	public DBDeleteController(Query query) {				
		this.schemaMapping = ObjectRegistry.getInstance().getSchemaMapping();
		this.eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
		this.objectCounter = new HashMap<>();
		this.query = query;	
	}

	public void cleanup() {
		eventDispatcher.removeEventHandler(this);
	}

	public boolean doProcess() throws DBDeleteException {
		long start = System.currentTimeMillis();
		int minThreads = 2;
		int maxThreads = Math.max(minThreads, Runtime.getRuntime().availableProcessors());
		
		// adding listeners
		eventDispatcher.addEventHandler(EventType.OBJECT_COUNTER, this);
		eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
		
		Connection connection = null;
		try {		
			try {
				connection = DatabaseConnectionPool.getInstance().getConnection();
				connection.setAutoCommit(false);
			} catch (SQLException e1) {
				throw new DBDeleteException("Failed to initilize a database connection.");
			}	
			
			dbWorkerPool = new WorkerPool<DBSplittingResult>(
					"db_deleter_pool",
					minThreads,
					maxThreads,
					PoolSizeAdaptationStrategy.AGGRESSIVE,
					new DBDeleteWorkerFactory(eventDispatcher, connection),
					300,
					false);

			dbWorkerPool.prestartCoreWorkers();

			if (dbWorkerPool.getPoolSize() == 0)
				throw new DBDeleteException("Failed to start database delete worker pool. Check the database connection pool settings.");

			// get database splitter and start query
			dbSplitter = null;
			try {
				dbSplitter = new DBDeleteSplitter(
						schemaMapping,
						dbWorkerPool,
						query,
						eventDispatcher);

				if (shouldRun) {
					dbSplitter.setCalculateNumberMatched(true);
					dbSplitter.startQuery();
				}
			} catch (SQLException | QueryBuildException e) {
				throw new DBDeleteException("Failed to query the database.", e);
			}

			try {
				dbWorkerPool.shutdownAndWait();
			} catch (InterruptedException e) {
				throw new DBDeleteException("Failed to shutdown worker pools.", e);
			}
		} finally {
			if (shouldRun) {			
				try {
					if (!connection.getAutoCommit())
						connection.commit();					
					connection.close();
				} catch (SQLException e) {
					//
				}
			}
			else {
				try {
					if (!connection.getAutoCommit())
						connection.rollback();				
				} catch (SQLException e) {
					//
				}
			}

			// clean up
			if (dbWorkerPool != null && !dbWorkerPool.isTerminated())
				dbWorkerPool.shutdownNow();

			try {
				eventDispatcher.flushEvents();
			} catch (InterruptedException e) {
				//
			}
			
			DatabaseConnectionPool.getInstance().purge();	
		}		
		
		// show exported features
		if (!objectCounter.isEmpty()) {
			log.info("Deleted city objects:");
			Map<String, Long> typeNames = Util.mapObjectCounter(objectCounter, schemaMapping);					
			typeNames.keySet().stream().sorted().forEach(object -> log.info(object + ": " + typeNames.get(object)));			
		}

		if (shouldRun)
			log.info("Process time: " + Util.formatElapsedTime(System.currentTimeMillis() - start) + ".");

		objectCounter.clear();
		
		return shouldRun;
	}
	
	public boolean cleanupGlobalAppearances() throws DBDeleteException {
		String dbSchema = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getConnectionDetails().getSchema();	
		DatabaseType databaseType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();	
		Connection connection = null;
		Statement cleanupStmt = null;
		int sum = 0;
		
		try {
			connection = DatabaseConnectionPool.getInstance().getConnection();	
			if (databaseType == DatabaseType.ORACLE) {		
				cleanupStmt = connection.prepareCall("{? = call " + dbSchema + ".citydb_delete.cleanup_global_appearances()}");
				((CallableStatement)cleanupStmt).registerOutParameter(1, OracleTypes.ARRAY, dbSchema.trim().toUpperCase() + ".ID_ARRAY");
				((CallableStatement)cleanupStmt).execute();			
				BigDecimal[] results = (BigDecimal[]) ((CallableStatement)cleanupStmt).getArray(1).getArray();           
				sum = results.length;
			}
			else if (databaseType == DatabaseType.POSTGIS) {						
				cleanupStmt = connection.prepareStatement("select " + dbSchema + ".cleanup_global_appearances()");
				ResultSet rs = ((PreparedStatement)cleanupStmt).executeQuery();	
				while (rs.next()) {
					sum++;
				} 					
			}
			else
				throw new DBDeleteException("Unsupported database type for running appearance cleanup.");
		} catch (SQLException e) {
			throw new DBDeleteException("Failed to cleanup global appearances.", e);
		} finally {
			if (cleanupStmt != null) {
				try {
					cleanupStmt.close();
				} catch (SQLException e) {
					//
				}
			}							
			if (connection != null) {
				try {
					if (!connection.getAutoCommit())
						connection.commit();
					connection.close();
				} catch (SQLException e) {
					//
				}
			}
			DatabaseConnectionPool.getInstance().purge();	
		}
		
		log.info("Cleaned up global appearances: " + sum);
		
		return shouldRun;
	}
	
	public boolean cleanupSchema() throws DBDeleteException {
		String dbSchema = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		DatabaseType databaseType = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getDatabaseType();	
		Connection connection = null;
		CallableStatement cleanupStmt = null;;
		
		try {
			connection = DatabaseConnectionPool.getInstance().getConnection();	
			cleanupStmt = connection.prepareCall("{call " + dbSchema + "."
					+ (databaseType == DatabaseType.ORACLE ? "citydb_delete." : "") + "cleanup_schema()}");
			cleanupStmt.execute();	
		} catch (SQLException e) {
			throw new DBDeleteException("Failed to cleanup data schema.", e);
		} finally {
			if (cleanupStmt != null) {
				try {
					cleanupStmt.close();
				} catch (SQLException e) {
					//
				}
			}							
			if (connection != null) {
				try {
					if (!connection.getAutoCommit())
						connection.commit();
					connection.close();
				} catch (SQLException e) {
					//
				}
			}			
			DatabaseConnectionPool.getInstance().purge();			
		}

		return shouldRun;
	}
	
	@Override
	public void handleEvent(Event e) throws Exception {
		if (e.getEventType() == EventType.OBJECT_COUNTER) {
			HashMap<Integer, Long> counter = ((ObjectCounterEvent)e).getCounter();
			
			for (Entry<Integer, Long> entry : counter.entrySet()) {
				Long tmp = objectCounter.get(entry.getKey());
				objectCounter.put(entry.getKey(), tmp == null ? entry.getValue() : tmp + entry.getValue());
			}
		}

		else if (e.getEventType() == EventType.INTERRUPT) {
			if (isInterrupted.compareAndSet(false, true)) {
				shouldRun = false;
				InterruptEvent interruptEvent = (InterruptEvent)e;

				if (interruptEvent.getCause() != null) {
					Throwable cause = interruptEvent.getCause();

					if (cause instanceof SQLException) {
						Iterator<Throwable> iter = ((SQLException)cause).iterator();
						log.error("A SQL error occurred: " + iter.next().getMessage());
						while (iter.hasNext())
							log.error("Cause: " + iter.next().getMessage());
					} else {
						log.error("An error occurred: " + cause.getMessage());
						while ((cause = cause.getCause()) != null)
							log.error("Cause: " + cause.getMessage());
					}
				}

				String msg = interruptEvent.getLogMessage();
				if (msg != null)
					log.log(interruptEvent.getLogLevelType(), msg);

				if (dbSplitter != null)
					dbSplitter.shutdown();

				if (dbWorkerPool != null)
					dbWorkerPool.drainWorkQueue();
			}
		}
	}
	
}
