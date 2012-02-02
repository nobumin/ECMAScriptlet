package info.dragonlady.scriptlet;

import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.DocumentA;
import info.dragonlady.util.Navigator;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

//import com.google.appengine.api.datastore.DatastoreService;
//import com.google.appengine.api.datastore.DatastoreServiceFactory;
//import com.google.appengine.api.datastore.Entity;
//import com.google.appengine.api.datastore.FetchOptions;
//import com.google.appengine.api.datastore.Key;
//import com.google.appengine.api.datastore.Query;

/**
 * Rhino実装クラス<br>
 * サーバサイドスクリプトの実行部です。<br>
 * サーバサイドスクリプト内のserverscriptタグを1シリンダとカウントし、<br>
 * このクラスと1対1の対応となります。
 * @author nobu
 *
 */
public class ESCylinder{

	private Scriptlet scriptlet = null;
	private Context cx = null;
	private Scriptable scriptable = null; 
	private Object scriptResult = null;
	private String scriptErrorDetail = null;
	private String scriptImportWord = "@importScript";
	private String scriptImportWordEx = "@import";
	
	/**
	 * ヘルパークラスです。<br>
	 * サーバサイドスクリプト内のグローバルオブジェクト<br>
	 * ”helper”で登録されます。
	 * @author nobu
	 *
	 */
	public static class ESHelper {
		protected Scriptlet scriptlet = null;
		protected String charCode = null;
		/**
		 * 唯一のコンストラクタ
		 * @param slet
		 */
		public ESHelper (Scriptlet slet, String code) {
			scriptlet = slet;
			charCode = code;
		}
		/**
		 * 文字コードを変換します。
		 * @param value
		 * @param fromCharcode
		 * @param toCharcode
		 * @return
		 */
		public String charcodeExchange(String value, String fromCharcode, String toCharcode) {
			try {
				if(value != null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					baos.write(value.getBytes(fromCharcode));
					return baos.toString(toCharcode);
				}
			}
			catch(Exception e) {
			}
			return value;
		}
		/**
		 * 文字コード”UTF-8”で、Stringクラスを再構成します。
		 * @param value：再構成したいStringクラスのインスタンス
		 * @return：再構成されたStringクラス
		 */
		public String toUTF8(String value) {
			try {
				if(value != null) {
					if(charCode != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						baos.write(value.getBytes("UTF-8"));
						return baos.toString(charCode);
					}else{
						return new String(value.getBytes("UTF-8"));
					}
				}
			}
			catch(Exception e) {
			}
			return value;
		}
		/**
		 * 文字コード”Shift-jis”で、Stringクラスを再構成します。
		 * @param value：再構成したいStringクラスのインスタンス
		 * @return：再構成されたStringクラス
		 */
		public String toSJIS(String value) {
			try {
				if(value != null) {
					if(charCode != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						baos.write(value.getBytes("Shift_jis"));
						return baos.toString(charCode);
					}else{
						return new String(value.getBytes("Shift_jis"));
					}
				}
			}
			catch(Exception e) {
			}
			return value;
		}
		/**
		 * 文字コード”EUC_JP”で、Stringクラスを再構成します。
		 * @param value：再構成したいStringクラスのインスタンス
		 * @return：再構成されたStringクラス
		 */
		public String toEUC(String value) {
			try {
				if(value != null) {
					if(charCode != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						baos.write(value.getBytes("euc-jp"));
						return baos.toString(charCode);
					}else{
						return new String(value.getBytes("euc-jp"));
					}
				}
			}
			catch(Exception e) {
			}
			return value;
		}
		/**
		 * 文字コード”ISO-8859-1”で、Stringクラスを再構成します。
		 * @param value：再構成したいStringクラスのインスタンス
		 * @return：再構成されたStringクラス
		 */
		public String to8859(String value) {
			try {
				if(value != null) {
					if(charCode != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						baos.write(value.getBytes("iso-8859-1"));
						return baos.toString(charCode);
					}else{
						return new String(value.getBytes("iso-8859-1"));
					}
				}
			}
			catch(Exception e) {
			}
			return value;
		}
		/**
		 * 
		 * @param url
		 * @return
		 */
		public String createNoCookieURL(String url) {
			return url+";jsessionid="+scriptlet.getRequest().getSession().getId();
		}
		
		/**
		 * URLエンコードを任意の文字コードで行ないます。
		 * @param value：エンコードする文字列
		 * @param charset：文字コード
		 * @return
		 */
		public static String URLEncode(String value, String charset) {
			try {
				return URLEncoder.encode(value, charset);
			}
			catch(Exception e) {
				//
			}
			return null;
		}
		
		/**
		 * 
		 * @param value
		 * @return
		 */
		public static String HTMLEncode(String value) {
			String result = value;
			result = result.replaceAll("&", "&amp;");
			result = result.replaceAll("\"", "&quot;");
			result = result.replaceAll("<", "&lt;");
			result = result.replaceAll(">", "&gt;");
			return result;
		}

		/*
		public void sessionCleaner() throws UtilException {
			try {
		        //実際の消去処理開始
		        Calendar now = Calendar.getInstance();
		        now.add(Calendar.DATE, -2);
		        long targetDate = now.getTimeInMillis();

		        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		        Query q = new Query("_ah_SESSION");
		        q = q.addSort("_expires", Query.SortDirection.ASCENDING);
		        q = q.addFilter("_expires", Query.FilterOperator.LESS_THAN_OR_EQUAL, targetDate);

		        Iterator<Entity> entries = ds.prepare(q).asIterator(FetchOptions.Builder.withLimit(1));
		        if(entries.hasNext()){
		        	Calendar exp = Calendar.getInstance();
		            exp.setTimeInMillis((Long)entries.next().getProperty("_expires"));

		            Query qDel = q.setKeysOnly();
		            while(true){
		            	List<Entity> entDels = ds.prepare(qDel).asList(FetchOptions.Builder.withLimit(200));
		            	if(entDels == null || entDels.size() == 0) {
		                	break;
		                }
		                List<Key> keys = new ArrayList<Key>();
		                for(Entity entDel : entDels){
		                	keys.add(entDel.getKey());
		                }
		                ds.delete(keys);
		            }
		        }
			}
			catch(Exception e) {
				e.printStackTrace();
				throw new UtilException(e);
			}
		}
		*/
	}

	/**
	 * 隠蔽されたコンストラクタ
	 * @param slet
	 */
	private ESCylinder(Scriptlet slet) {
		scriptlet = slet;
	}

	/**
	 * シリンダを生成します。
	 * @param slet：実行されているScriptlet
	 * @return：シリンダ
	 * @throws IOException
	 */
	public static ESCylinder createInstanse(Scriptlet slet, Writer writer, OutputStream stream, String charCode) throws IOException{
		ESCylinder cylinder = new ESCylinder(slet);
		//Rhinoの開始宣言
		ContextFactory cxFactory = new ContextFactory();
		cylinder.cx = cxFactory.enterContext();
		//ECMAScriptエンジンの初期化
		cylinder.scriptable = cylinder.cx.initStandardObjects();
		//グローバルオブジェクトの追加
		Map<String, Object> jsObjectMap = cylinder.scriptlet.getScriptNewProperties();
		if(jsObjectMap != null && !jsObjectMap.isEmpty()) {
			Iterator<String> i = jsObjectMap.keySet().iterator();
			while(i.hasNext()) {
				String key = i.next();
				Object value = jsObjectMap.get(key);
				Object jsObject = Context.javaToJS(value, cylinder.scriptable);
				ScriptableObject.putProperty(cylinder.scriptable, key, jsObject);
			}
		}
		Object jsRequest = Context.javaToJS(cylinder.scriptlet.getRequest(), cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "request", jsRequest);
		Object jsResponse = Context.javaToJS(cylinder.scriptlet.getResponse(), cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "response", jsResponse);
		Object jsSession = Context.javaToJS(cylinder.scriptlet.getSession(), cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "session", jsSession);
		Object jsWriter = Context.javaToJS(writer, cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "serverout", jsWriter);
		Object jsStream = Context.javaToJS(stream, cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "serverstream", jsStream);
		Object jsOut = Context.javaToJS(System.out, cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "sysout", jsOut);
		Object jsErr = Context.javaToJS(System.err, cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "syserr", jsErr);
		Object jsHelper = Context.javaToJS(new ESHelper(slet, charCode), cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "helper", jsHelper);
		Object jsNavigator = Context.javaToJS(new Navigator(slet), cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "navigator", jsNavigator);
		Object jsDBAccesser = Context.javaToJS(cylinder.scriptlet.getDBAccessObject(), cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "dbaccesser", jsDBAccesser);
		Object jsMongoAccesser = Context.javaToJS(cylinder.scriptlet.getMongoDBAccesser(), cylinder.scriptable);
		ScriptableObject.putProperty(cylinder.scriptable, "mongodb", jsMongoAccesser);
		
		return cylinder;
	}

	/**
	 * HTML documentオブジェクトをグローバルオブジェクトとして追加
	 * @param doc
	 */
	public void addDocumentObject(DocumentA doc) {
		doc.setScriptlet(scriptlet);
		Object jsDocument = Context.javaToJS(doc, scriptable);
		ScriptableObject.putProperty(scriptable, "document", jsDocument);
	}
	
	/**
	 * 例外発生時の例外オブジェクトをグローバルオブイェクトに追加する関数
	 * @param e：捕捉した例外（ESException）
	 */
	public void setException(ESException e) {
		Object jsException = Context.javaToJS(e, scriptable);
		ScriptableObject.putProperty(scriptable, "exception", jsException);
	}
	
	/**
	 * 要求パラメータの検証を行う。
	 * @param validateSource
	 * @return true:パラメータエラーあり<br>false:正常パラメータ
	 */
	public void validate(String validateSource) throws ESException{
		StringBuffer sb = new StringBuffer();
		String validates[] = validateSource.split("\n");
		for(int i=0;i<validates.length;i++){
			if(validates[i].startsWith("//")) {
				continue;
			}
			int equalValIdx = validates[i].indexOf("=");
//			int commaValIdx = validates[i].lastIndexOf(",");
			if(equalValIdx < 0) {
				String errMsg = scriptlet.getInvalidValidationParamErrorMsg();
				if(errMsg == null || errMsg.length() < 1) {
					throw new ESException("Invalid validation parameter.\n");
				}else{
					throw new ESException(errMsg);
				}
			}
			String key = validates[i].substring(0, equalValIdx);
//			String regexp = commaValIdx > 0 && (validates[i].toLowerCase().endsWith("true") || validates[i].toLowerCase().endsWith("false"))? validates[i].substring(equalValIdx+1, commaValIdx) : validates[i].substring(equalValIdx+1);
//			boolean require = false;
//			if(commaValIdx > 0) {
//				String temp = validates[i].substring(commaValIdx+1);
//				if(temp != null) {
//					require = Boolean.parseBoolean(temp);
//				}
//			}

			String regexp = ".*";
			boolean require = false;
			String paramName = key;
			String values = validates[i].substring(equalValIdx+1);
			if(values != null) {
				if(values.split(",").length > 0) {
					if(values.toLowerCase().endsWith("true") || values.toLowerCase().endsWith("false")) {
						regexp = values.substring(0, values.lastIndexOf(","));
						require = Boolean.parseBoolean(values.substring(values.lastIndexOf(",")+1));
					}else{
						regexp = values;
					}
				}else{
					regexp = values;
				}
			}

			//TODO
			//CharctorEncodingFilterは利用しないこと！
			//下記の記述により文字化けが発生する可能性があります。
			String reqValue = (new ESHelper(scriptlet, null)).to8859(scriptlet.getRequest().getParameter(key));

			if(reqValue == null || reqValue.length() < 1) {
				if(require) {
					String errMsg = scriptlet.getRequiredParamErrorMsg();
					if(errMsg == null || errMsg.length() < 1) {
						errMsg = paramName + " is required parameter.";
					}else{
						errMsg = paramName + errMsg;
					}
					sb.append(errMsg + "\n");
					continue;
				}
			}else{
				Pattern pattern = Pattern.compile(regexp);
				Matcher matcher = pattern.matcher(reqValue);
				if(!matcher.matches()){
					String errMsg = scriptlet.getInvalidParamErrorMsg();
					if(errMsg == null || errMsg.length() < 1) {
						errMsg = paramName + " is invalid parameter.";
					}else{
						errMsg = paramName + errMsg;
					}
					sb.append(errMsg + "\n");
					continue;
				}
			}
			
		}
		if(sb.length() > 0) {
			throw new ESException(sb.toString());
		}
	}

	/**
	 * サーバサイドスクリプト（Rhino）実行処理
	 * @param jsSource：サーバサイドスクリプト
	 * @throws ESException
	 */
	public void process(String jsSource, String path) throws ESException{
		try {
			String jsName = scriptlet.getServletName();
			scriptResult = cx.evaluateString(scriptable, scriptImport(jsSource, path), jsName, 1, null);
		}
		catch(Exception e) {
			if(e instanceof RhinoException) {
				scriptErrorDetail = ((RhinoException)e).getScriptStackTrace();
			}else{
				scriptErrorDetail = null;
			}
			String errMsg = scriptlet.getEScriptErrorMsg();
			if(errMsg == null || errMsg.length() < 1) {
				throw new ESException(errMsg, e);
			}
			throw new ESException(e);
		}
	}
	
	/**
	 * '@importScript'ディレクティブを処理する関数
	 * @param jsSource：サーバサイドスクリプト
	 * @param path：サーバサイドスクリプトの格納パス
	 * @return：外部スクリプトが追加されたサーバサイドスクリプト
	 * @throws IOException
	 * @throws ESException 
	 */
	protected String scriptImport(String jsSource, String path) throws IOException, ESException {
		StringBuffer result = new StringBuffer(jsSource);
		while(result.indexOf(scriptImportWord) >=0) {
			int beginIndex = result.indexOf(scriptImportWord);
			int endIndex   = result.indexOf(";", beginIndex);
			String importValue = result.substring(beginIndex, endIndex);
			result.replace(beginIndex, endIndex, loadScript(importValue, path));
		}
		//2011.05.12
		//@importは必ず後から処理しなければならない！
		while(result.indexOf(scriptImportWordEx) >=0) {
			int beginIndex = result.indexOf(scriptImportWordEx);
			int endIndex   = result.indexOf(";", beginIndex);
			String importValue = result.substring(beginIndex, endIndex);
			result.replace(beginIndex, endIndex, loadScriptEx(importValue, path));
		}
		return result.toString();
	}
	
	/**
	 * '@importScript'ディレクティブを解析して追加するスクリプトを読み込む関数
	 * @param importValue：'@importScript'ディレクティブ
	 * @param path：サーバサイドスクリプトの格納パス
	 * @return：追加するサーバサイドスクリプト
	 * @throws IOException
	 */
	protected String loadScript(String importValue, String path) throws IOException {
		String scriptFileName = new String();
		if(importValue.indexOf("\"") > 0){
			scriptFileName = importValue.substring(importValue.indexOf("\"")+1, importValue.lastIndexOf("\""));
		}else
		if(importValue.indexOf("'") > 0){
			scriptFileName = importValue.substring(importValue.indexOf("'")+1, importValue.lastIndexOf("'"));
		}
		
		if(!path.endsWith("/") && !scriptFileName.startsWith("/")){
			scriptFileName = "/" + scriptFileName;
		}
		
		//TODO
		//文字コード対策
		StringBuffer script = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(path+scriptFileName));
		String line = null;
		while((line = reader.readLine()) != null) {
			script.append(line);
			script.append("\n");
		}
		reader.close();
		return script.toString();
	}
	
	/**
	 * '@import'ディレクティブを解析して追加するスクリプトを埋込む関数
	 * '@importScript'と異なり、serverscriptタグ等を指定できるスクリプトファイルを読み込む
	 * @param importValue
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws ESException 
	 */
	protected String loadScriptEx(String importValue, String path) throws IOException, ESException {
		String scriptFileName = new String();
		if(importValue.indexOf("\"") > 0){
			scriptFileName = importValue.substring(importValue.indexOf("\"")+1, importValue.lastIndexOf("\""));
		}else
		if(importValue.indexOf("'") > 0){
			scriptFileName = importValue.substring(importValue.indexOf("'")+1, importValue.lastIndexOf("'"));
		}
		
		if(!path.endsWith("/") && !scriptFileName.startsWith("/")){
			scriptFileName = "/" + scriptFileName;
		}

		//TODO
		//文字コード対策
		StringBuffer script = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(path+scriptFileName));
		String line = null;
		while((line = reader.readLine()) != null) {
			script.append(line);
			script.append("\n");
		}
		reader.close();
		
		parseWordBuffer = new StringBuffer();
		return parse(script.toString(), true);
	}

	protected StringBuffer parseWordBuffer = null;
	protected String parse(String script, boolean start) throws ESException {
		if(start) {
			int scriptIdx = script.indexOf(ESEngine.serverScriptTag);
			int validateIdx = script.indexOf(ESEngine.serverValidationTag);
			if(scriptIdx >= 0 || validateIdx >= 0) {
				if((scriptIdx >= 0 && scriptIdx < validateIdx) || validateIdx < 0) {
					parseWordBuffer.append("serverout.write('");
					parseWordBuffer.append(script.substring(0, scriptIdx).replaceAll("'", "\\\\'").replaceAll("\n", "\\\\n"));
					parseWordBuffer.append("');\n");
					parse(script.substring(scriptIdx+ESEngine.serverScriptTag.length()), false);
				}else
				if((validateIdx >= 0 && validateIdx <  scriptIdx) || scriptIdx < 0){
					parseWordBuffer.append("serverout.write('");
					parseWordBuffer.append(script.substring(0, validateIdx).replaceAll("'", "\\\\'").replaceAll("\n", "\\\\n"));
					parseWordBuffer.append("');\n");
					parse(script.substring(validateIdx+ESEngine.serverValidationTag.length()), false);
				}
			}
			if(scriptIdx < 0 && validateIdx < 0) {
				parseWordBuffer.append("serverout.write('");
				parseWordBuffer.append(script.replaceAll("'", "\\\\'").replaceAll("\n", "\\\\n"));
				parseWordBuffer.append("');\n");
			}
		}else{
			int scriptEndIdx = script.indexOf(ESEngine.serverScriptEndTag);
			int validateEndIdx = script.indexOf(ESEngine.serverValidationEndTag);
			if(scriptEndIdx >= 0 || validateEndIdx >= 0) {
				if((scriptEndIdx >= 0 && scriptEndIdx < validateEndIdx) || validateEndIdx < 0) {
					parseWordBuffer.append(script.substring(0, scriptEndIdx));
					parse(script.substring(scriptEndIdx+ESEngine.serverScriptEndTag.length()), true);
				}else
				if((validateEndIdx >= 0 && validateEndIdx <  scriptEndIdx) || scriptEndIdx < 0){
					validate(script.substring(0, validateEndIdx));
					parse(script.substring(validateEndIdx+ESEngine.serverValidationEndTag.length()), true);
				}
			}
			if(scriptEndIdx < 0 && validateEndIdx < 0) {
				parseWordBuffer.append("serverout.write('");
				parseWordBuffer.append(script.replaceAll("'", "\\\\'").replaceAll("\n", "\\\\n"));
				parseWordBuffer.append("');");
			}
		}
		return parseWordBuffer.toString();
	}
	
	/**
	 * スクリプトの実行結果を取得できます。
	 * @return
	 */
	public Object getResult() {
		return scriptResult;
	}
	
	/**
	 * スクリプトのエラー詳細を取得できます。
	 * @return
	 */
	public String getErrorDetail() {
		return scriptErrorDetail;
	}

	/**
	 * Rhinoの終了宣言<br>
	 * 必ず開始宣言と対で呼出すこと！
	 */
	public void exit() {
		Context.exit();
	}
}
