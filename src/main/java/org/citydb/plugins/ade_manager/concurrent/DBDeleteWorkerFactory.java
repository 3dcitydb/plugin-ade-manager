package org.citydb.plugins.ade_manager.concurrent;

import org.citydb.citygml.exporter.database.content.DBSplittingResult;
import org.citydb.concurrent.Worker;
import org.citydb.concurrent.WorkerFactory;

public class DBDeleteWorkerFactory implements WorkerFactory<DBSplittingResult>{

	@Override
	public Worker<DBSplittingResult> createWorker() {

		return null;
	}

}
