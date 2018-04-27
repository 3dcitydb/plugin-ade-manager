package org.citydb.plugins.ade_manager.registry.pkg.delete.adapter;

import java.sql.Connection;
import java.sql.SQLException;
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
	protected final String brDent1 = br + "  ";
	protected final String brDent2 = brDent1 + "  ";
	protected final String brDent3 = brDent2 + "  ";
	protected final String brDent4 = brDent3 + "  ";
	protected final String brDent5 = brDent4 + "  ";
	protected final int MAX_FUNCNAME_LENGTH = 30;
	protected final String FUNNAME_PREFIX = "del_";

	protected String updateConstraintsSql = "";
	protected Map<String, String> functionNames;
	protected Map<String, String> functionCollection;
	protected Map<QName, AggregationInfo> aggregationInfoCollection;
	protected final Connection connection;
	protected final ConfigImpl config;
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
		this.functionNames = new TreeMap<String, String>();
		this.functionCollection = new TreeMap<String, String>();
		try {		
			this.aggregationInfoCollection = adeMetadataManager.queryAggregationInfo();
		} catch (SQLException e) {
			throw new SQLException("Failed to fetch the table aggregation information from 3dcitydb", e);
		} 
		String schema = dbPool.getActiveDatabaseAdapter().getConnectionDetails().getSchema();
		this.registerFunction("cityobject", schema);	
		
		return this.printScript();
	}

	protected abstract String constructDeleteFunction(String tableName, String schemaName) throws SQLException;
	protected abstract String printScript();

	protected void registerFunction(String tableName, String schemaName) throws SQLException {
		if (!functionCollection.containsKey(tableName)) {
			functionCollection.put(tableName, ""); // dummy
			functionCollection.put(tableName, constructDeleteFunction(tableName, schemaName));
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

	protected String addSQLComment(String text) {
		StringBuilder builder = new StringBuilder();
		builder.append("/*").append(brDent1).append(text).append(br).append("*/");			
		return builder.toString();
	}

}
