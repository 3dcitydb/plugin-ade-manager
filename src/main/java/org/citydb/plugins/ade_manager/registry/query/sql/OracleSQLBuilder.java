package org.citydb.plugins.ade_manager.registry.query.sql;

public class OracleSQLBuilder extends AbstractSQLBuilder{

	@Override
	public String create_query_selfref_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT ")
				  		.append("ac.column_name ")
				  .append("FROM ")
				  		.append("all_cons_columns ac ")
				  .append("JOIN ")
				  		.append("all_constraints c1 ")
				  		.append("ON ac.constraint_name = c1.constraint_name ")
				  		.append("AND ac.table_name = c1.table_name ")
				  		.append("AND ac.owner = c1.owner " )
				  .append("JOIN ")
				  		.append("all_constraints c2 ")
				  		.append("ON c2.constraint_name = c1.r_constraint_name ")
				  		.append("AND c2.owner = c1.owner ")
				  .append("WHERE ")
				  		.append("c1.table_name = upper('").append(tableName).append("') ")
				  		.append("AND c1.owner = upper('").append(schemaName).append("') ")
				  		.append("AND c1.table_name = c2.table_name ")
				  		.append("AND c1.constraint_type = 'R'");
		
		return strBuilder.toString();
	}

	@Override
	public String create_query_ref_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT ")
			  			.append("c2.table_name AS root_table_name, ")
			  			.append("c.table_name AS n_table_name, ")
			  			.append("a.column_name AS n_fk_column_name ")
			  		.append("FROM ")
			  			.append("all_constraints c ")
			  		.append("JOIN ")
			  			.append("all_cons_columns a ")
			  			.append("ON a.constraint_name = c.constraint_name ")
			  			.append("AND a.table_name = c.table_name ")
			  			.append("AND a.owner = c.owner ")
			  		.append("JOIN ")
			  			.append("all_constraints c2 ")
			  			.append("ON c2.constraint_name = c.r_constraint_name ")
			  			.append("AND c2.owner = c.owner ")
			  		.append("WHERE ")
			  			.append("c2.table_name = upper('").append(tableName).append("') ")
			  			.append("AND c2.owner = upper('").append(schemaName).append("') ")
			  			.append("AND c.table_name <> c2.table_name ")
			  			.append("AND c.constraint_type = 'R' ")
			  		.append("ORDER BY ")
			  			.append("n_table_name, ")
			  			.append("n_fk_column_name");
		
		return strBuilder.toString();
	}

	@Override
	public String create_query_ref_to_parent_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT ")
				  		.append("p.table_name ")
				  .append("FROM ")
				  		.append("all_constraints fk ")
				  .append("JOIN ")
				  		.append("all_cons_columns fka ")
				  		.append("ON fka.constraint_name = fk.constraint_name ")
				  		.append("AND fka.table_name = fk.table_name ")
				  		.append("AND fka.owner = fk.owner " )
				  .append("JOIN ")
				  		.append("all_constraints p ")
				  		.append("ON p.constraint_name = fk.r_constraint_name ")
				  		.append("AND p.owner = fk.owner ")
				  .append("WHERE ")
				  		.append("fk.table_name = upper('").append(tableName).append("') ")
				  		.append("AND fk.owner = upper('").append(schemaName).append("') ")
				  		.append("AND fk.constraint_type = 'R' ")
				  		.append("AND fka.column_name = upper('id')");
		
		return strBuilder.toString();
	}

	@Override
	public String create_query_ref_to_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT ")
				  		.append("a_ref.table_name AS ref_table_name, ")
				  		.append("LISTAGG(ac.column_name, ',') WITHIN GROUP (ORDER BY ac.position) AS fk_columns ")
				  .append("FROM ")
				  		.append("all_constraints c ")
				  .append("JOIN ")
				  		.append("all_cons_columns ac ")
				  		.append("ON ac.constraint_name = c.constraint_name ")
				  		.append("AND ac.table_name = c.table_name ")
				  		.append("AND ac.owner = c.owner ")
				  .append("JOIN ")
				  		.append("all_cons_columns a_ref ")
				  		.append("ON a_ref.constraint_name = c.r_constraint_name ")
				  		.append("AND a_ref.owner = c.owner ")
				  .append("WHERE ")
				  		.append("c.table_name = upper('").append(tableName).append("') ")
				  		.append("AND c.owner = upper('").append(schemaName).append("') ")
				  		.append("AND c.table_name <> a_ref.table_name ")
				  		.append("AND ac.column_name <> upper('id') ")
				  		.append("AND c.constraint_type = 'R' ")
				  .append("GROUP BY ")
				  		.append("a_ref.table_name, ")
				  		.append("a_ref.column_name");				  

		return strBuilder.toString();
	}

	@Override
	public String create_query_associative_tables(String schemaName) {
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT ")
				  		.append("table_name ")
				  .append("FROM ")
				  		.append("all_tables at ")
				  .append("WHERE ")
				  		.append("NOT EXISTS ( ")
				  			.append("SELECT ")
				  				.append("table_name ")
				  			.append("FROM " )
				  				.append("all_cons_columns c ")
				  			.append("WHERE ")
				  				.append("c.table_name = at.table_name AND ")
				  				.append("c.column_name = 'ID' ")
				  		.append(") ")
				  .append("AND at.owner = upper('").append(schemaName).append("') ")
				  .append("AND at.table_name NOT LIKE '%\\_AUX' ESCAPE '\\' ")
				  .append("AND at.table_name NOT LIKE '%TMP\\_%' ESCAPE '\\' ")
				  .append("AND at.table_name NOT LIKE '%MDRT%' ")
				  .append("AND at.table_name NOT LIKE '%MDXT%' ")
				  .append("AND at.table_name NOT LIKE '%MDNT%' ");
		
		return strBuilder.toString();
	}

}
