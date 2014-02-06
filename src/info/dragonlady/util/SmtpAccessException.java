package info.dragonlady.util;

public class SmtpAccessException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1322917596398176218L;

	public SmtpAccessException(String message) {
		super(message);
	}
	
	public SmtpAccessException(Throwable cause) {
		super(cause);
	}
}
