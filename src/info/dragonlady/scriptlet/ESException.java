package info.dragonlady.scriptlet;

/**
 * 最終的に捉える例外クラス
 * @author nobu
 *
 */
public class ESException extends Exception {
	private static final long serialVersionUID = 1415958389721015598L;

	private static String initURL = new String();
	private static String initFullURL = new String();
	/**
	 * {@link java.lang.Exception#Exception(String)}
	 */
	public ESException(String message) {
		super(message);
	}

	/**
	 * {@link java.lang.Exception#Exception(Throwable)}
	 */
	public ESException(Throwable cause) {
		super(cause);
	}

	/**
	 * {@link java.lang.Exception#Exception(String, Throwable)}
	 */
	public ESException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * 呼び出されたサーブレットのパスを設定します。
	 * @param url：サーブレットのパス
	 */
	public void setInitURL(String url) {
		initURL = "."+url;
	}
	
	/**
	 * 例外発生時のサーブレットのパスを応答します。
	 * @return：サーブレットのパス
	 */
	public String getInitURL() {
		return initURL;
	}

	/**
	 * 呼出されたサーブレットのURL（フルパス）を設定します。
	 * @param url：サーブレットのURL
	 */
	public void setInitFullURL(String url) {
		initFullURL = url;
	}
	
	/**
	 * 例外発生時のサーブレットのURL（フルパス）を応答します。
	 * @return：サーブレットのURL
	 */
	public String getInitFullURL() {
		return initFullURL;
	}
}
