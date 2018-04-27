package org.citydb.plugins.ade_manager.registry.schema;

import java.sql.SQLException;

public interface ADEDBSchemaManager {
	public void createADEDatabaseSchema() throws SQLException;
	public void dropADEDatabaseSchema(String adeId) throws SQLException;
	public void cleanupADEData(String adeId) throws SQLException;
}
