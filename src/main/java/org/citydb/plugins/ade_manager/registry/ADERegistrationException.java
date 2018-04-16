package org.citydb.plugins.ade_manager.registry;

public class ADERegistrationException extends Exception {

	private static final long serialVersionUID = -3173169978726674774L;

	public ADERegistrationException() {
		super();
	}
	
	public ADERegistrationException(String message) {
		super(message);
	}
	
	public ADERegistrationException(Throwable cause) {
		super(cause);
	}
	
	public ADERegistrationException(String message, Throwable cause) {
		super(message, cause);
	}
}
