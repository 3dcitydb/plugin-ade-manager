package org.citydb.plugins.ade_manager.registry.pkg;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.metadata.AggregationInfo;
import org.citydb.plugins.ade_manager.registry.pkg.DBStoredFunction;
import org.citydb.plugins.ade_manager.registry.pkg.DBScriptGenerator;
import org.citydb.plugins.ade_manager.registry.query.Querier;
import org.citydb.plugins.ade_manager.registry.query.datatype.RelationType;

public abstract class DefaultDBScriptGenerator implements DBScriptGenerator {
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();	
	protected final Logger LOG = Logger.getInstance();	
	protected final String br = System.lineSeparator();
	protected final String space = " ";
	protected final String dent = "  ";
	protected final String brDent1 = br + dent;
	protected final String brDent2 = brDent1 + dent;
	protected final String brDent3 = brDent2 + dent;
	protected final String brDent4 = brDent3 + dent;
	protected final String brDent5 = brDent4 + dent;
	protected final String brDent6 = brDent5 + dent;
	protected final int MAX_FUNCNAME_LENGTH = 30;

	protected Map<String, DBStoredFunction> functionCollection;
	protected Map<QName, AggregationInfo> aggregationInfoCollection;
	protected final Connection connection;
	protected final ConfigImpl config;
	protected final String defaultSchema = dbPool.getActiveDatabaseAdapter().getSchemaManager().getDefaultSchema();
	protected ADEMetadataManager adeMetadataManager;
	protected Querier querier;

	public DefaultDBScriptGenerator(Connection connection, ConfigImpl config) {
		this.connection = connection;
		this.config = config;
		this.functionCollection = new TreeMap<String, DBStoredFunction>();
		this.adeMetadataManager = new ADEMetadataManager(connection, config);
		this.querier = new Querier(connection);
		try {		
			this.aggregationInfoCollection = adeMetadataManager.queryAggregationInfo();
		} catch (SQLException e) {
			LOG.error("Failed to fetch the table aggregation information from 3dcitydb");
		} 	
	}
	
	@Override
	public String generateScript() throws SQLException {	
		functionCollection.clear();
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();			
		registerFunctions(schema);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream writer = new PrintStream(os);		
		writer.println(sqlComment("Automatically generated database-stored functions (Creation Date: "
				+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ")"));

		for (String funcName: functionCollection.keySet()) 
			writer.println(sqlComment(funcName));
		writer.println("------------------------------------------" + br);				
		printFunctionsDDL(writer);
		
		return os.toString();
	}

	protected abstract void registerFunctions(String schemaName) throws SQLException;
	protected abstract void printFunctionsDDL(PrintStream writer);	
	protected abstract String createFunctionName(String tableName);

	protected RelationType checkTableRelationType(String childTable, String parentTable) {
		QName key = new QName(childTable, parentTable);
		if (aggregationInfoCollection.containsKey(key)) {
			boolean isComposite = aggregationInfoCollection.get(key).isComposite();
			if (isComposite)
				return RelationType.composition;
			else
				return RelationType.aggregation;
		} 			
		else {
			return RelationType.no_agg_comp;
		} 			
	}	

	protected String wrapSchemaName(String entryName, String schemaName) {
		return schemaName + "." + entryName;
	}
	
	protected String sqlComment(String text) {
		return "-- " + text;
	}	 

}
