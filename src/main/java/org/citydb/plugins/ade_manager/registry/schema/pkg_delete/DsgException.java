package org.citydb.plugins.ade_manager.registry.schema.pkg_delete;

public class DsgException extends Exception {

	private static final long serialVersionUID = -1115965491412100047L;

	public DsgException() {
		super();
	}
	
	public DsgException(String message) {
		super(message);
	}
	
	public DsgException(Throwable cause) {
		super(cause);
	}
	
	public DsgException(String message, Throwable cause) {
		super(message, cause);
	}
}
