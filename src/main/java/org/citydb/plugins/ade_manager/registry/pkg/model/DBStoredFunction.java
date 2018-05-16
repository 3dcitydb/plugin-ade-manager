package org.citydb.plugins.ade_manager.registry.pkg.model;

public class DBStoredFunction {
	private String name;
	private String declareField;
	private String definition;
	private String annotation;
	private String ownerSchema;

	public DBStoredFunction(String name, String schema) {
		this.name = name;
		this.ownerSchema = schema;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDeclareField() {
		return declareField;
	}

	public void setDeclareField(String declareField) {
		this.declareField = declareField;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	
	public String getOwnerSchema() {
		return ownerSchema;
	}

	public void setOwnerSchema(String ownerSchema) {
		this.ownerSchema = ownerSchema;
	}
}
