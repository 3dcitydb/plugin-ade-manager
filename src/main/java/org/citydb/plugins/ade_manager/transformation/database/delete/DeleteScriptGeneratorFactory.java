package org.citydb.plugins.ade_manager.transformation.database.delete;


import org.citydb.config.project.database.DatabaseType;
import org.citydb.plugins.ade_manager.transformation.database.delete.adapter.oracle.OracleDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.transformation.database.delete.adapter.postgis.PostgisDeleteGeneratorGenerator;

public class DeleteScriptGeneratorFactory {
	
	public IDeleteScriptGenerator createDatabaseAdapter(DatabaseType databaseType) {
		switch (databaseType) {
		case ORACLE:
			return new OracleDeleteScriptGenerator();
		case POSTGIS:
			return new PostgisDeleteGeneratorGenerator();
		}
		
		return null;
	}
}
