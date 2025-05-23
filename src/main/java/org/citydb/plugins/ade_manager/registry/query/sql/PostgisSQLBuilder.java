/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
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

public class PostgisSQLBuilder extends AbstractSQLBuilder{

	@Override
	public String create_query_selfref_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT a.attname ")
				  .append("FROM pg_constraint c ")
				  .append("JOIN pg_attribute a ")
				      .append("ON a.attrelid = c.conrelid ")
				      .append("AND a.attnum = ANY (c.conkey) ")
				  .append("WHERE c.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND c.conrelid = c.confrelid ")
				      .append("AND c.contype = 'f'");

		return strBuilder.toString();
	}

	@Override
	public String create_query_ref_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
			  	 	  .append("c.confrelid::regclass::text AS root_table_name, ")
			  	 	  .append("c.conrelid::regclass::text AS n_table_name, ")
			  	 	  .append("a.attname::text AS n_fk_column_name ")
			  	  .append("FROM ")
			  	  	  .append("pg_constraint c ")
			  	  .append("JOIN ")
			  	  	  .append("pg_attribute a ")
			  	  	  .append("ON a.attrelid = c.conrelid ")
			  	  	  .append("AND a.attnum = ANY (c.conkey) ")
			  	  .append("WHERE ")
			  	       .append("c.confrelid::regclass::text = '").append(tableName).append("' ")
			  	       .append("AND c.conrelid <> c.confrelid ")
			  	       .append("AND c.contype = 'f' ")
			  	  .append("ORDER BY ")
			  	  	   .append("n_table_name, ")
			  	  	   .append("n_fk_column_name");

		return strBuilder.toString();
	}

	@Override
	public String create_query_ref_to_parent_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
				      .append("f.confrelid::regclass::text AS parent_table ")
				  .append("FROM ")
				      .append("pg_constraint f, ")
				      .append("pg_constraint p ")
				  .append("WHERE ")
				      .append("f.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND p.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND f.conkey = p.conkey ")
				      .append("AND f.contype = 'f' ")
				      .append("AND p.contype = 'p' ");

		return strBuilder.toString();
	}

	@Override
	public String create_query_ref_to_fk(String tableName, String schemaName) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
				      .append("c.confrelid::regclass::text AS ref_table_name, ")
				      .append("string_agg(a.attname::text,',' order by a.attnum) AS fk_columns ")
				  .append("FROM ")
				      .append("pg_constraint c ")
				  .append("JOIN ")
				      .append("pg_attribute a ")
				      .append("ON a.attrelid = c.conrelid ")
				      .append("AND a.attnum = ANY (c.conkey) ")
				  .append("JOIN ")
				      .append("pg_attribute a_ref ")
				      .append("ON a_ref.attrelid = c.confrelid ")
				      .append("AND a_ref.attnum = ANY (c.confkey) ")
				  .append("WHERE ")
				      .append("c.conrelid::regclass::text = '").append(tableName).append("' ")
				      .append("AND c.conrelid <> c.confrelid ")
				      .append("AND upper(a.attnum::text) <> upper('id') ")
				      .append("AND c.contype = 'f' ")
				  .append("GROUP BY ")
				      .append("c.confrelid");

		return strBuilder.toString();
	}

	@Override
	public String create_query_associative_tables(String schemaName) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("SELECT ")
				  		.append("table_name ")
				  .append("FROM ")
				  		.append("information_schema.tables at ")
				  .append("WHERE ")
				  		.append("NOT EXISTS ( ")
				  			.append("SELECT ")
				  				.append("table_name ")
				  			.append("FROM " )
				  				.append("information_schema.columns c ")
				  			.append("WHERE ")
				  				.append("c.table_name = at.table_name AND ")
				  				.append("c.column_name = 'id' ")
				  		.append(") ")
				  .append("AND table_schema = '").append(schemaName).append("'");

		return strBuilder.toString();
	}
}
