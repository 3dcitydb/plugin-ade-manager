package org.citydb.plugins.ade_manager.registry.datatype;

public class MnRefEntry {
	private String rootTableName;
	private String nTableName;
	private String nFkColumnName;
	private String nFkName;
	private String mTableName;
	private String mFkColumnName;
	private String mRefColumnName;
	private String mFkName;
	private boolean nColIsNotNull;

	public MnRefEntry() {}

	public boolean isnColIsNotNull() {
		return nColIsNotNull;
	}

	public void setnColIsNotNull(boolean nColIsNotNull) {
		this.nColIsNotNull = nColIsNotNull;
	}

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