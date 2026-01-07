/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2026
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
package org.citydb.plugins.ade_manager.registry.schema;

/**
 * Inspired from the website: https://allstarnix.blogspot.de/2013/03/how-to-execute-sql-script-file-using.html
 */

import org.citydb.core.database.connection.DatabaseConnectionPool;
import org.citydb.util.log.Logger;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLScriptRunner {
	private static SQLScriptRunner instance;
    private final String DELIMITER_LINE_REGEX = "(?i)DELIMITER.+"; 
    private final String DELIMITER_LINE_SPLIT_REGEX = "(?i)DELIMITER"; 
    private final String DEFAULT_DELIMITER = ";";
    private final Logger LOG = Logger.getInstance();
    private final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();
    private String delimiter = DEFAULT_DELIMITER;

	@SuppressWarnings("serial")
	private final List<String> DDL_KEY_WORDS = new ArrayList<String>(){{
		add("CREATE TABLE ");
		add("ALTER TABLE ");
		add("DROP TABLE ");
		add("REFERENCES ");
		add("CREATE SEQUENCE ");
		add("DROP SEQUENCE ");	
		add(" ON ");
	}};
	
	private SQLScriptRunner() {}

	public static synchronized SQLScriptRunner getInstance() {
		if (instance == null)
			instance = new SQLScriptRunner();		
		return instance;
	}
	
    public void runScript(String scriptString, Connection connection) throws SQLException {
    	String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
    	StringReader reader = null;
    	LineNumberReader lineReader = null;
    	reader = new StringReader(scriptString);    	
		StringBuffer command = null;
		try {
			lineReader = new LineNumberReader(reader);
			String line = null;
			while ((line = lineReader.readLine()) != null) {
				if (command == null)
					command = new StringBuffer();

				String trimmedLine = line.trim();
				if (trimmedLine.startsWith("--") || trimmedLine.startsWith("//") || trimmedLine.startsWith("#")) {
					if (trimmedLine.indexOf("***") >= 0) {
						LOG.info(trimmedLine);
					}
				} else if (trimmedLine.endsWith(this.delimiter)) {
					// Line is end of statement; 
					Pattern pattern = Pattern.compile(DELIMITER_LINE_REGEX);
					Matcher matcher = pattern.matcher(trimmedLine);
					if (matcher.matches()) {
						delimiter = trimmedLine.split(DELIMITER_LINE_SPLIT_REGEX)[1].trim();
						// New delimiter is processed, continue on next statement
						line = lineReader.readLine();
						if (line == null)
							break;
						trimmedLine = line.trim();
					}

					command.append(line.substring(0, line.lastIndexOf(this.delimiter)));
					command.append(" ");
					if (!(command.toString().toLowerCase().trim().startsWith("create")
							|| command.toString().toLowerCase().trim().startsWith("insert")
							|| command.toString().toLowerCase().trim().startsWith("delete")
							|| command.toString().toLowerCase().trim().startsWith("alter")
							|| command.toString().toLowerCase().trim().startsWith("drop"))) {
						command = null;
					} else {
						Statement stmt = connection.createStatement();
						String commandStr = command.toString();
						// set schema for database objects
						for (String ddlKeyword: DDL_KEY_WORDS) {
							if (commandStr.indexOf(ddlKeyword) >= 0) {
								if (ddlKeyword.equalsIgnoreCase(" ON ") && commandStr.indexOf("ON DELETE") > 0)
									continue;
								commandStr = commandStr.replace(ddlKeyword, ddlKeyword + schema + ".");
							}
						}

						try {
							try {
								stmt.execute(commandStr);
							} catch (SQLException e) {
								throw new SQLException("Error on command: " + command, e);
							}
						} finally {
							if (stmt != null) {
								stmt.close();
							}							
							command = null;
						}
					}
				} else {
					// Line is middle of a statement; 
					Pattern pattern = Pattern.compile(DELIMITER_LINE_REGEX);
					Matcher matcher = pattern.matcher(trimmedLine);
					if (matcher.matches()) {
						delimiter = trimmedLine.split(DELIMITER_LINE_SPLIT_REGEX)[1].trim();
						line = lineReader.readLine();
						if (line == null)
							break;

						trimmedLine = line.trim();
					}
					command.append(line);
					command.append(" ");
				}
			}
		} catch (SQLException e) {
			throw new SQLException("Error on command: " + command, e);
		} catch (IOException e) {
			throw new SQLException("Faild to read the SQL script file", e);
		} finally {
			try {
				reader.close();
				lineReader.close();				
			} catch (IOException e) {
				throw new SQLException(e);
			}				
		}			
    }
    
}