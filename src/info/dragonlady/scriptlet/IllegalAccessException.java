package info.dragonlady.scriptlet;

/**
 * シーケンスに従わないアクセスを受信した際に発生する例外
 * @author nobu
 *
 */
public class IllegalAccessException extends Exception {

	private static final long serialVersionUID = -1981324088713929405L;

	public IllegalAccessException(String message) {
		super(message);
	}
	
	public IllegalAccessException(Throwable cause) {
		super(cause);
	}
}
