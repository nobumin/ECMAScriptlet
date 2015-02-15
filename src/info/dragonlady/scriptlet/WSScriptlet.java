package info.dragonlady.scriptlet;

import info.dragonlady.scriptlet.Scriptlet.MongoDBCall;
import info.dragonlady.scriptlet.Scriptlet.SQLiteCall;
import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.MongoDBAccesser;
import info.dragonlady.websocket.WebsocketServer;

import java.io.Serializable;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.websocket.Session;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public abstract class WSScriptlet implements Serializable {
	public enum WS_STATUS {
		OPEN,
		CLOSE,
		ERROR,
		EXEC,
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -3174178825779239961L;

	public static final String DEFAULT_CHARSET = "utf-8";
	public static final String DEFAULT_CONTENT_TYPE = "text/html";
	
	protected WebsocketServer wsScriptlet = null;
	protected String charset = DEFAULT_CHARSET;
	protected String contentType = DEFAULT_CONTENT_TYPE;

	/**
	 * WebSocketScriptletから呼ばれる関数
	 */
	public void setServlet(WebsocketServer server) {
		wsScriptlet = server;
	}

	/**
	 * WebSocketScriptletの同名ラッパー関数
	 * @return
	 */
	public DBAccesser getDBAccessObject() {
		return wsScriptlet.getDBAccessObject();
	}
	
	public MongoDBAccesser getMongoDBAccesser() {
		return wsScriptlet.getMongoDBAccesser();
	}

	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public String getServletName() {
		return "WebsocketServer";
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public ServletContext getServletContext() {
		return null;
	}
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public String getScriptletPath(Session session) {
		return wsScriptlet.getScriptletPath(session);
	}
	/**
	 * 
	 * @param session
	 * @return
	 */
	public String getScriptletPath(ServletContext context) {
		return wsScriptlet.getScriptletPath(context);
	}

	/**
	 * SQLへのアクセスをJSONで行うためのクラス
	 * @return
	 */
	public SQLiteCall getSqlDbWithJSon() {
		return new SQLiteCall();
	}
	
	/**
	 * MongoDBへのアクセスをJSONで行うためのクラス
	 * @return
	 */
	public MongoDBCall getMongoDbWithJSon() {
		return new MongoDBCall();
	}

	/**
	 * 文字コードを変更します。
	 * @param code
	 */
	public void setCharSet(String code) {
		charset = code;
		wsScriptlet.setCharacterEncoding(charset);
	}
	
	/**
	 * HttpServletResponse#setContentTypeに指定する、<br>
	 * CharSetの値を応答します。<br>
	 * デフォルトはutf-8です。変更したい場合はオーバーライド
	 * @return：文字コードの文字列（IANA）
	 */
	protected String getCharSet() {
		return charset;
	}
	
	/**
	 * WebSocketScriptletの同名ラッパー関数
	 * @return
	 */
	public String getScriptExtName() {
		return wsScriptlet.getScriptExtName();
	}
	
	/**
	 * mime/typeを取得します。
	 * @return
	 */
	public String getContentTypeValue() {
		return getContentType() + "; charset=" + getCharSet();
	}

	/**
	 * コンテントタイプを変更します。（デフォルトはtext/html）
	 * @param type
	 */
	public void setContentType(String type) {
		contentType = type;
	}
	
	/**
	 * HttpServletResponse#setContentTypeに指定する、<br>
	 * ContentTypeの値を応答します。<br>
	 * デフォルトはtext/htmlです。変更したい場合はオーバーライド
	 * @return：コンテントタイプの文字列
	 */
	public String getContentType() {
		return contentType;
	}
	
	/**
	 * GenericServletの同名のラッパー関数
	 * @param msg
	 */
	public void log(String msg) {
		wsScriptlet.log(msg);
	}
	
	/**
	 * GenericServletの同名のラッパー関数
	 * @param message
	 * @param t
	 */
	public void log(String message, Throwable t) {
		wsScriptlet.log(message, t);
	}
	
	/**
	 * JavaScriptの無名関数を処理するためのヘルパーメソッド
	 * @param function:引数に指定された関数を指定
	 * @param args:無名関数へ渡す引数の配列
	 * @return
	 */
	public Object execAnonymousFunction(Object function, Vector<Object> args) {
		ContextFactory cxFactory = new ContextFactory();
		Context cx = cxFactory.enterContext();
		Scriptable scope = cx.initStandardObjects();
		org.mozilla.javascript.BaseFunction anonymousFunc = (org.mozilla.javascript.BaseFunction)function;
		return anonymousFunc.call(cx, scope, anonymousFunc, args.toArray());
	}

	/**
	 * SevureServletより呼ばれる起動メソッド
	 * 継承クラスで実装します。
	 * @param path
	 * @param session
	 * @param wsStatus
	 * @throws SystemErrorException
	 */
	abstract public void start(String path, Session session, WSScriptlet.WS_STATUS wsStatus) throws SystemErrorException;

	/**
	 * サーバサイドスクリプト内で利用する、複数のグローバルオブジェクトを応答します。<br>
	 * KEY:サーバサイドスクリプト内グローバルオブジェクト名<br>
	 * VALUE:グローバルオブジェクトに関連づけるインスタンス<br>
	 * ※予約されたグローバルオブジェクト名は利用しないこと。以下予約オブジェクト名<br>
	 * request：javax.servlet.http.HttpServletRequestのインスタンス<br>
	 * response：javax.servlet.http.HttpServletResponseのインスタンス<br>
	 * session：javax.servlet.http.HttpSessionのインスタンス<br>
	 * serverout：javax.servlet.http.HttpServletResponse#getWriterの戻り値（java.io.PrintWrite）<br>
	 * sysout：System.out<br>
	 * syserr：System.err<br>
	 * helper：info.dragonlady.scriptlet.ESCylinder.ESHelperのインスタンス<br>
	 * exception：例外発生時の例外オブジェクト
	 * @return：グローバルオブジェクト名-インスタンスのMap
	 */
	abstract public Map<String, Object> getScriptNewProperties();
	
	/**
	 * 必須パラメータが記述されたいなかった際のメッセージ本文を応答する。
	 * @return：必須パラメータエラーメッセージ
	 */
	abstract public String getRequiredParamErrorMsg();
	
	/**
	 * パラメータの検証によるエラーが発せした際のメッセージ本文を応答する。
	 * @return：パラメータ検証エラーメッセージ
	 */
	abstract public String getInvalidParamErrorMsg();
	
	/**
	 * サーバサイドスクリプトでエラーが発生した際のメッセージ本文を応答する。
	 * @return：スクリプトエラーメッセージ
	 */
	abstract public String getEScriptErrorMsg();
	
	/**
	 * パラメータ検証ルール記述にエラーがあった際のメッセージ本文を応答する。
	 * @return：検証ルール記述エラーメッセージ
	 */
	abstract public String getInvalidValidationParamErrorMsg();

	/**
	 * serialVersionUIDを応答する仮想関数
	 * @return
	 */
	abstract public long getSerialVersionUID();

}
