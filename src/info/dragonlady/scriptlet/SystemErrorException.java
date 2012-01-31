package info.dragonlady.scriptlet;

/**
 * セットアップエラー時に発生する例外
 * @author nobu
 *
 */
public class SystemErrorException extends Exception {

	private static final long serialVersionUID = -5130813176177159627L;

	public SystemErrorException(String message) {
		super(message);
	}
	
	public SystemErrorException(Throwable cause) {
		super(cause);
	}
}
