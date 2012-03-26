package info.dragonlady.scriptlet;

import info.dragonlady.util.DocumentA;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * サーバサイドスクリプトの初期読み込み、およびシリンダの実行要求を行う、<br>
 * フレームワークの中核クラス<br>
 * Scriptletクラスからは、必ずexecuteScript関数を呼出す必要があります。
 * @author nobu
 *
 */
public class ESEngine {
	//追加したタグ名
	public static final String serverScriptTag = "<serverscript>";
	public static final String serverValidationTag = "<servervalidation>";
	public static final String serverScriptEndTag = "</serverscript>";
	public static final String serverValidationEndTag = "</servervalidation>";
	protected static final String bodyTagOnloadAttrPt = ".*\\<\\s*body[^\\>]+onloadserver=['\"]([^\\)]+\\))['\"].+";
	protected static final String scriptLoadAttrPt = ".*\\<\\s*serverscript[^\\>]+src=['\"]([^'\"]+)['\"].+";
	
	//スクリプトレットのデフォルト文字コード
	protected static final String defaultScriptletCharcode = "utf-8";
	protected String contentCharcode = defaultScriptletCharcode;
	//Buffer writer
	protected StringWriter bufferWriter = new StringWriter();
	protected ByteArrayOutputStream bufferOutputStream = new ByteArrayOutputStream();
	
	//デフォルトスクリプトレット名
	public static final String defaultScriptletName = "/index";
	//content-lengthヘッダの強制設定フラグ
	public static boolean forceContentLengthHeader = false;

	/**
	 * DOM解析エラーハンドラー
	 * @author nobu
	 *
	 */
	private class ParseErrorHandler implements ErrorHandler {
		public void warning(SAXParseException e) {
			System.err.println("WARN: " + e.getLineNumber());
			System.err.println(e.getMessage());
		}
		public void error(SAXParseException e) {
			System.err.println("ERROR: " + e.getLineNumber());
			System.err.println(e.getMessage());
		}
		public void fatalError(SAXParseException e) {
			System.err.println("FATAL: " + e.getLineNumber());
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * サーバサイドスクリプトの解析クラス<br>
	 * 内部非公開クラスのため、説明を省略
	 * @author nobu
	 *
	 */
	private class HtmlParser {
		protected ESCylinder cylinder = null;
		protected String onLoadFunction = null;
		protected String loadScript = null;
		protected StringWriter writer = null;
		protected DocumentA document = null;
		
		public HtmlParser(ESCylinder cyl, StringWriter w) throws IOException {
			cylinder = cyl;
			writer = w;
		}
		
		public void parse(String script) throws ESException{
			int scriptIdx = script.indexOf(serverScriptTag);
			int validateIdx = script.indexOf(serverValidationTag);
			if(scriptIdx >= 0 || validateIdx >= 0) {
				if((scriptIdx >= 0 && scriptIdx < validateIdx) || validateIdx < 0) {
					writer.write(script.substring(0, scriptIdx));
					writer.flush();
					parse(execScript(script.substring(scriptIdx+serverScriptTag.length())));
					writer.flush();
				}else
				if((validateIdx >= 0 && validateIdx <  scriptIdx) || scriptIdx < 0){
					writer.write(script.substring(0, validateIdx));
					writer.flush();
					parse(execValidation(script.substring(validateIdx+serverValidationTag.length())));
					writer.flush();
				}
			}
			if(scriptIdx < 0 && validateIdx < 0) {
				writer.write(script);
				writer.flush();
			}
		}
		
		public Document getHTMLDocuemnt() {
			return document;
		}
		
		public boolean isNormalizeXML() {
			try {
				checkHTML(writer.toString());
				if(getOnLoadFunction() != null && getOnLoadFunction().length() > 2 && 
					getLoadScriptFileName() != null && getLoadScriptFileName().length() > 1) {
					StringReader sr = new StringReader(writer.toString());
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//					factory.setValidating(true);
//					factory.setNamespaceAware(true);
					DocumentBuilder builder = factory.newDocumentBuilder();
					builder.setErrorHandler(new ParseErrorHandler());
					InputSource is = new InputSource(sr);
					document = new DocumentA(builder.parse(is));
					cylinder.addDocumentObject(document);
					return true;
				}
			}
			catch(Exception e) {
				//NOP
				e.printStackTrace(System.err);
			}
			
			return false;
		}
		
		public String getOnLoadFunction() {
			return onLoadFunction;
		}
		
		public String getLoadScriptFileName() {
			return loadScript;
		}
		
		protected void checkHTML(String script) {
			Pattern p = Pattern.compile(bodyTagOnloadAttrPt, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CASE);
			Matcher m = p.matcher(script);
			if(m.matches()) {
				onLoadFunction = m.group(1); 
			}

			p = Pattern.compile(scriptLoadAttrPt, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CASE);
			m = p.matcher(script);
			if(m.matches()) {
				loadScript = m.group(1); 
			}
		}
		
		protected String execScript(String script) throws ESException{
			int scriptIdx = script.indexOf(serverScriptEndTag);
			String scriptValue = removeStartLF(script.substring(0, scriptIdx));
			cylinder.process(scriptValue, getScriptletDirPath());
			return script.substring(scriptIdx+serverScriptEndTag.length());
		}
		
		protected String execValidation(String script) throws ESException{
			int validateIdx = script.indexOf(serverValidationEndTag);
			String validationValue = removeStartLF(script.substring(0, validateIdx));
			cylinder.validate(validationValue);
			return script.substring(validateIdx+serverValidationEndTag.length());
		}
		
		private String removeStartLF(String value) {
			if(value.startsWith("\n")) {
				return value.substring(1);
			}
			return value;
		}
	}
	
	
	private class TextParser {
		protected ESCylinder cylinder = null;
		protected StringWriter writer = null;
		
		public TextParser(ESCylinder cyl, StringWriter w) throws IOException {
			cylinder = cyl;
			writer = w;
		}

		public void parse(String script) throws ESException{
			int scriptIdx = script.indexOf(serverScriptTag);
			if(scriptIdx >= 0) {
				writer.write(script.substring(0, scriptIdx));
				writer.flush();
				parse(execScript(script.substring(scriptIdx+serverScriptTag.length())));
				writer.flush();
			}
			if(scriptIdx < 0) {
				writer.write(script);
				writer.flush();
			}
		}

		protected String execScript(String script) throws ESException{
			int scriptIdx = script.indexOf(serverScriptEndTag);
			String scriptValue = removeStartLF(script.substring(0, scriptIdx));
			cylinder.process(scriptValue, getScriptletDirPath());
			return script.substring(scriptIdx+serverScriptEndTag.length());
		}

		private String removeStartLF(String value) {
			if(value.startsWith("\n")) {
				return value.substring(1);
			}
			return value;
		}
	}

	//サーバサイドスクリプトを格納するバッファ
	private static HashMap<String, String> scriptsMap = new HashMap<String, String>();
	private static HashMap<String, Long> scriptsLastModify = new HashMap<String, Long>();
	//サーバサイドスクリプトの実格納パス
	private String scriptPath = "";

	//サーバサイドスクリプトのファイル命名規約
	private static final String errorSufttix = "_error";
	
	private ESEngine() {
	}

	/**
	 * 内部非公開クラスHtmlParserを生成する関数
	 * @param cylinder：生成したシリンダ
	 * @param scriptlet：呼び出し元のスクリプトレット
	 * @return：HtmlParserクラスのインスタンス
	 * @throws IOException
	 */
	protected final HtmlParser craeteHtmlParser(ESCylinder cylinder) throws IOException{
		return new HtmlParser(cylinder, bufferWriter);
	}
	
	/**
	 * 内部非公開クラスTextParserを生成する関数
	 * @param cylinder
	 * @return
	 * @throws IOException
	 */
	protected final TextParser createTextParser(ESCylinder cylinder) throws IOException{
		return new TextParser(cylinder, bufferWriter);
	}

	/**
	 * サーバサイドスクリプトを読み込む関数
	 * @param file：サーバサイドスクリプトのファイル
	 * @return：読み込んだサーバサイドスクリプトの文字列
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected final String loadScript(File file) throws FileNotFoundException, IOException {
		//Check charcode
		StringBuffer script = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), defaultScriptletCharcode));
		String line = null;
		boolean hasMetaTag = false;
		String charCode = null;
		try {
			while((line = reader.readLine()) != null) {
				//search this line
				//<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
				if(line.toLowerCase().indexOf("meta") > -1) {
					hasMetaTag = true;
				}
				if(hasMetaTag && line.toLowerCase().indexOf("charset=") > -1) {
					String metaCharsetValue = line.substring(line.toLowerCase().indexOf("charset=")+"charset=".length());
					Pattern p = Pattern.compile("(^[a-zA-Z0-9\\-_]+).*");
					Matcher m = p.matcher(metaCharsetValue.replaceAll("[\"']", ""));
					if(m.matches()) {
						charCode = m.group(1);
					}
					break;
				}
				script.append(line);
				script.append("\n");
			}
		}
		catch(IOException e ){
			throw e;
		}
		finally{
			if(reader != null) {
				reader.close();
			}
		}
		reader=null;

		if(charCode != null) {
			contentCharcode = charCode;
			script = new StringBuffer();
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charCode));
				while((line = reader.readLine()) != null) {
					script.append(line);
					script.append("\n");
				}
			}
			catch(IOException e ){
				throw e;
			}
			finally{
				if(reader != null) {
					reader.close();
				}
			}
		}
		
		return script.toString();
	}
	
	/**
	 * このクラスのインスタンス生成時に一度だけ実行される初期化関数
	 * @param basePath：サーバサイドスクリプトの格納パス
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void initialize(String basePath) throws FileNotFoundException, IOException {
		if(scriptsMap.size() < 2) {
			File scriptsDir = new File(basePath);
			if(scriptsDir.isDirectory()) {
				directoryReflexive("/", scriptsDir);
			}
		}
	}
	
	/**
	 * 
	 * @param absolutePath
	 * @param dir
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void directoryReflexive(String absolutePath, File dir) throws FileNotFoundException, IOException {
		if(!absolutePath.startsWith(".")) {
			for(int i=0;i<dir.listFiles().length;i++) {
				if(!dir.list()[i].startsWith(".")) {
					if(!dir.listFiles()[i].isDirectory()) {
						scriptsMap.put(absolutePath+dir.list()[i], loadScript(dir.listFiles()[i]));
						scriptsLastModify.put(absolutePath+dir.list()[i], dir.listFiles()[i].lastModified());
					}else{
						directoryReflexive(absolutePath+dir.list()[i]+"/", dir.listFiles()[i]);
					}
				}
			}
		}
	}
	
	/**
	 * BodyタグのOnLoadイベント用のスクリプトテンプレートを生成する関数
	 * @param scriptlet
	 * @param fileName
	 * @param functionName
	 * @return
	 * @throws InvalidPropertiesFormatException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected static String createOnLoadScript(Scriptlet scriptlet, String fileName, String functionName) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		String realPath = scriptlet.getServletContext().getRealPath("/");
		if(!realPath.endsWith("/")) { //for Jetty
			realPath += "/";
		}
		String paramPath = realPath+"WEB-INF"+File.separator+"config.xml";
		Properties prop = new Properties();   
		prop.loadFromXML(new FileInputStream(paramPath));
		return String.format(prop.getProperty("onloadTemplate"), fileName, functionName);
	}
	
	/**
	 * スクリプトレットの文字コードを応答する
	 * デフォルトはUTF-8
	 * @return
	 */
	public String getContentCharcode() {
		return contentCharcode;
	}
	
	/**
	 * サーバサイドスクリプトを実行する関数<br>
	 * スクリプトファイル名：サーブレット名<br>
	 * 例外発生時のスクリプトファイル名：サーブレット名_error<br>
	 * スクリプトレット名はsitemap.xmlに定義されたclass要素内の値
	 * @param scriptlet：呼び出し元のスクリプトレット
	 * @throws ESException
	 */
	public static void executeScript(Scriptlet scriptlet) throws ESException{
		ESCylinder cylinder = null;
		String errorFileName = null;
		ESEngine engine = new ESEngine();
		try {
			engine.scriptPath = scriptlet.getScriptletPath();
			engine.initialize(engine.scriptPath);
			
			String scriptFileName = scriptlet.getRequest().getRequestURI();
			String contextPath = scriptlet.getRequest().getContextPath();
			if(scriptFileName.startsWith(contextPath)) {
				scriptFileName = scriptFileName.substring(contextPath.length());
			}
			if(scriptFileName.lastIndexOf(";jsessionid") > 0) {
				scriptFileName = scriptFileName.substring(0, scriptFileName.lastIndexOf(";jsessionid"));
			}
			if(scriptFileName == null || scriptFileName.length() < 2 || scriptFileName.endsWith("/")) {
				scriptFileName = defaultScriptletName + scriptlet.getScriptExtName();
			}
			errorFileName = scriptFileName + errorSufttix;
			
			File scriptFile = new File(engine.scriptPath+scriptFileName);
			if(scriptFile.exists()) {
				if(scriptsLastModify.get(scriptFileName) == null || scriptsLastModify.get(scriptFileName) != scriptFile.lastModified()) {
					scriptsMap.put(scriptFileName, engine.loadScript(scriptFile));
					scriptsLastModify.put(scriptFileName, scriptFile.lastModified());
				}
			}else{
				scriptsMap.remove(scriptFileName);
				scriptsLastModify.remove(scriptFileName);
				throw new NotFoundException("404 not found");
			}
			scriptlet.setCharSet(engine.getContentCharcode());
			
			String script = scriptsMap.get(scriptFileName);
			cylinder = ESCylinder.createInstanse(scriptlet, engine.bufferWriter, engine.bufferOutputStream, engine.getContentCharcode());
			HtmlParser htmlParser = engine.craeteHtmlParser(cylinder);
			htmlParser.parse(script);
			engine.bufferWriter.flush();
			engine.bufferWriter.close();
			scriptlet.getResponse().setContentType(scriptlet.getContentTypeValue());
			if(htmlParser.isNormalizeXML() && 
				htmlParser.getOnLoadFunction() != null && htmlParser.getOnLoadFunction().length() > 2 && 
				htmlParser.getLoadScriptFileName() != null && htmlParser.getLoadScriptFileName().length() > 1) {
				String onLoadScript = createOnLoadScript(scriptlet, htmlParser.getLoadScriptFileName(), htmlParser.getOnLoadFunction());
				cylinder.process(onLoadScript, engine.scriptPath);
				engine.bufferOutputStream.flush();
				engine.bufferOutputStream.close();
				if(engine.bufferOutputStream.toByteArray().length > 0){
					if(forceContentLengthHeader){
						scriptlet.getResponse().setContentLength(engine.bufferOutputStream.size());
					}
					scriptlet.getResponse().getOutputStream().write(engine.bufferOutputStream.toByteArray(), 0, engine.bufferOutputStream.size());
					scriptlet.getResponse().getOutputStream().flush();
				}else{
					StringBuffer result = new StringBuffer();
					print(htmlParser.getHTMLDocuemnt(), result);
					if(forceContentLengthHeader){
						scriptlet.getResponse().setContentLength(result.toString().getBytes().length);
					}
					scriptlet.getResponse().getWriter().write(result.toString());
					scriptlet.getResponse().getWriter().flush();
				}
			}else{
				engine.bufferOutputStream.flush();
				engine.bufferOutputStream.close();
				if(engine.bufferOutputStream.toByteArray().length > 0){
					if(forceContentLengthHeader){
						scriptlet.getResponse().setContentLength(engine.bufferOutputStream.size());
					}
					scriptlet.getResponse().getOutputStream().write(engine.bufferOutputStream.toByteArray(), 0, engine.bufferOutputStream.size());
					scriptlet.getResponse().getOutputStream().flush();
				}else{
					if(forceContentLengthHeader){
						scriptlet.getResponse().setContentLength(engine.bufferWriter.toString().getBytes().length);
					}
					scriptlet.getResponse().getWriter().write(engine.bufferWriter.toString());
					scriptlet.getResponse().getWriter().flush();
				}
			}
		}
		catch(NotFoundException e) { //404
			throw new ESException(new IOException("not found script"));
		}
		catch(Exception e) {
			if(cylinder != null) {
				try {
					if(cylinder.getErrorDetail() != null) {
						System.err.println("-Script Error------------------------------------");
						System.err.println(cylinder.getErrorDetail());
						System.err.println("-------------------------------------------------");
					}
				}
				finally {
					cylinder.exit();
				}
				cylinder = null;
			}
			try {
				File errorFile = new File(engine.scriptPath+errorFileName);
				if(errorFile.exists()) {
					scriptsMap.put(errorFileName, engine.loadScript(errorFile));
					scriptsLastModify.put(errorFileName, errorFile.lastModified());
				}else{
					if(scriptlet.getCommonErrorScript() != null) {
						File defaultErrorFile = new File(scriptlet.getCommonErrorScript());
						if(defaultErrorFile.exists()) {
							scriptsMap.put(scriptlet.getCommonErrorScript(), engine.loadScript(defaultErrorFile));
							scriptsLastModify.put(scriptlet.getCommonErrorScript(), defaultErrorFile.lastModified());
						}
					}
				}
			}
			catch(Exception ex) {
				//NOP
				ex.printStackTrace(System.out);
			}
			scriptlet.setCharSet(engine.getContentCharcode());
			exceptionProc(scriptlet, e);
		}
		finally{
			if(cylinder != null) {
				cylinder.exit();
			}
		}
	}
		
	/**
	 * スクリプトの格納パスを応答する関数
	 * @return
	 */
	public String getScriptletDirPath() {
		return scriptPath;
	}
	
	/**
	 * エラー処理時のスクリプトを実行する関数<br>
	 * 例外発生時のスクリプトファイル名：サーブレット名_error.ses<br>
	 * サーブレット名はweb.xmlに定義されたurl-pattern要素内の値
	 * @param scriptlet：呼び出し元のスクリプトレット
	 * @param e：スクリプトに渡したい例外オブジェクト
	 * @throws ESException
	 */
	public static void exceptionProc(Scriptlet scriptlet, Exception e) throws ESException{
		ESEngine engine = new ESEngine();
		engine.scriptPath = scriptlet.getScriptletPath();
		String scriptFileName = scriptlet.getRequest().getRequestURI();
		String contextPath = scriptlet.getRequest().getContextPath();
		if(scriptFileName.startsWith(contextPath)) {
			scriptFileName = scriptFileName.substring(contextPath.length());
		}
		if(scriptFileName.lastIndexOf(";jsessionid") > 0) {
			scriptFileName = scriptFileName.substring(0, scriptFileName.lastIndexOf(";jsessionid"));
		}
		if(scriptFileName == null || scriptFileName.length() < 2 || scriptFileName.endsWith("/")) {
			scriptFileName = defaultScriptletName + scriptlet.getScriptExtName();
		}
		String errorFileName = scriptFileName + errorSufttix;
		ESCylinder cylinder = null;
		if(scriptsMap.containsKey(errorFileName)) {
			try {
				String script = scriptsMap.get(errorFileName);
				cylinder = ESCylinder.createInstanse(scriptlet,  engine.bufferWriter, engine.bufferOutputStream, engine.getContentCharcode());
				ESException ex = new ESException(e);
				ex.setInitFullURL(scriptlet.getRequest().getRequestURL().toString());
				ex.setInitURL(scriptlet.getRequest().getServletPath());
				cylinder.setException(ex);
				HtmlParser htmlParser = engine.craeteHtmlParser(cylinder);
				htmlParser.parse(script);
				engine.bufferWriter.flush();
				engine.bufferWriter.close();
				scriptlet.getResponse().getWriter().write(engine.bufferWriter.toString());
			}
			catch(Exception ex) {
				if(cylinder != null && cylinder.getErrorDetail() != null) {
					System.err.println("-Script Error------------------------------------");
					System.err.println(cylinder.getErrorDetail());
					System.err.println("-------------------------------------------------");
				}
				ex.printStackTrace(System.out);
				throw new ESException(e);
			}
			finally{
				if(cylinder != null) {
					cylinder.exit();
				}
			}
		}else if(scriptsMap.containsKey(scriptlet.getCommonErrorScript())) {
			try {
				String script = scriptsMap.get(scriptlet.getCommonErrorScript());
				cylinder = ESCylinder.createInstanse(scriptlet,  engine.bufferWriter, engine.bufferOutputStream, engine.getContentCharcode());
				ESException ex = new ESException(e);
				ex.setInitFullURL(scriptlet.getRequest().getRequestURL().toString());
				ex.setInitURL(scriptlet.getRequest().getServletPath());
				cylinder.setException(ex);
				HtmlParser htmlParser = engine.craeteHtmlParser(cylinder);
				htmlParser.parse(script);
				engine.bufferWriter.flush();
				engine.bufferWriter.close();
				scriptlet.getResponse().getWriter().write(engine.bufferWriter.toString());
			}
			catch(Exception ex) {
				if(cylinder != null && cylinder.getErrorDetail() != null) {
					System.err.println("-Script Error------------------------------------");
					System.err.println(cylinder.getErrorDetail());
					System.err.println("-------------------------------------------------");
				}
				ex.printStackTrace(System.out);
				throw new ESException(e);
			}
			finally{
				if(cylinder != null) {
					cylinder.exit();
				}
			}
		}else{
			throw new ESException(e);
		}
	}

	/**
	 * XML(DOM)の文字列表現を作成する。
	 * @param node:文字列表現にするDOMオブジェクト
	 * @param buffer:文字列表現を格納するバッファ
	 */
	protected static void print(Node node, StringBuffer buffer) {
		StringBuffer result = buffer;
		if(result == null) {
			result = new StringBuffer();
		}

		if(node == null) {
			return ;
		}
		
		switch(node.getNodeType()){
		case Node.DOCUMENT_NODE:
			Document doc = (Document)node;
			DocumentType docType = doc.getDoctype();
			if(docType != null) {
				String dtdName  = docType.getName();
				String interSub = docType.getInternalSubset();
				String PubID	= docType.getPublicId();
				String SysID	= docType.getSystemId();
				if(interSub != null)
				{
					result.append("<!DOCTYPE " + dtdName + "[\n");
					result.append(interSub);
					result.append("]>\n");
				}else{
					if(dtdName != null)
					{
						if(PubID != null)
						{
							result.append("<!DOCTYPE " + dtdName + " PUBLIC " + "\"" + PubID + "\">\n");
						}
						if(SysID != null)
						{
							result.append("<!DOCTYPE " + dtdName + " SYSTEM " + "\"" + SysID + "\">\n");
						}
					}
				}
			}
			print(doc.getDocumentElement(), result);
			break;
		case Node.ELEMENT_NODE:
			result.append('<');
			result.append(node.getNodeName());
			org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
			for(int i=0;i<attrs.getLength();i++)
			{
				if(escape(attrs.item(i).getNodeValue()) != null && escape(attrs.item(i).getNodeValue()).length() > 0) {
					result.append(' ');
					result.append(attrs.item(i).getNodeName());
					result.append("=\"");
					result.append(escape(attrs.item(i).getNodeValue()));
					result.append("\"");
				}
			}
			if(node.hasChildNodes()) {
				result.append(">");
				org.w3c.dom.NodeList children = node.getChildNodes();
				if(children != null)
				{
					if(children.getLength() > 1 ||
						(children.getLength() == 1 && children.item(0).getNodeType() != Node.TEXT_NODE)){
						result.append("\n");
					}
					for(int i=0;i<children.getLength();i++)
					{
						print(children.item(i), result);
					}
				}
			}else{
				result.append("/>");
				result.append("\n");
			}
			break;

		case Node.ENTITY_REFERENCE_NODE:
			result.append("&");
			result.append(node.getNodeName());
			result.append(";");
			break;

		case Node.CDATA_SECTION_NODE:
			result.append("<![CDATA[");
			result.append(node.getNodeValue());
			result.append("]]>");
			result.append("\n");
			break;

		case Node.TEXT_NODE:
			result.append(escape(node.getNodeValue()));
			break;

		case Node.PROCESSING_INSTRUCTION_NODE:
			result.append("<?");
			result.append(node.getNodeName());
			String data = node.getNodeValue();
			if(data != null && data.length() > 0)
			{
				result.append(" ");
				result.append(data);
			}
			result.append("?>");
			result.append("\n");
			break;
		}

		if(node.getNodeType() == Node.ELEMENT_NODE && node.hasChildNodes())
		{
			result.append("</");
			result.append(node.getNodeName());
			result.append(">");
			result.append("\n");
		}
		return;
	}

	/**
	 * HTMLエスケープ
	 * @param target:テキストノードの内容
	 * @return:エスケープされた文字列
	 */
	protected static String escape(String target) {
		StringBuffer str = new StringBuffer();
		int i=0;
		String checkTarget = target.replaceAll("[\\n\\r\\s\\t]", "");
		if(checkTarget.length() < 1) {
			return "";
		}

		int len = (target != null) ? target.length() : 0;
		for(i=0;i<len;i++)
		{
			char ch = target.charAt(i);
			switch ( ch ) {
			case '<':
				str.append("&lt;");
				break;

			case '>':
				str.append("&gt;");
				break;

			case '&':
				str.append("&amp;");
				break;

			case '"':
				str.append("&quot;");
				break;

			default:
				str.append(ch);
			}
		}

		return str.toString();
    }
}
