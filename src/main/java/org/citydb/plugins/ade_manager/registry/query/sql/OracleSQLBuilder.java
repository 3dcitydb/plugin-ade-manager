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
				  		.append("n.root_table_name, ")
				  		.append("n.n_table_name, ")
				  		.append("n.n_fk_column_name, ")
				  		.append("m.m_table_name, ")
				  		.append("m.m_fk_column_name ")
				  .append("FROM ( ")
				  		.append("SELECT ")
				  			.append("c2.table_name AS root_table_name, ")
				  			.append("c.table_name AS n_table_name, ")
				  			.append("c.owner, ")
				  			.append("a.column_name AS n_fk_column_name, ")
				  			.append("c.delete_rule ")
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
				  		.append(") n ")
				  	.append("LEFT JOIN (")
				  		.append("SELECT ")
				  			.append("mn.table_name, ")
				  			.append("mn.owner, ")
				  			.append("mn2.table_name AS m_table_name, ")
				  			.append("mnpa.column_name, ")
				  			.append("mna.column_name AS m_fk_column_name ")
				  		.append("FROM ")
				  			.append("all_constraints mn ")
				  		.append("JOIN ")
				  			.append("all_cons_columns mna ")
				  			.append("ON mna.constraint_name = mn.constraint_name ")
				  			.append("AND mna.table_name = mn.table_name ")
				  			.append("AND mna.owner = mn.owner ")
				  			.append("AND mn.constraint_type = 'R' ")
				  			.append("AND mna.column_name <> upper('id') ")
				  		.append("JOIN ")
				  			.append("all_constraints mnp ")
				  			.append("ON mnp.table_name = mn.table_name ")
				  			.append("AND mnp.owner = mn.owner ")
				  			.append("AND mnp.constraint_type = 'P' ")
				  		.append("JOIN ")
				  			.append("all_cons_columns mnpa ")
				  			.append("ON mnpa.constraint_name = mnp.constraint_name ")
				  			.append("AND mnpa.table_name = mnp.table_name ")
				  			.append("AND mnpa.owner = mnp.owner ")
				  			.append("AND mnpa.column_name = mna.column_name ")
				  		.append("JOIN ")
				  			.append("all_constraints mn2 ")
				  			.append("ON mn2.constraint_name = mn.r_constraint_name ")
				  			.append("AND mn2.owner = mn.owner ")
				  		.append("JOIN ")
				  			.append("all_cons_columns mna_ref ")
				  			.append("ON mna_ref.constraint_name = mn2.constraint_name ")
				  			.append("AND mna_ref.table_name = mn2.table_name ")
				  			.append("AND mna_ref.owner = mn2.owner ")
				  		.append(") m ")
				  		.append("ON m.table_name = n.n_table_name ")
				  		.append("AND m.owner = n.owner ")
				  	.append("WHERE ")
				  		.append("n.root_table_name <> m.m_table_name OR m.m_table_name IS NULL ")
				  	.append("ORDER BY ")
				  		.append("n.n_table_name, ")
				  		.append("m.m_table_name");
		
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

}
