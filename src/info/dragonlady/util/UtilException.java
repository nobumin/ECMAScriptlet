package info.dragonlady.util;

/**
 * セットアップエラー時に発生する例外
 * @author nobu
 *
 */
public class UtilException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2769942256055568362L;

	public UtilException(String message) {
		super(message);
	}
	
	public UtilException(Throwable cause) {
		super(cause);
	}
}
