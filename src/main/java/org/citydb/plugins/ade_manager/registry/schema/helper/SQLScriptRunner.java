/**
 * Inspired from the website: https://allstarnix.blogspot.de/2013/03/how-to-execute-sql-script-file-using.html
 */
package org.citydb.plugins.ade_manager.registry.schema.helper;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.registry.ADERegistrationImpl;

public class SQLScriptRunner extends ADERegistrationImpl {
    private final String DELIMITER_LINE_REGEX = "(?i)DELIMITER.+"; 
    private final String DELIMITER_LINE_SPLIT_REGEX = "(?i)DELIMITER"; 
    private final String DEFAULT_DELIMITER = ";";
    private final int SRID = -1; 
    private final Logger LOG = Logger.getInstance();	
    
    private String delimiter = DEFAULT_DELIMITER;
    
    public SQLScriptRunner(Connection connection) {
    	this.connection = connection;
    }
    
    public void runScript(String scriptString) throws SQLException {
    	runScript(scriptString, SRID);
    }
    
    public void runScript(String scriptString, int srid) throws SQLException {
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
						String subString = "&SRSNO";

						int position = command.lastIndexOf(subString);
						if (position > -1)
							command.replace(position, position + subString.length(), String.valueOf(srid));

						Statement stmt = connection.createStatement();
						try {
							try {
								stmt.execute(command.toString());
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