package org.citydb.plugins.ade_manager.util;

public class SQLRunnerException extends Exception {

	private static final long serialVersionUID = -6236151924939406423L;

	public SQLRunnerException() {
		super();
	}
	
	public SQLRunnerException(String message) {
		super(message);
	}
	
	public SQLRunnerException(Throwable cause) {
		super(cause);
	}
	
	public SQLRunnerException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
