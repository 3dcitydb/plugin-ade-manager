package org.citydb.plugins.ade_manager.util;

import java.io.File;

import org.citydb.config.project.database.DatabaseType;

public class PathResolver {
	private static final String CITYDB_FOLDER_NAME = "3dcitydb";
	private static final String CITYDB_POSTGIS_FOLDER_NAME = "postgreSQL";
	private static final String CITYDB_ORACLE_FOLDER_NAME = "oracle";
	private static final String SCHEMA_MAPPING_FOLDER_NAME = "schema-mapping";
	private static final String SCHEMA_MAPPING_FILE_PATH_NAME = "schema-mapping.xml";

	private static final String CREATE_ADE_DB_FILE_NAME = "CREATE_ADE_DB.sql";
	private static final String DROP_ADE_DB_FILE_NAME = "DROP_ADE_DB.sql";
	private static final String ENABLE_ADE_VERSIONING_FILE_NAME = "ENABLE_ADE_VERSIONING.sql";
	private static final String DISABLE_ADE_VERSIONING_FILE_NAME = "DISABLE_ADE_VERSIONING.sql";

	public static final String get_citydb_folder_path(String rootPath) {
		return rootPath + File.separator + CITYDB_FOLDER_NAME;
	}
	
	public static final String get_citydb_schema_folder_path(String rootPath, DatabaseType databaseType) {
		switch (databaseType) {
		case ORACLE:
			return get_citydb_oracle_folder_path(rootPath);
		case POSTGIS:
			return get_citydb_postgis_folder_path(rootPath);
		}		
		return null;
	}

	public static final String get_create_ade_db_filepath(String rootPath, DatabaseType databaseType) {
		switch (databaseType) {
		case ORACLE:
			return get_oracle_create_ade_db_filepath(rootPath);
		case POSTGIS:
			return get_postgis_create_ade_db_filepath(rootPath);
		}		
		return null;
	} 
	
	public static final String get_drop_ade_db_filepath(String rootPath, DatabaseType databaseType) {
		switch (databaseType) {
		case ORACLE:
			return get_oracle_drop_ade_db_filepath(rootPath);
		case POSTGIS:
			return get_postgis_drop_ade_db_filepath(rootPath);
		}		
		return null;
	} 
	
	public static final String get_enable_ade_versioning_filepath(String rootPath, DatabaseType databaseType) {
		switch (databaseType) {
		case ORACLE:
			return get_oracle_enable_ade_versioning_filepath(rootPath);
		case POSTGIS:
			return null; // not supported
		}		
		return null;
	}

	public static final String get_disable_ade_versioning_filepath(String rootPath, DatabaseType databaseType) {
		switch (databaseType) {
		case ORACLE:
			return get_oracle_disable_ade_versioning_filepath(rootPath);
		case POSTGIS:
			return null; // not supported
		}		
		return null;
	}
	
	public static final String get_schemaMapping_folder_path(String rootPath) {
		return rootPath + File.separator + SCHEMA_MAPPING_FOLDER_NAME;
	}
	
	public static final String get_schemaMapping_filepath(String rootPath) {
		return get_schemaMapping_folder_path(rootPath) + File.separator + SCHEMA_MAPPING_FILE_PATH_NAME;
	}
	
	private static final String get_oracle_enable_ade_versioning_filepath(String rootPath) {
		return get_citydb_oracle_folder_path(rootPath) + File.separator + ENABLE_ADE_VERSIONING_FILE_NAME;
	}

	private static final String get_oracle_disable_ade_versioning_filepath(String rootPath) {
		return get_citydb_oracle_folder_path(rootPath) + File.separator + DISABLE_ADE_VERSIONING_FILE_NAME;
	}
	
	private static final String get_citydb_oracle_folder_path(String rootPath) {
		return get_citydb_folder_path(rootPath) + File.separator + CITYDB_ORACLE_FOLDER_NAME;
	}

	private static final String get_citydb_postgis_folder_path(String rootPath) {
		return get_citydb_folder_path(rootPath) + File.separator + CITYDB_POSTGIS_FOLDER_NAME;
	}

	private static final String get_oracle_create_ade_db_filepath(String rootPath) {
		return get_citydb_oracle_folder_path(rootPath) + File.separator + CREATE_ADE_DB_FILE_NAME;
	}

	private static final String get_postgis_create_ade_db_filepath(String rootPath) {
		return get_citydb_postgis_folder_path(rootPath) + File.separator + CREATE_ADE_DB_FILE_NAME;
	}

	private static final String get_oracle_drop_ade_db_filepath(String rootPath) {
		return get_citydb_oracle_folder_path(rootPath) + File.separator + DROP_ADE_DB_FILE_NAME;
	}

	private static final String get_postgis_drop_ade_db_filepath(String rootPath) {
		return get_citydb_postgis_folder_path(rootPath) + File.separator + DROP_ADE_DB_FILE_NAME;
	}

}