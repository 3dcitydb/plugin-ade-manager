package org.citydb.plugins.ade_manager.registry.query.sql;

public interface SQLBuilder {	
	public String create_query_selfref_fk(String tableName, String schemaName);	
	public String create_query_ref_fk(String tableName, String schemaName);
	public String create_query_ref_to_parent_fk(String tableName, String schemaName) ;
	public String create_query_ref_to_fk(String tableName, String schemaName);
	public String create_query_associative_tables(String schemaName);
}
