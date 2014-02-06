package info.dragonlady.scriptlet.mail;

public class MailReceivedException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 0L;

	public MailReceivedException(String message) {
		super(message);
	}
	
	public MailReceivedException(Throwable cause) {
		super(cause);
	}
}
