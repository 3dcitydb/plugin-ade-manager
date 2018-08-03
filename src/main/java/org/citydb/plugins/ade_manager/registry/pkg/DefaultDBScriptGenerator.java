package org.citydb.plugins.ade_manager.registry.pkg;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.metadata.AggregationInfoCollection;
import org.citydb.plugins.ade_manager.registry.model.DBSQLScript;
import org.citydb.plugins.ade_manager.registry.model.DBStoredFunctionCollection;
import org.citydb.plugins.ade_manager.registry.pkg.DBScriptGenerator;
import org.citydb.plugins.ade_manager.registry.query.Querier;

public abstract class DefaultDBScriptGenerator implements DBScriptGenerator {
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();	
	protected final Logger LOG = Logger.getInstance();	
	protected final String br = System.lineSeparator();
	protected final String commentPrefix = "-- ";
	protected final String separatorLine = "------------------------------------------";
	protected final String dent = "  ";
	protected final String brDent1 = br + dent;
	protected final String brDent2 = brDent1 + dent;
	protected final String brDent3 = brDent2 + dent;
	protected final String brDent4 = brDent3 + dent;
	protected final String brDent5 = brDent4 + dent;
	protected final String brDent6 = brDent5 + dent;
	protected final String brDent7 = brDent6 + dent;
	protected final int MAX_FUNCNAME_LENGTH = 30;

	protected DBStoredFunctionCollection functionCollection;
	protected AggregationInfoCollection aggregationInfoCollection;
	protected final Connection connection;
	protected final ConfigImpl config;
	protected final String defaultSchema = dbPool.getActiveDatabaseAdapter().getSchemaManager().getDefaultSchema();
	protected ADEMetadataManager adeMetadataManager;
	protected Querier querier;

	public DefaultDBScriptGenerator(Connection connection, ConfigImpl config, ADEMetadataManager adeMetadataManager) {
		this.connection = connection;
		this.config = config;
		this.functionCollection = new DBStoredFunctionCollection();		
		this.querier = new Querier(connection);
		this.adeMetadataManager = adeMetadataManager;
		this.aggregationInfoCollection = adeMetadataManager.getAggregationInfoCollection();	
	}
	
	public DBSQLScript generateDBScript() throws SQLException {
		functionCollection.clear();
		String schemaName = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();		
		DBSQLScript script = generateScript(schemaName);	
		
		// create script header text
		StringBuilder builder = new StringBuilder();
		builder.append(commentPrefix).append("Automatically generated database script ")
									 .append("(Creation Date: ")
									 .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
									 .append(")").append(br);
		builder.append(functionCollection.printFunctionNameList(commentPrefix));
		
		script.setHeaderText(builder.toString());
		
		return script;
	}
	
	protected abstract DBSQLScript generateScript(String schemaName) throws SQLException;
	
	protected String wrapSchemaName(String entryName, String schemaName) {
		return schemaName + "." + entryName;
	}

	protected boolean tableExists(String tableName, String schemaName) throws SQLException {
		boolean exist = false;
		Statement stmt = null;
		ResultSet rs = null;
		try {					
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select 1 from ALL_TABLES "
					 + "where TABLE_NAME = upper('" + tableName + "') "
					 + "and OWNER = upper('" + schemaName + "')");		
			if (rs.next()) 
				exist = true;	
		} finally {
			if (rs != null) 
				rs.close();
	
			if (stmt != null) 
				stmt.close();
		}

		return exist;
	}
}
