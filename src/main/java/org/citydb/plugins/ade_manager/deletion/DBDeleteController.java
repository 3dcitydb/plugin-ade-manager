package org.citydb.plugins.ade_manager.deletion;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.citydb.citygml.exporter.CityGMLExportException;
import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.PoolSizeAdaptationStrategy;
import org.citydb.concurrent.WorkerPool;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
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
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.query.Query;
import org.citydb.query.builder.QueryBuildException;
import org.citydb.query.builder.config.ConfigQueryBuilder;
import org.citydb.util.Util;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.model.gml.GMLClass;

public class DBDeleteController implements EventHandler {
	private final Logger log = Logger.getInstance();
	private final AbstractDatabaseAdapter databaseAdapter;
	private final SchemaMapping schemaMapping;
	private final ConfigImpl config;
	private final EventDispatcher eventDispatcher;
	private DBDeleteSplitter dbSplitter;

	private volatile boolean shouldRun = true;
	private AtomicBoolean isInterrupted = new AtomicBoolean(false);
	private WorkerPool<DBSplittingResult> dbWorkerPool;
	private HashMap<Integer, Long> objectCounter;
	
	public DBDeleteController(CityGMLBuilder cityGMLBuilder, 
			SchemaMapping schemaMapping, 
			ConfigImpl config, 
			EventDispatcher eventDispatcher) {
		this.schemaMapping = schemaMapping;
		this.config = config;
		this.eventDispatcher = eventDispatcher;

		databaseAdapter = DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter();
		objectCounter = new HashMap<>();
		new EnumMap<>(GMLClass.class);
	}

	public void cleanup() {
		eventDispatcher.removeEventHandler(this);
	}

	public boolean doProcess() throws CityGMLExportException {
		long start = System.currentTimeMillis();
		
		// adding listeners
		eventDispatcher.addEventHandler(EventType.OBJECT_COUNTER, this);
		eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
				
		// build query from filter settings
		Query query = null;
		try {
			ConfigQueryBuilder queryBuilder = new ConfigQueryBuilder(schemaMapping, databaseAdapter);
			query = queryBuilder.buildQuery(config.getDeleteQuery(), config.getNamespaceFilter());
		} catch (QueryBuildException e) {
			throw new CityGMLExportException("Failed to build the export query expression.", e);
		}

		dbWorkerPool = new WorkerPool<DBSplittingResult>(
				"db_deleter_pool",
				2,
				10,
				PoolSizeAdaptationStrategy.AGGRESSIVE,
				new DBDeleteWorkerFactory(),
				300,
				false);

		dbWorkerPool.prestartCoreWorkers();

		// fail if we could not start a single import worker
		if (dbWorkerPool.getPoolSize() == 0)
			throw new CityGMLExportException("Failed to start database export worker pool. Check the database connection pool settings.");

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
			throw new CityGMLExportException("Failed to query the database.", e);
		}

		try {
			dbWorkerPool.shutdownAndWait();
		} catch (InterruptedException e) {
			throw new CityGMLExportException("Failed to shutdown worker pools.", e);
		}

		// show exported features
		if (!objectCounter.isEmpty()) {
			log.info("Exported city objects:");
			Map<String, Long> typeNames = Util.mapObjectCounter(objectCounter, schemaMapping);					
			typeNames.keySet().stream().sorted().forEach(object -> log.info(object + ": " + typeNames.get(object)));			
		}

		if (shouldRun)
			log.info("Total export time: " + Util.formatElapsedTime(System.currentTimeMillis() - start) + ".");

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
