
package org.citydb.plugins.ade_manager.registry.metadata;

public class ADEMetadataEntity {	
	private String adeid;
	private String name;
	private String description;
	private String version;
	private String dbPrefix;

	public ADEMetadataEntity(String adeid, String name, String description, String version, String dbPrefix) {
		this.adeid = adeid;
		this.name = name;
		this.description = description;
		this.version = version;
		this.dbPrefix = dbPrefix;
	}
	
	public String getAdeid() {
		return adeid;
	}

	public void setAdeid(String adeid) {
		this.adeid = adeid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDbPrefix() {
		return dbPrefix;
	}

	public void setDbPrefix(String dbPrefix) {
		this.dbPrefix = dbPrefix;
	}

}
