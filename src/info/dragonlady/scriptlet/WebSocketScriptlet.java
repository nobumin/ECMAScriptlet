package info.dragonlady.scriptlet;

import static java.util.concurrent.TimeUnit.SECONDS;
import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.MongoDBAccesser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;

public class WebSocketScriptlet extends WebSocketServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2891636336085761944L;
	private String SCRIPTLET_PATH = "scriptlet_path";
	private String SITEMAP_PATH = "sitemap";
	private String DBCONFIG_PATH = "dbconfig";
	private String DBOBJECT_CONTROL = "dbobject";
	private String MONGO_DBOBJECT_CONTROL = "mongodbject";
	private String MONGO_DBCONFIG_PATH = "mongodbconfig";
	private String EXT_NAME = "extendName";
//	private String OUT_OF_SERVICE = "out_of_service";
	protected String scriptletPath = "WEB-INF"+File.separator+"scriptlet"+File.separator;
	protected String sitemapPath = "WEB-INF"+File.separator+"sitemap.xml";
	protected String dbConfigPath = "WEB-INF"+File.separator+"db_config.xml";
	protected String mongoDBConfigPath = "WEB-INF"+File.separator+"mongodb.xml";
	protected Document sitemapXML = null;
	protected DBAccesser dbaccesser = null;
	protected MongoDBAccesser mongoDBAccesser = null;
	protected String defaultWSScriptClassName = null;
	protected String extendName = null;
	private Properties properties = new Properties();
	private long sitemapFileModify = -1;
	private boolean outOfServer = false;
	private String charSetCode = "utf-8";
	
	private static ScheduledExecutorService scheduler = null;
	private static ScheduledFuture<?> timeHandle = null;

	private Vector<ScriptletServerInbound> _inboundList = new Vector<ScriptletServerInbound>();
	
	/**
	 * 
	 * @author nobu
	 *
	 */
	protected class ScriptletServerInbound extends MessageInbound {
		private WebSocketScriptlet _wsScriptlet = null;
		private WsOutbound _outbound = null;
		private BaseJsonRequest _baseJson = null;
		
		protected ScriptletServerInbound(WebSocketScriptlet wsScriptlet) {
			_wsScriptlet = wsScriptlet;
		}
		
		protected void onOpen(WsOutbound outbound) {
			_outbound = outbound;
			if(!_inboundList.contains(this)) {
				_inboundList.add(this);
			}
			if(_inboundList.size() == 1) {
				if(scheduler == null) {
					scheduler = Executors.newSingleThreadScheduledExecutor();
					if(timeHandle != null) {
						timeHandle.cancel(true);
					}
					CallbackEventWatcher watcher = new CallbackEventWatcher();
					timeHandle = scheduler.scheduleAtFixedRate(watcher, 1, 1, SECONDS);
				}
			}
		}
		
		protected void onClose(int status) {
			if(_inboundList.contains(this)) {
				_inboundList.remove(this);
			}
			if(_inboundList.size() < 1) {
				if(timeHandle != null) {
					timeHandle.cancel(true);
					timeHandle = null;
				}
				try {
					if(scheduler != null) {
						scheduler.shutdownNow();
						scheduler.awaitTermination(1, SECONDS); //shutdown wating 10 sec
						scheduler = null;
					}
				} catch (Exception ex) {
					ex.printStackTrace(System.out);
				}
			}
		}
		
		@Override
		protected void onBinaryMessage(ByteBuffer binary) throws IOException {
			//TODO 未対応
		}

		@Override
		protected void onTextMessage(CharBuffer text) throws IOException {
			String session = null;
			if(_baseJson != null && _baseJson.session != null && _baseJson.session.length() > 0) {
				session = _baseJson.session;
			}
			Gson gson = new Gson();
			_baseJson = gson.fromJson(text.toString(), BaseJsonRequest.class);
			if(session != null) {
				_baseJson.setSession(session);
			}
			execScript(true);
		}
		
		protected void execScript(boolean isRequest) throws IOException {
			String path = null;
			if(_baseJson !=null) {
				if(isRequest) {
					path = _baseJson.path;
				}else{
					path = _baseJson.excute;
				}
			}
			try {
				if(path != null && path.length() > 0) {
					_baseJson.setResult("");
					WSScriptlet scriptlet = buildScriptlet(path);
					if(scriptlet != null) {
						_baseJson.clearResult();
						scriptlet.setServlet(_wsScriptlet);
						scriptlet.start(_baseJson, !isRequest);
						//正常
						//charSetCodeでBaseJsonRequest#responseを文字コード設定?
						//TODO						
						if(_baseJson.response != null && _baseJson.response.length() > 0) {
							CharBuffer cbuff = CharBuffer.allocate(_baseJson.response.length());
							cbuff.position(0);
							cbuff.put(_baseJson.response);
							cbuff.position(0);
							_outbound.writeTextMessage(cbuff);
						}
					}else{
						String staticFilePath = getScriptletPath() + path;
						//実ファイルの存在チェック
						File staticFile = new File(staticFilePath);
						if(staticFile.exists()) {
							//SITEMAPに追加
							NodeList siteNodes = sitemapXML.getElementsByTagName("site");
							if(siteNodes != null && siteNodes.getLength() > 0) {
								Element siteNode = (Element)siteNodes.item(0);
								NodeList pageNodes = sitemapXML.getElementsByTagName("page");
								boolean newPath = true;
								for(int i=0;i<pageNodes.getLength();i++) {
									Element pageNode = (Element)pageNodes.item(i);
									if(pageNode.hasAttribute("path") && pageNode.getAttribute("path").equals(path)) {
										//SITEMAPに登録済
										newPath = false;
									}
								}
								if(newPath) {
									Element pageNode = sitemapXML.createElement("page");
									pageNode.setAttribute("path", path);
									siteNode.appendChild(pageNode);
								}
							}
							if(staticFilePath.endsWith(extendName)) {
								scriptlet = buildScriptlet(path);
								_baseJson.clearResult();
								scriptlet.setServlet(_wsScriptlet);
								scriptlet.start(_baseJson, !isRequest);
								//正常
								//charSetCodeでBaseJsonRequest#responseを文字コード設定?
								//TODO
								if(_baseJson.response != null && _baseJson.response.length() > 0) {
									CharBuffer cbuff = CharBuffer.allocate(_baseJson.response.length());
									cbuff.position(0);
									cbuff.put(_baseJson.response);
									cbuff.position(0);
									_outbound.writeTextMessage(cbuff);
								}
							}else{
								//エラーJSON
								String errMsg = "{\"status\":\"99\", \"reason\":\"not found script(extended)\"}";
								CharBuffer cbuff = CharBuffer.allocate(errMsg.length());
								cbuff.position(0);
								cbuff.put(errMsg);
								cbuff.position(0);
								_outbound.writeTextMessage(cbuff);
							}
						}else{
							//エラーJSON
							String errMsg = "{\"status\":\"99\", \"reason\":\"not found script\"}";
							CharBuffer cbuff = CharBuffer.allocate(errMsg.length());
							cbuff.position(0);
							cbuff.put(errMsg);
							cbuff.position(0);
							_outbound.writeTextMessage(cbuff);
						}
					}
				}else{
					if(isRequest) {
						//エラーJSON
						String errMsg = "{\"status\":\"99\", \"reason\":\"no path paramater\"}";
						CharBuffer cbuff = CharBuffer.allocate(errMsg.length());
						cbuff.position(0);
						cbuff.put(errMsg);
						cbuff.position(0);
						_outbound.writeTextMessage(cbuff);
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace(System.err);
				//エラーJSON
				String errMsg = "{\"status\":\"99\", \"reason\":\""+e.getMessage()+"\"}";
				CharBuffer cbuff = CharBuffer.allocate(errMsg.length());
				cbuff.position(0);
				cbuff.put(errMsg);
				cbuff.position(0);
				_outbound.writeTextMessage(cbuff);
			}
			finally {
			}
		}
	}

	protected class CallbackEventWatcher implements Runnable {

		@Override
		public void run() {
			for(int i=0;i<_inboundList.size();i++) {
				ScriptletServerInbound inBound = _inboundList.get(i);
				try {
					// TODO ここでスクリプトレットを呼び出す
					inBound.execScript(false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * サイトマップXMLを解析します。
	 * @throws SystemErrorException
	 */
	protected void setupSiteMap() throws SystemErrorException {
		try {
			String sitemapXMLPath = getRealPath() + sitemapPath;
			if(properties.getProperty(SITEMAP_PATH) != null && properties.getProperty(SITEMAP_PATH).length() > 2) {
				sitemapXMLPath = properties.getProperty(SITEMAP_PATH);
				File sitemapXMLFile = new File(sitemapXMLPath);
				if(sitemapXML == null || sitemapFileModify < 0 || sitemapFileModify != sitemapXMLFile.lastModified()){
					sitemapFileModify = sitemapXMLFile.lastModified();
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					sitemapXML = builder.parse(sitemapXMLPath);
				}
			}else{
				File sitemapXMLFile = new File(sitemapXMLPath);
				if(sitemapXML == null || sitemapFileModify < 0 || sitemapFileModify != sitemapXMLFile.lastModified()){
					sitemapFileModify = sitemapXMLFile.lastModified();
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					sitemapXML = builder.parse(sitemapXMLPath);
				}
			}
			if(sitemapXML == null) {
				throw new SystemErrorException("SITE-MAP XML NOT FOUND <"+sitemapXMLPath+">.");
			}else{
				NodeList siteNodes = sitemapXML.getElementsByTagName("site");
				if(siteNodes != null && siteNodes.getLength() > 0) {
					Element siteNode = (Element)siteNodes.item(0);
					if(siteNode.hasAttribute("OUT_OF_SERVICE") && siteNode.getAttribute("OUT_OF_SERVICE").length() > 1) {
						outOfServer = true;
					}
					if(siteNode.hasAttribute("wsclass")) {
						defaultWSScriptClassName = siteNode.getAttribute("wsclass");
						return;
					}
				}
				throw new SystemErrorException("'SITE' ATTRIBUTE or 'CLASS' ATTRIBUTE NOT FOUND IN SITE_MAP XML <"+sitemapXMLPath+">.");
			}
		}
		catch(SystemErrorException e) {
			throw e;
		}
		catch(java.lang.NullPointerException e){
			StringBuffer stack = new StringBuffer();
			StackTraceElement stes[] = e.getStackTrace();
			for(StackTraceElement ste : stes) {
				stack.append(String.format("%s.%s(%d)\n", ste.getClassName(), ste.getMethodName(), ste.getLineNumber()));
			}
			throw new SystemErrorException(stack.toString());
		}
		catch(Exception e) {
			throw new SystemErrorException(e);
		}
	}

	/**
	 * 
	 * @throws SystemErrorException
	 */
	protected void setupDBObject() throws SystemErrorException {
		try {
			if(useDBObject()) {
				String dbConfigXMLPath = getRealPath() + dbConfigPath;
				if(properties.getProperty(DBCONFIG_PATH) != null && properties.getProperty(DBCONFIG_PATH).length() > 2) {
					dbConfigXMLPath = properties.getProperty(DBCONFIG_PATH);
				}
				dbaccesser = new DBAccesser(new FileInputStream(dbConfigXMLPath));
			}
		}
		catch(Exception e) {
			throw new SystemErrorException(e);
		}
	}
	
	/**
	 * DBアクセスオブジェクトが利用可能か検証する。
	 * @return
	 */
	public boolean useDBObject() {
		if(properties.getProperty(DBOBJECT_CONTROL) != null && Boolean.parseBoolean(properties.getProperty(DBOBJECT_CONTROL))) {
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return
	 */
	public DBAccesser getDBAccessObject() {
		return dbaccesser;
	}
	
	/**
	 * 
	 * @throws SystemErrorException
	 */
	protected void setupMongoDBObject() throws SystemErrorException {
		try {
			if(useMongoDBObject()) {
				String mongoDBConfigXMLPath = getRealPath() + mongoDBConfigPath;
				if(properties.getProperty(MONGO_DBCONFIG_PATH) != null && properties.getProperty(MONGO_DBCONFIG_PATH).length() > 2) {
					mongoDBConfigXMLPath = properties.getProperty(MONGO_DBCONFIG_PATH);
				}
				mongoDBAccesser = new MongoDBAccesser(new FileInputStream(mongoDBConfigXMLPath));
			}
		}
		catch(Exception e) {
			throw new SystemErrorException(e);
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean useMongoDBObject() {
		if(properties.getProperty(MONGO_DBOBJECT_CONTROL) != null && Boolean.parseBoolean(properties.getProperty(MONGO_DBOBJECT_CONTROL))) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return
	 */
	public MongoDBAccesser getMongoDBAccesser() {
		return mongoDBAccesser;
	}
	
	/**
	 * @throws IllegalAccessException
	 * @throws SystemErrorException 
	 */
	protected void initialize() throws IllegalAccessException, SystemErrorException{
		if(properties.getProperty("force_content_length") != null &&
			properties.getProperty("force_content_length").toLowerCase().equals("true")) {
			ESEngine.forceContentLengthHeader = true;
		}
		extendName = properties.getProperty(EXT_NAME);
		if(extendName == null) {
			extendName = new String();
		}
		setupSiteMap();
		setupDBObject();
		setupMongoDBObject();
	}

	/**
	 * for Jetty Server
	 * @return
	 */
	protected String getRealPath() {
		String realPath = getServletContext().getRealPath("/");
		if(!realPath.endsWith("/")) {
			realPath += "/";
		}
		return realPath;
	}
	
	/**
	 * Scriptletを解決します。
	 * @return
	 * @throws SystemErrorException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	protected WSScriptlet buildScriptlet(String path) throws ClassNotFoundException, InstantiationException, java.lang.IllegalAccessException {
		NodeList pageNodes = sitemapXML.getElementsByTagName("page");
		for(int i=0;i<pageNodes.getLength();i++) {
			Element pageNode = (Element)pageNodes.item(i);
			if(pageNode.hasAttribute("path") && pageNode.getAttribute("path").equals(path)) {
				if(pageNode.hasAttribute("class")) {
					String className = pageNode.getAttribute("class");
					Class<?> loadClass = this.getClass().getClassLoader().loadClass(className);
					Object scriptlet = (Object)loadClass.newInstance();
					if (scriptlet instanceof WSScriptlet) {
						return (WSScriptlet) scriptlet;
					}
				}
				Class<?> loadClass = this.getClass().getClassLoader().loadClass(defaultWSScriptClassName);
				Object scriptlet = (Object)loadClass.newInstance();
				if (scriptlet instanceof WSScriptlet) {
					return (WSScriptlet) scriptlet;
				}
			}
		}
		return null;
	}

	/**
	 * スクリプトレットのパスを取得します。
	 * オーバーライドすることで、任意のパス構成を構築できます。
	 * 例）getScriptletPath(String any)でオーバーロードしてanyをsuper#getScriptletPathに付加する
	 * @return
	 */
	public String getScriptletPath() {
		String path = getRealPath() + scriptletPath;
		
		if(properties.getProperty(SCRIPTLET_PATH) != null && properties.getProperty(SCRIPTLET_PATH).length() > 2) {
			path = properties.getProperty(SCRIPTLET_PATH);
		}
		if(path.endsWith(File.separator)) {
			path = path.substring(0, path.lastIndexOf(File.separator));
		}
		return path;
	}
	
	/**
	 * 設定ファイルに指定した、スクリプトレットの拡張子を応答する。
	 * @return
	 */
	public String getScriptExtName() {
		return extendName;
	}
	
	/**
	 * 
	 * @param charset
	 */
	public void setCharacterEncoding(String charset) {
		charSetCode = charset;
	}

	/**
	 * 
	 */
	@Override
	protected StreamInbound createWebSocketInbound(String arg0, HttpServletRequest arg1) {
		FileInputStream fisConfig = null;
		try {
			String paramPath = getRealPath()+"WEB-INF"+File.separator+"config.xml";
			fisConfig = new FileInputStream(paramPath);
			properties.loadFromXML(fisConfig);
			initialize();
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
		}
		finally {
			if(fisConfig!=null) {
				try {
					fisConfig.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new ScriptletServerInbound(this);
	}
	
	public void destroy() {
		if(timeHandle != null) {
			timeHandle.cancel(true);
			timeHandle = null;
		}
		try {
			if(scheduler != null) {
				scheduler.shutdownNow();
				scheduler.awaitTermination(10, SECONDS); //shutdown wating 10 sec
				scheduler = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}
}
