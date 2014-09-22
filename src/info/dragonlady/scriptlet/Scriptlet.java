package info.dragonlady.scriptlet;

import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.MongoDBAccesser;
import info.dragonlady.util.UtilException;
import info.dragonlady.util.DBAccesser.DBStatementParam;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * 独自の処理を行い、サーバサイドスクリプトを実行する際の基底クラスです。
 * @author nobu
 *
 */
abstract public class Scriptlet implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7690907011797395682L;
	public static final String DEFAULT_CHARSET = "utf-8";
	public static final String DEFAULT_CONTENT_TYPE = "text/html";


	/**
	 * SQLラッピングクラス
	 * @author nobu
	 *
	 */
	static public class SQLiteCall
	{
		private DBStatementParam[] createStatementParam(DBAccesser dba, String[] jsonList) throws JSONException {
			DBStatementParam[] params = null;
			if(jsonList != null && jsonList.length > 0) {
				params = new DBStatementParam[jsonList.length];
				for(int i=0;i<jsonList.length;i++) {
					String jsonParam = jsonList[i];
					JSONObject param = new JSONObject(jsonParam);
					String type = param.getString("type");
					String value = param.getString("value");
					if(type.toUpperCase().equals("INT")) {
						params[i] = dba.createIntDBParam(Integer.parseInt(value));
					}else if(type.toUpperCase().equals("STRING")) {
						params[i] = dba.createStringDBParam(value);
					}else if(type.toUpperCase().equals("BOOL")) {
						params[i] = dba.createBooleanDBParam(Boolean.parseBoolean(value));
					}else if(type.toUpperCase().equals("LONG")) {
						params[i] = dba.createLongDBParam(Long.parseLong(value));
					}else if(type.toUpperCase().equals("FLOAT")) {
						params[i] = dba.createFloatDBParam(Float.parseFloat(value));
					}else if(type.toUpperCase().equals("DOUBLE")) {
						params[i] = dba.createDoubleDBParam(Double.parseDouble(value));
					}else if(type.toUpperCase().equals("DATE")) {
						params[i] = dba.createDateDBParam(value);
					}else if(type.toUpperCase().equals("TIMESTAMP")) {
						params[i] = dba.createTimestampDBParam(value);
					}else{
						params[i] = dba.createStringDBParam(value);
					}
				}
			}
			return params;
		}
		/**
		 * 
		 * @param dba
		 * @param sqlName
		 * @param jsonList
		 * @return
		 * @throws UtilException
		 */
		public String execSelectSQL(DBAccesser dba, String sqlName, String[] jsonList) throws UtilException {
			JSONArray jarray = new JSONArray();
			Connection con = null;
			try {
				DBStatementParam[] params = createStatementParam(dba, jsonList);
				con = dba.getConnection();
				con.setAutoCommit(false);
				Vector<HashMap<String, Object>>resultSet = dba.selectQuery(sqlName, con, params);
				for(HashMap<String, Object> record : resultSet) {
					JSONObject json = new JSONObject();
					for(Map.Entry<String, Object>entry : record.entrySet()) {
						String key = entry.getKey();
						Object val = entry.getValue();
						json.put(key, val);
					}
					jarray.put(json);
				}
			}
			catch(UtilException e) {
				throw e;
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				if(con != null) {
					try {
						con.close();
					}
					catch(Exception e) {
						//NOP
					}
				}
			}
			
			return jarray.toString();
		}
		/**
		 * 
		 * @param dba
		 * @param sqlName
		 * @param jsonList
		 * @return
		 * @throws UtilException
		 */
		public boolean execUpdateSQL(DBAccesser dba, String sqlName, String[] jsonList) throws UtilException {
			Connection con = null;
			boolean result = false;
			try {
				DBStatementParam[] params = createStatementParam(dba, jsonList);
				con = dba.getConnection();
				con.setAutoCommit(false);
				int count = dba.updateQuery(sqlName, con, params);
				if(count >= 0) {
					con.commit();
					result = true;
				}
			}
			catch(UtilException e) {
				throw e;
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				if(con != null) {
					try {
						if(!result) {
							try {
								con.rollback();
							}
							catch(Exception e) {
								//
							}
						}
						con.close();
					}
					catch(Exception e) {
						//NOP
					}
				}
			}
			return result;
		}
		/**
		 * 
		 * @param dba
		 * @param con
		 * @param sqlName
		 * @param jsonList
		 * @return
		 * @throws UtilException
		 */
		public Connection execUpdateSQL(DBAccesser dba, Connection con, String sqlName, String[] jsonList) throws UtilException {
			Connection conn = null;
			boolean result = false;
			try {
				if(con == null) {
					conn = dba.getConnection();
				}
				DBStatementParam[] params = createStatementParam(dba, jsonList);
				conn = dba.getConnection();
				int count = dba.updateQuery(sqlName, conn, params);
				if(count >= 0) {
					result = true;
				}
			}
			catch(UtilException e) {
				throw e;
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				if(conn != null) {
					try {
						if(!result) {
							try {
								conn.rollback();
							}
							catch(Exception e) {
								//
							}
						}
						conn.close();
					}
					catch(Exception e) {
						//NOP
					}
				}
			}
			return conn;
		}
	}
	
	/**
	 * mongoDBアクセス ラッピングクラス
	 * @author nobu
	 *
	 */
	static public class MongoDBCall {
		/**
		 * 
		 * @param mongodb
		 * @param colectionName
		 * @param queryJson
		 * @return
		 * @throws UtilException
		 */
		public String findDB(MongoDBAccesser mongodb, String colectionName, String queryJson) throws UtilException {
			StringBuffer result = new StringBuffer();
			result.append("[");
			try {
				mongodb.open();
				DBCollection collection = mongodb.getCollection(colectionName);
				DBObject findObj = (DBObject)JSON.parse(queryJson);
				DBCursor cursor = collection.find(findObj);
				while(cursor.hasNext()) {
					if(result.length() > 1) {
						result.append(",");
					}
					result.append(cursor.next().toString());
				}
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				mongodb.close();
			}
			result.append("]");
			return result.toString();
		}
		
		/**
		 * 
		 * @param mongodb
		 * @param colectionName
		 * @param queryJson
		 * @param sortJson
		 * @return
		 * @throws UtilException
		 */
		public String findDBWithSort(MongoDBAccesser mongodb, String colectionName, String queryJson, String sortJson) throws UtilException {
			StringBuffer result = new StringBuffer();
			result.append("[");
			try {
				mongodb.open();
				DBCollection collection = mongodb.getCollection(colectionName);
				DBObject findObj = (DBObject)JSON.parse(queryJson);
				DBObject sortObj = (DBObject)JSON.parse(sortJson);
				DBCursor cursor = collection.find(findObj).sort(sortObj);
				while(cursor.hasNext()) {
					if(result.length() > 1) {
						result.append(",");
					}
					result.append(cursor.next().toString());
				}
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				mongodb.close();
			}
			result.append("]");
			return result.toString();
		}

		/**
		 * 
		 * @param mongodb
		 * @param colectionName
		 * @param insertJson
		 * @throws UtilException
		 */
		public void insertDB(MongoDBAccesser mongodb, String colectionName, String insertJson) throws UtilException {
			try {
				mongodb.open();
				DBCollection collection = mongodb.getCollection(colectionName);
				DBObject insertObj = (DBObject)JSON.parse(insertJson);
				collection.insert(insertObj);
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				mongodb.close();
			}
		}

		/**
		 * 
		 * @param mongodb
		 * @param colectionName
		 * @param queryJson
		 * @param updateJson
		 * @throws UtilException
		 */
		public void updateDB(MongoDBAccesser mongodb, String colectionName, String queryJson, String updateJson) throws UtilException {
			try {
				mongodb.open();
				DBCollection collection = mongodb.getCollection(colectionName);
				DBObject findObj = (DBObject)JSON.parse(queryJson);
				DBObject updateObj = (DBObject)JSON.parse(updateJson);
				collection.update(findObj, updateObj);
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				mongodb.close();
			}
		}
		/**
		 * 
		 * @param mongodb
		 * @param colectionName
		 * @param removeJson
		 * @throws UtilException
		 */
		public void removeDB(MongoDBAccesser mongodb, String colectionName, String removeJson) throws UtilException {
			try {
				mongodb.open();
				DBCollection collection = mongodb.getCollection(colectionName);
				DBObject removeObj = (DBObject)JSON.parse(removeJson);
				collection.remove(removeObj);
			}
			catch(Exception e) {
				throw new UtilException(e);
			}
			finally {
				mongodb.close();
			}
		}
	}
	
	protected SecureServlet secServlet = null;
	protected HttpServletRequest request = null;
	protected HttpServletResponse response = null;
	protected String charset = DEFAULT_CHARSET;
	protected String contentType = DEFAULT_CONTENT_TYPE;
	
	/**
	 * SevureServletから呼ばれる唯一のコンストラクタ
	 * @param servlet：SevureServletです。
	 */
	public Scriptlet() {
	}
	
	
	/**
	 * SevureServletから呼ばれる関数
	 * @param req
	 * @param res
	 */
	public void setServlet(SecureServlet servlet, HttpServletRequest req, HttpServletResponse res) {
		request = req;
		response = res;
		secServlet = servlet;
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public HttpSession getSession() {
		return request.getSession();
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public HttpServletRequest getRequest() {
		return request;
	}

	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public HttpServletResponse getResponse() {
		return response;
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public DBAccesser getDBAccessObject() {
		return secServlet.getDBAccessObject();
	}
	
	public MongoDBAccesser getMongoDBAccesser() {
		return secServlet.getMongoDBAccesser();
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public String getServletName() {
		return secServlet.getServletName();
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public ServletContext getServletContext() {
		return secServlet.getServletContext();
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public String getScriptletPath() {
		return secServlet.getScriptletPath();
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public String getCommonErrorScript() {
		return secServlet.getCommonErrorScript();
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
		response.setCharacterEncoding(charset);
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
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public String getScriptExtName() {
		return secServlet.getScriptExtName();
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
		secServlet.log(msg);
	}
	
	/**
	 * GenericServletの同名のラッパー関数
	 * @param message
	 * @param t
	 */
	public void log(String message, Throwable t) {
		secServlet.log(message, t);
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
	 * @throws SystemErrorException
	 */
	abstract public void start() throws SystemErrorException;

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
