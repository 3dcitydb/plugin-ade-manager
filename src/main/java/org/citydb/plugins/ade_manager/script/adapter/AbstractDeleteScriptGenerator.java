package org.citydb.plugins.ade_manager.script.adapter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.plugins.ade_manager.config.ConfigImpl;
import org.citydb.plugins.ade_manager.script.DsgException;
import org.citydb.plugins.ade_manager.script.IDeleteScriptGenerator;
import org.citydb.plugins.ade_manager.script.RelationType;

public abstract class AbstractDeleteScriptGenerator implements IDeleteScriptGenerator {
	private final int MAX_FUNCNAME_LENGTH = 30;
	protected final String br = System.lineSeparator();
	protected final String space = " ";
	protected final String brDent1 = br + "  ";
	protected final String brDent2 = brDent1 + "  ";
	protected final String brDent3 = brDent2 + "  ";
	protected final String brDent4 = brDent3 + "  ";
	protected final String brDent5 = brDent4 + "  ";
	
	protected String updateConstraintsSql = "";
	protected Map<String, String> deleteFuncNames;
	protected Map<String, String> deleteFuncDefs;

	@SuppressWarnings("serial")
	protected final Map<QName, Boolean> tableAggregationInfo = new HashMap<QName, Boolean>() {
		{
			put(new QName("cityobject", "citymodel"), false);
			put(new QName("external_reference", "cityobject"), true);
			put(new QName("surface_geometry", "cityobject"), true);
			put(new QName("surface_geometry", "surface_geometry"), true);
			put(new QName("surface_geometry", "implict_geometry"), true);
			put(new QName("appearance", "citymodel"), true);
			put(new QName("appearance", "cityobject"), true);
			put(new QName("surface_data", "appearance"), false);
			put(new QName("textureparam", "surface_data"), false);
			put(new QName("tex_image", "surface_data"), false);
			put(new QName("cityobject", "cityobjectgroup"), false);
			put(new QName("implicit_geometry", "cityobjectgroup"), true);
			put(new QName("implicit_geometry", "city_furniture"), false);
			put(new QName("surface_geometry", "city_furniture"), true);
			put(new QName("cityobject_genericattrib", "cityobject_genericattrib"), false);
			put(new QName("cityobject_genericattrib", "cityobject"), true);
			put(new QName("surface_geometry", "cityobject_genericattrib"), true);
			put(new QName("surface_geometry", "generic_cityobject"), true);
			put(new QName("implicit_geometry", "generic_cityobject"), false);
			put(new QName("surface_geometry", "land_use"), false);
			put(new QName("relief_component", "relief_feature"), false);
			put(new QName("surface_geometry", "tin_relief"), true);
			put(new QName("grid_coverage", "raster_relief"), true);
			put(new QName("traffic_area", "transportation_complex"), true);
			put(new QName("surface_geometry", "traffic_area"), true);
			put(new QName("surface_geometry", "transportation_complex"), true);
			put(new QName("surface_geometry", "solitary_vegetat_object"), true);
			put(new QName("implicit_geometry", "solitary_vegetat_object"), false);
			put(new QName("surface_geometry", "plant_cover"), true);
			put(new QName("waterboundary_surface", "waterbody"), false);
			put(new QName("surface_geometry", "waterbody"), true);
			put(new QName("surface_geometry", "waterboundary_surface"), true);
			put(new QName("address", "bridge"), false);
			put(new QName("address", "bridge_opening"), false);
			put(new QName("bridge", "bridge"), true);
			put(new QName("bridge_constr_element", "bridge"), true);
			put(new QName("bridge_furniture", "bridge_room"), true);
			put(new QName("bridge_installation", "bridge_room"), true);
			put(new QName("bridge_installation", "bridge"), true);
			put(new QName("bridge_opening", "bridge_thematic_surface"), true);
			put(new QName("bridge_room", "bridge"), true);
			put(new QName("bridge_thematic_surface", "bridge_room"), true);
			put(new QName("bridge_thematic_surface", "bridge"), true);
			put(new QName("bridge_thematic_surface", "bridge_installation"), true);
			put(new QName("bridge_thematic_surface", "bridge_thematic_surface"), true);
			put(new QName("surface_geometry", "bridge"), true);
			put(new QName("surface_geometry", "bridge_furniture"), true);
			put(new QName("surface_geometry", "bridge_installation"), true);
			put(new QName("surface_geometry", "bridge_opening"), true);
			put(new QName("surface_geometry", "bridge_thematic_surface"), true);
			put(new QName("surface_geometry", "bridge_room"), true);
			put(new QName("surface_geometry", "bridge_constr_element"), true);
			put(new QName("implicit_geometry", "bridge_furniture"), true);
			put(new QName("implicit_geometry", "bridge_installation"), true);
			put(new QName("implicit_geometry", "bridge_opening"), true);
			put(new QName("implicit_geometry", "bridge_constr_element"), true);
			put(new QName("address", "building"), false);
			put(new QName("address", "opening"), false);
			put(new QName("building", "building"), true);
			put(new QName("building_furniture", "room"), true);
			put(new QName("building_installation", "room"), true);
			put(new QName("building_installation", "building"), true);
			put(new QName("opening", "thematic_surface"), false);
			put(new QName("room", "building"), true);
			put(new QName("thematic_surface", "room"), true);
			put(new QName("thematic_surface", "building_installation"), true);
			put(new QName("thematic_surface", "building"), true);
			put(new QName("surface_geometry", "building"), true);
			put(new QName("surface_geometry", "building_furniture"), true);
			put(new QName("surface_geometry", "building_installation"), true);
			put(new QName("surface_geometry", "opening"), true);
			put(new QName("surface_geometry", "thematic_surface"), true);
			put(new QName("surface_geometry", "room"), true);
			put(new QName("implicit_geometry", "building_furniture"), true);
			put(new QName("implicit_geometry", "building_installation"), true);
			put(new QName("implicit_geometry", "opening"), true);
			put(new QName("tunnel", "tunnel"), true);
			put(new QName("tunnel_furniture", "tunnel_hollow_space"), true);
			put(new QName("tunnel_installation", "tunnel_hollow_space"), true);
			put(new QName("tunnel_installation", "tunnel"), true);
			put(new QName("tunnel_opening", "tunnel_thematic_surface"), false);
			put(new QName("tunnel_hollow_space", "tunnel"), true);
			put(new QName("tunnel_thematic_surface", "tunnel_hollow_space"), true);
			put(new QName("tunnel_thematic_surface", "tunnel"), true);
			put(new QName("tunnel_thematic_surface", "tunnel_installation"), true);
			put(new QName("surface_geometry", "tunnel"), true);
			put(new QName("surface_geometry", "tunnel_furniture"), true);
			put(new QName("surface_geometry", "tunnel_installation"), true);
			put(new QName("surface_geometry", "tunnel_opening"), true);
			put(new QName("surface_geometry", "tunnel_thematic_surface"), true);
			put(new QName("surface_geometry", "tunnel_hollow_space"), true);
			put(new QName("implicit_geometry", "tunnel_furniture"), true);
			put(new QName("implicit_geometry", "tunnel_installation"), true);
			put(new QName("implicit_geometry", "tunnel_opening"), true);
		}
	};
	
	
	protected DatabaseConnectionPool dbPool;
		
	@Override
	public void doProcess(DatabaseConnectionPool dbPool, ConfigImpl config) throws DsgException {	
		this.deleteFuncNames = new HashMap<String, String>();
		this.deleteFuncDefs = new HashMap<String, String>();
		this.dbPool = dbPool;

		generateDeleteFuncs("cityobject", "citydb");
		
		writeToFile(new File(config.getTransformationOutputPath() + File.separator + "3dcitydb-delete-script.sql"));
	}
	
	protected abstract void generateDeleteFuncs(String initTableName, String schemaName) throws DsgException;
		
	protected abstract String create_delete_function(String tableName, String schemaName) throws SQLException;
	
	protected String buildComment(String text) {
		{
			StringBuilder builder = new StringBuilder();
			builder.append("/*").append(brDent1).append(text).append(br).append("*/");			
			return builder.toString();
	    }
	}
	
	protected void createDeleteFunc(String tableName, String schemaName) throws SQLException {
		if (!deleteFuncDefs.containsKey(tableName)) {
			deleteFuncDefs.put(tableName, ""); // dummy
			deleteFuncDefs.put(tableName, create_delete_function(tableName, schemaName));
		}			
	}
	
	protected String getFuncName(String tableName) {
		if (deleteFuncNames.containsKey(tableName))
			return deleteFuncNames.get(tableName);
		
		String funcName = "delete_" + tableName;
		if (funcName.length() >= MAX_FUNCNAME_LENGTH)
			funcName = funcName.substring(0, MAX_FUNCNAME_LENGTH);
		
		deleteFuncNames.put(tableName, funcName);
		
		return funcName;
	}
	
	protected RelationType checkTableRelation(String childTable, String parentTable) {
		QName key = new QName(childTable, parentTable);
		if (tableAggregationInfo.containsKey(key)) {
			boolean isComposite = tableAggregationInfo.get(key);
			if (isComposite)
				return RelationType.composition;
			else
				return RelationType.aggregation;
		} 			
		else {
			return RelationType.no_agg_comp;
		} 			
	}	
	
	private void writeToFile(File outputFile) throws DsgException {
		PrintWriter writer = null;
		try {
			// header part
			writer = new PrintWriter(outputFile);		
			writer.println(buildComment("Automatically generated 3DcityDB-delete-functions"));
			for (String funcName: deleteFuncNames.values()) {
				writer.println("--" + funcName);
				System.out.println(funcName);
			}
			writer.println("------------------------------------------");
			// function definition part
			for (String tableName: deleteFuncDefs.keySet()) {
				writer.println(buildComment("Delete function for table: " + tableName.toUpperCase() 
						+ brDent1 + "caller = 0 (default): function is called from neither its parent, nor children tables"
						+ brDent1 + "caller = 1 : function is called from its parent table" 
						+ brDent1 + "caller = 2 : function is called from its children tables" ));
				writer.println(deleteFuncDefs.get(tableName));
				writer.println("------------------------------------------");
			} 
		} catch (IOException e) {
			throw new DsgException("Failed to open file '" + outputFile.getName() + "' for writing.", e);
		} finally {
			if (writer != null)
				writer.close();				
		}	
	}
	
	protected class ReferencingEntry {
		private String refColumn;
		private String refTable;
		
		public ReferencingEntry(String refTable, String refColumn) {
			this.refTable = refTable;
			this.refColumn = refColumn;
		}
		
		public String getRefColumn() {
			return refColumn;
		}

		public void setRefColumn(String refColumn) {
			this.refColumn = refColumn;
		}

		public String getRefTable() {
			return refTable;
		}

		public void setRefTable(String refTable) {
			this.refTable = refTable;
		}		
	}
	
	protected class ReferencedEntry {
		private String refTable;
		private String refColumn;
		private String[] fkColumns;

		public ReferencedEntry(String refTable, String refColumn, String[] fkColumns) {
			this.refTable = refTable;
			this.refColumn = refColumn;
			this.fkColumns = fkColumns;
		}
		
		public String getRefTable() {
			return refTable;
		}

		public void setRefTable(String refTable) {
			this.refTable = refTable;
		}

		public String getRefColumn() {
			return refColumn;
		}

		public void setRefColumn(String refColumn) {
			this.refColumn = refColumn;
		}

		public String[] getFkColumns() {
			return fkColumns;
		}

		public void setFkColumns(String[] fkColumns) {
			this.fkColumns = fkColumns;
		}
	}

	protected class MnRefEntry {
		private String rootTableName;
		private String nTableName;
		private String nFkColumnName;
		private String nFkName;
		private String mTableName;
		private String mFkColumnName;
		private String mRefColumnName;
		private String mFkName;
		private boolean nColIsNotNull;

		public boolean isnColIsNotNull() {
			return nColIsNotNull;
		}

		public void setnColIsNotNull(boolean nColIsNotNull) {
			this.nColIsNotNull = nColIsNotNull;
		}

		public MnRefEntry() {}

		public String getRootTableName() {
			return rootTableName;
		}

		public void setRootTableName(String rootTableName) {
			this.rootTableName = rootTableName;
		}

		public String getnTableName() {
			return nTableName;
		}

		public void setnTableName(String nTableName) {
			this.nTableName = nTableName;
		}

		public String getnFkColumnName() {
			return nFkColumnName;
		}

		public void setnFkColumnName(String nFkColumnName) {
			this.nFkColumnName = nFkColumnName;
		}

		public String getmTableName() {
			return mTableName;
		}

		public void setmTableName(String mTableName) {
			this.mTableName = mTableName;
		}

		public String getmFkColumnName() {
			return mFkColumnName;
		}

		public void setmFkColumnName(String mFkColumnName) {
			this.mFkColumnName = mFkColumnName;
		}

		public String getmRefColumnName() {
			return mRefColumnName;
		}

		public void setmRefColumnName(String mRefColumnName) {
			this.mRefColumnName = mRefColumnName;
		}

		public String getnFkName() {
			return nFkName;
		}

		public void setnFkName(String nFkName) {
			this.nFkName = nFkName;
		}

		public String getmFkName() {
			return mFkName;
		}

		public void setmFkName(String mFkName) {
			this.mFkName = mFkName;
		}

	}
	
}
