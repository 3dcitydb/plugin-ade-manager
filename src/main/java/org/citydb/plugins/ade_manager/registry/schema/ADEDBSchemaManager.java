package org.citydb.plugins.ade_manager.registry.schema;

import java.sql.SQLException;
import java.util.List;

import org.citydb.plugins.ade_manager.registry.datatype.MnRefEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencedEntry;
import org.citydb.plugins.ade_manager.registry.datatype.ReferencingEntry;

public interface ADEDBSchemaManager {
	public void createADEDatabaseSchema() throws SQLException;
	public void dropADEDatabaseSchema(String adeId) throws SQLException;
	public List<String> query_selfref_fk(String tableName, String schemaName) throws SQLException;
	public void cleanupADEData(String adeId) throws SQLException;
	public List<MnRefEntry> query_ref_fk(String tableName, String schemaName) throws SQLException;
	public List<ReferencingEntry> query_ref_tables_and_columns(String tableName, String schemaName) throws SQLException;
	public String query_ref_to_parent_fk(String tableName, String schemaName) throws SQLException;
	public List<ReferencedEntry> query_ref_to_fk(String tableName, String schemaName) throws SQLException;
}
