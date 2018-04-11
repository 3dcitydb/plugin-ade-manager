/**
 * Inspired from the website: https://allstarnix.blogspot.de/2013/03/how-to-execute-sql-script-file-using.html
 */
package org.citydb.plugins.ade_manager.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.citydb.log.Logger;

public class SQLRunner {
    private final String DELIMITER_LINE_REGEX = "(?i)DELIMITER.+"; 
    private final String DELIMITER_LINE_SPLIT_REGEX = "(?i)DELIMITER"; 
    private final String DEFAULT_DELIMITER = ";";
    private final boolean AUTOCOMMIT = false;
    private final Logger LOG = Logger.getInstance();	

    private Connection connection;
    private String delimiter = DEFAULT_DELIMITER;

    public SQLRunner(Connection connection) throws SQLRunnerException {
    	if (connection == null)
    		throw new SQLRunnerException("SQL scripts cannot be runned, because database connection is not available.");
    	
        this.connection = connection;
    }

    public void runScript(Reader reader, int srid) throws SQLRunnerException {
		Boolean originalAutoCommit = null;
		
		try {
			originalAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(AUTOCOMMIT);
			
			// Process SQL-command line by line... 
			runScriptLines(reader, srid);
		} catch (SQLException e) {
			throw new SQLRunnerException(e);
		} finally {
			if (originalAutoCommit != null) {
				try {
					connection.setAutoCommit(originalAutoCommit);					
				} catch (SQLException e) {
					throw new SQLRunnerException(e);
				} finally {
					try {
						connection.close();
					} catch (SQLException e) {
						throw new SQLRunnerException(e);
					}
				}
			}	
		}
    }

	private void runScriptLines(Reader reader, int srid) throws SQLRunnerException {
		StringBuffer command = null;

		try {
			LineNumberReader lineReader = new LineNumberReader(reader);
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
								throw new SQLRunnerException("Error on command: " + command, e);
							}
							connection.commit();
						} finally {
							if (stmt != null) {
								try {
									stmt.close();
								} catch (SQLException e) {
									throw new SQLRunnerException("Failed to close statement", e);
								}
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
			throw new SQLRunnerException("Error on command: " + command, e);
		} catch (IOException e) {
			throw new SQLRunnerException("Faild to read the SQL script file", e);
		}
	}
}