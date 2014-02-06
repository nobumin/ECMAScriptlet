package info.dragonlady.util;

public class SmtpException extends Exception{

	private static final long serialVersionUID = 2557175517109386614L;

	public SmtpException(String message) {
		super(message);
	}
	
	public SmtpException(Throwable cause) {
		super(cause);
	}
}
