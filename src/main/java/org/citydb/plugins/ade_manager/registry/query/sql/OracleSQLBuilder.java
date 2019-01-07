/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2019
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.plugins.ade_manager.registry.query.sql;

public class OracleSQLBuilder extends AbstractSQLBuilder{

	@Override
	public String create_query_selfref_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder(); 
		strBuilder.append("SELECT ")
				  		.append("ac.column_name ")
				  .append("FROM ")
				  		.append("user_cons_columns ac ")
				  .append("JOIN ")
				  		.append("user_constraints c1 ")
				  		.append("ON ac.constraint_name = c1.constraint_name ")
				  		.append("AND ac.table_name = c1.table_name ")
				  .append("JOIN ")
				  		.append("user_constraints c2 ")
				  		.append("ON c2.constraint_name = c1.r_constraint_name ")
				  .append("WHERE ")
				  		.append("c1.table_name = upper('").append(tableName).append("') ")
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
			  			.append("user_constraints c ")
			  		.append("JOIN ")
			  			.append("user_cons_columns a ")
			  			.append("ON a.constraint_name = c.constraint_name ")
			  			.append("AND a.table_name = c.table_name ")
			  		.append("JOIN ")
			  			.append("user_constraints c2 ")
			  			.append("ON c2.constraint_name = c.r_constraint_name ")
			  		.append("WHERE ")
			  			.append("c2.table_name = upper('").append(tableName).append("') ")
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
				  		.append("user_constraints fk ")
				  .append("JOIN ")
				  		.append("user_cons_columns fka ")
				  		.append("ON fka.constraint_name = fk.constraint_name ")
				  		.append("AND fka.table_name = fk.table_name ")
				  .append("JOIN ")
				  		.append("user_constraints p ")
				  		.append("ON p.constraint_name = fk.r_constraint_name ")
				  .append("WHERE ")
				  		.append("fk.table_name = upper('").append(tableName).append("') ")
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
				  		.append("user_constraints c ")
				  .append("JOIN ")
				  		.append("user_cons_columns ac ")
				  		.append("ON ac.constraint_name = c.constraint_name ")
				  		.append("AND ac.table_name = c.table_name ")
				  .append("JOIN ")
				  		.append("user_cons_columns a_ref ")
				  		.append("ON a_ref.constraint_name = c.r_constraint_name ")
				  .append("WHERE ")
				  		.append("c.table_name = upper('").append(tableName).append("') ")
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
				  		.append("user_tables at ")
				  .append("WHERE ")
				  		.append("NOT EXISTS ( ")
				  			.append("SELECT ")
				  				.append("table_name ")
				  			.append("FROM " )
				  				.append("user_cons_columns c ")
				  			.append("WHERE ")
				  				.append("c.table_name = at.table_name AND ")
				  				.append("c.column_name = 'ID' ")
				  		.append(") ")
				  .append("AND at.table_name NOT LIKE '%\\_AUX' ESCAPE '\\' ")
				  .append("AND at.table_name NOT LIKE '%TMP\\_%' ESCAPE '\\' ")
				  .append("AND at.table_name NOT LIKE '%MDRT%' ")
				  .append("AND at.table_name NOT LIKE '%MDXT%' ")
				  .append("AND at.table_name NOT LIKE '%MDNT%' ");
		
		return strBuilder.toString();
	}

}
