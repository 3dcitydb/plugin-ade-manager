package org.citydb.plugins.ade_manager.transformation;

public class TransformationException extends Exception {

	private static final long serialVersionUID = -2280187184046869153L;
	
	public TransformationException() {
		super();
	}
	
	public TransformationException(String message) {
		super(message);
	}
	
	public TransformationException(Throwable cause) {
		super(cause);
	}
	
	public TransformationException(String message, Throwable cause) {
		super(message, cause);
	}
}
