package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.log.Logger;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.registry.DefaultADERegistrationProcessor;
import org.citydb.plugins.ade_manager.registry.datatype.RelationType;
import org.citydb.plugins.ade_manager.registry.metadata.ADEMetadataManager;
import org.citydb.plugins.ade_manager.registry.metadata.AggregationInfo;
import org.citydb.plugins.ade_manager.registry.pkg.delete.DeleteScriptGenerator;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManager;
import org.citydb.plugins.ade_manager.registry.schema.ADEDBSchemaManagerFactory;

public abstract class AbstractDeleteScriptGenerator extends DefaultADERegistrationProcessor implements DeleteScriptGenerator {
	protected final DatabaseConnectionPool dbPool = DatabaseConnectionPool.getInstance();	
	protected final Logger LOG = Logger.getInstance();
	protected final int MAX_FUNCNAME_LENGTH = 30;
	protected final String br = System.lineSeparator();
	protected final String space = " ";
	protected final String brDent1 = br + "  ";
	protected final String brDent2 = brDent1 + "  ";
	protected final String brDent3 = brDent2 + "  ";
	protected final String brDent4 = brDent3 + "  ";
	protected final String brDent5 = brDent4 + "  ";
	
	protected String updateConstraintsSql = "";
	protected Map<String, String> functionNames;
	protected Map<String, String> functionCollection;
	protected Map<QName, AggregationInfo> aggregationInfoCollection;
	protected ConfigImpl config;
	
	protected ADEMetadataManager adeMetadataManager;
	protected ADEDBSchemaManager adeDatabaseSchemaManager;

	public AbstractDeleteScriptGenerator(Connection connection, ConfigImpl config) {
		this.connection = connection;
		this.config = config;
		this.adeMetadataManager = new ADEMetadataManager(connection, config);
		this.adeDatabaseSchemaManager = ADEDBSchemaManagerFactory.getInstance().createADEDatabaseSchemaManager(connection, config);
	}
	
	@Override
	public String generateDeleteScript() throws SQLException {	
		this.functionNames = new HashMap<String, String>();
		this.functionCollection = new HashMap<String, String>();
		try {		
			this.aggregationInfoCollection = adeMetadataManager.queryAggregationInfo();
		} catch (SQLException e) {
			throw new SQLException("Failed to fetch the table aggregation information from 3dcitydb", e);
		} 
		this.generateDeleteScript("cityobject", "citydb");	
		
		return this.printScript();
	}
	
	@Override
	public void installDeleteScript(String scriptString) throws SQLException {	
		CallableStatement cs = null;
		try {
			cs = connection.prepareCall(scriptString);
			cs.execute();
		}
		finally {
			if (cs != null)
				cs.close();
		}		
	}
	
	protected abstract void generateDeleteScript(String initTableName, String schemaName) throws SQLException;		
	protected abstract String constructDeleteFunction(String tableName, String schemaName) throws SQLException;
	
	protected void registerFunction(String tableName, String schemaName) throws SQLException {
		if (!functionCollection.containsKey(tableName)) {
			functionCollection.put(tableName, ""); // dummy
			functionCollection.put(tableName, constructDeleteFunction(tableName, schemaName));
		}			
	}
	
	protected String createFunctionName(String tableName) {
		if (functionNames.containsKey(tableName))
			return functionNames.get(tableName);
		
		String funcName = "delete_" + tableName;
		// TODO use Util's NameShortener class
		if (funcName.length() >= MAX_FUNCNAME_LENGTH)
			funcName = funcName.substring(0, MAX_FUNCNAME_LENGTH);
		
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
	
	private String printScript() {	
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream writer = new PrintStream(os);
		// header text
		writer.println(addSQLComment("Automatically generated 3DcityDB-delete-functions"));
		for (String funcName: functionNames.values()) {
			writer.println("--" + funcName);
		}
		writer.println("------------------------------------------");
		
		// main body containing the function definitions
		for (String tableName: functionCollection.keySet()) {
			writer.println(addSQLComment("Delete function for table: " + tableName.toUpperCase() 
					+ brDent1 + "caller = 0 (default): function is called from neither its parent, nor children tables"
					+ brDent1 + "caller = 1 : function is called from its parent table" 
					+ brDent1 + "caller = 2 : function is called from its children tables" ));
			writer.println(functionCollection.get(tableName));
			writer.println("------------------------------------------");
		};
		
		return os.toString();
	}
	
	private String addSQLComment(String text) {
		StringBuilder builder = new StringBuilder();
		builder.append("/*").append(brDent1).append(text).append(br).append("*/");			
		return builder.toString();
	}

}
