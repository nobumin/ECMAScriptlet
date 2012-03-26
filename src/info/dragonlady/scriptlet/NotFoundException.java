package info.dragonlady.scriptlet;

/**
 * スクリプトファイルが削除された際に発生する例外
 * @author nobu
 *
 */
public class NotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6003492954478737759L;

	public NotFoundException(String message) {
		super(message);
	}
	
	public NotFoundException(Throwable cause) {
		super(cause);
	}
}
