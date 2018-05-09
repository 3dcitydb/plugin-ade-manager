package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter;

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
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.query.Querier;
import org.citydb.plugins.ade_manager.registry.query.datatype.RelationType;

public abstract class AbstractDeleteScriptGenerator implements DeleteScriptGenerator {
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
	protected final String FUNNAME_PREFIX = "del_";
	protected final String lineage_delete_funcname = "del_cityobject_by_lineage";
	protected final String appearance_cleanup_funcname = "cleanup_global_appearances";
	
	protected String updateConstraintsSql = "";
	protected Map<String, String> functionNames;
	protected Map<String, String> functionCollection;
	protected Map<QName, AggregationInfo> aggregationInfoCollection;
	protected final Connection connection;
	protected final ConfigImpl config;
	protected final String defaultSchema = dbPool.getActiveDatabaseAdapter().getSchemaManager().getDefaultSchema();
	protected ADEMetadataManager adeMetadataManager;
	protected Querier querier;

	public AbstractDeleteScriptGenerator(Connection connection, ConfigImpl config) {
		this.connection = connection;
		this.config = config;
		this.adeMetadataManager = new ADEMetadataManager(connection, config);
		this.querier = new Querier(connection);
	}
	
	@Override
	public String generateDeleteScript() throws SQLException {			
		try {		
			this.aggregationInfoCollection = adeMetadataManager.queryAggregationInfo();
		} catch (SQLException e) {
			throw new SQLException("Failed to fetch the table aggregation information from 3dcitydb", e);
		} 
		this.functionNames = new TreeMap<String, String>();
		this.functionCollection = new TreeMap<String, String>();
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();	
		this.registerFunction("cityobject", schema);	

		return this.printDeleteScript();
	}
	
	protected abstract String constructLineageDeleteFunction(String schemaName);
	protected abstract String constructAppearanceCleanupFunction(String schemaName);
	protected abstract String constructDeleteFunction(String tableName, String schemaName) throws SQLException;
	protected abstract void printDDLForAllDeleteFunctions(PrintStream writer);

	protected String printDeleteScript() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream writer = new PrintStream(os);
		
		writer.println(sqlComment("Automatically generated 3DcityDB-delete-functions (Creation Date: "
				+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ")"));
		for (String funcName: functionNames.values()) {
			writer.println(sqlComment(funcName));
		}
		writer.println("------------------------------------------" + br);		
		printDDLForAllDeleteFunctions(writer);
		
		return os.toString();
	};
	
	protected void registerFunction(String tableName, String schemaName) throws SQLException {
		if (!functionCollection.containsKey(tableName)) {			
			functionCollection.put(tableName, ""); 
			functionCollection.put(tableName, constructDeleteFunction(tableName, schemaName));
			LOG.info("Function '" + createFunctionName(tableName) + "' created." );
			
			// register and create lineage delete function
			if (tableName.equalsIgnoreCase("cityobject")) {
				functionNames.put(lineage_delete_funcname, lineage_delete_funcname);
				functionCollection.put(lineage_delete_funcname, constructLineageDeleteFunction(schemaName));
				LOG.info("Function '" + lineage_delete_funcname + "' created." );				
			}
			
			// register and create cleanup-function for global appearances
			if (tableName.equalsIgnoreCase("appearance")) {
				functionNames.put(appearance_cleanup_funcname, appearance_cleanup_funcname);
				functionCollection.put(appearance_cleanup_funcname, constructAppearanceCleanupFunction(schemaName));
				LOG.info("Function '" + appearance_cleanup_funcname + "' created." );				
			}
		}			
	}
	
	protected String createFunctionName(String tableName) {
		if (functionNames.containsKey(tableName))
			return functionNames.get(tableName);
		
		String funcName = FUNNAME_PREFIX + tableName;
		functionNames.put(tableName, funcName);
		
		return funcName;
	}

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
		if (schemaName.equalsIgnoreCase(defaultSchema))
			return entryName;
		else
			return schemaName + "." + entryName;
	}
	protected String sqlComment(String text) {
		return "-- " + text;
	}	 

}
