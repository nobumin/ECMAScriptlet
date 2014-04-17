package info.dragonlady.websocket;

import info.dragonlady.scriptlet.ESEngine;
import info.dragonlady.scriptlet.IllegalAccessException;
import info.dragonlady.scriptlet.WSScriptlet;
import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.MongoDBAccesser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@ServerEndpoint("/{subpath}")
public class WebsocketServer {
	private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
	private String SCRIPTLET_PATH = "scriptlet_path";
	private String SITEMAP_PATH = "sitemap";
	private String DBCONFIG_PATH = "dbconfig";
	private String DBOBJECT_CONTROL = "dbobject";
	private String MONGO_DBOBJECT_CONTROL = "mongodbject";
	private String MONGO_DBCONFIG_PATH = "mongodbconfig";
	private String EXT_NAME = "extendName";

	private Properties properties = new Properties();
	private long sitemapFileModify = -1;
	private String charSetCode = "utf-8";
	private boolean outOfServer = false;
	private boolean isFirstTime = true;
	
	protected String scriptletPath = "WEB-INF"+File.separator+"scriptlet"+File.separator;
	protected String sitemapPath = "WEB-INF"+File.separator+"sitemap.xml";
	protected String dbConfigPath = "WEB-INF"+File.separator+"db_config.xml";
	protected String mongoDBConfigPath = "WEB-INF"+File.separator+"mongodb.xml";
	protected Document sitemapXML = null;
	protected DBAccesser dbaccesser = null;
	protected MongoDBAccesser mongoDBAccesser = null;
	protected String defaultWSScriptClassName = null;
	protected String extendName = null;
	
	@OnOpen
	public void onOpen(Session session, EndpointConfig config, @PathParam("subpath")String subPath) {
		if(isFirstTime) {
			try {
				initialize(session);
				isFirstTime = false;
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		if(peers.contains(session)) {
			peers.remove(session);
		}
		peers.add(session);
		try {
			execScript(session, WSScriptlet.WS_STATUS.OPEN, null, null);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	@OnClose
	public void onClose(Session session, CloseReason reason, @PathParam("subpath")String subPath) {
		if(peers.contains(session)) {
			peers.remove(session);
		}
		try {
			execScript(session, WSScriptlet.WS_STATUS.CLOSE, null, null);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	@OnError
	public void onError(Session session, Throwable t, @PathParam("subpath")String subPath) {
		try {
			execScript(session, WSScriptlet.WS_STATUS.ERROR, null, null);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	@OnMessage
	public void onMesage(String message, Session session, @PathParam("subpath")String subPath) {
		Map<String, Object> userProp = session.getUserProperties();
		userProp.put("userdata", message);
		try {
			execScript(session, WSScriptlet.WS_STATUS.EXEC, null, null);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}

	@OnMessage
	public void onMesage(ByteBuffer buffer, boolean last, Session session, @PathParam("subpath")String subPath) {
//System.out.println(last?"LAST!!":"NOT LAST");
		Map<String, Object> userProp = session.getUserProperties();
		userProp.put("userdata", buffer);
		try {
			execScript(session, WSScriptlet.WS_STATUS.EXEC, null, null);
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	protected void execScript(Session session, WSScriptlet.WS_STATUS status, String message, ByteBuffer data) throws SystemErrorException {
		try {
			String path = requestScriptlet(session);
			WSScriptlet scriptlet = buildScriptlet(path, session);
			scriptlet.setServlet(this);
			scriptlet.start(path, session, status);
		}
		catch(Exception e) {
			throw new SystemErrorException(e);
		}
	}
	
	protected String requestScriptlet(Session session) {
		Map<String, List<String>> reqMap = session.getRequestParameterMap();
		Iterator<String>keys = reqMap.keySet().iterator();
		while(keys.hasNext()) {
			String key = keys.next();
			if(key.equals("script")) {
				List<String> value = reqMap.get(key);
				if(value.size() > 0) {
					return reqMap.get(key).get(0);
				}
			}
		}
		return null;
	}

	/**
	 * サイトマップXMLを解析します。
	 * @throws SystemErrorException
	 */
	protected void setupSiteMap(Session session) throws SystemErrorException {
		try {
			String sitemapXMLPath = getRealPath(session) + sitemapPath;
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
	protected void setupDBObject(Session session) throws SystemErrorException {
		try {
			if(useDBObject()) {
				String dbConfigXMLPath = getRealPath(session) + dbConfigPath;
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
	protected void setupMongoDBObject(Session session) throws SystemErrorException {
		try {
			if(useMongoDBObject()) {
				String mongoDBConfigXMLPath = getRealPath(session) + mongoDBConfigPath;
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
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidPropertiesFormatException 
	 */
	protected void initialize(Session session) throws IllegalAccessException, SystemErrorException, URISyntaxException, InvalidPropertiesFormatException, FileNotFoundException, IOException{

		String paramPath = getRealPath(session)+"WEB-INF"+File.separator+"config.xml";
		properties.loadFromXML(new FileInputStream(paramPath));
		
		if(properties.getProperty("force_content_length") != null &&
			properties.getProperty("force_content_length").toLowerCase().equals("true")) {
			ESEngine.forceContentLengthHeader = true;
		}
		extendName = properties.getProperty(EXT_NAME);
		if(extendName == null) {
			extendName = new String();
		}
		setupSiteMap(session);
		setupDBObject(session);
		setupMongoDBObject(session);
	}

	/**
	 * @return
	 * @throws URISyntaxException 
	 * @throws MalformedURLException 
	 */
	protected String getRealPath(Session session) throws URISyntaxException, MalformedURLException {
		String realPath = "";
		File binPath = new File(new File("").getAbsolutePath());
		if(binPath.isDirectory()) {
			String homePath = binPath.getParentFile().getAbsolutePath();
			if(!homePath.endsWith("/")) {
				homePath += "/";
			}
			String appPath = session.getRequestURI().getPath();
			realPath = String.format("%swebapps/%s", homePath, appPath.substring(0, appPath.lastIndexOf("/")));
		}
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
	protected WSScriptlet buildScriptlet(String path, Session session) throws SystemErrorException {
		if(path != null && path.length() > 0) {
			try {
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
				String staticFilePath = getScriptletPath(session) + path;
				//実ファイルの存在チェック
				File staticFile = new File(staticFilePath);
				if(staticFile.exists()) {
					//SITEMAPに追加
					NodeList siteNodes = sitemapXML.getElementsByTagName("site");
					if(siteNodes != null && siteNodes.getLength() > 0) {
						Element siteNode = (Element)siteNodes.item(0);
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
						Class<?> loadClass = this.getClass().getClassLoader().loadClass(defaultWSScriptClassName);
						Object scriptlet = (Object)loadClass.newInstance();
						if (scriptlet instanceof WSScriptlet) {
							return (WSScriptlet) scriptlet;
						}
					}else{
						throw new SystemErrorException("illegal extention scripte("+staticFilePath+") not found !");
					}
				}else{
					throw new SystemErrorException("scripte("+staticFilePath+") not found !");
				}		
			}
			catch(Exception e) {
				throw new SystemErrorException(e);
			}
		}
		throw new SystemErrorException("'scripte' Parameter not found !");
	}

	/**
	 * スクリプトレットのパスを取得します。
	 * オーバーライドすることで、任意のパス構成を構築できます。
	 * 例）getScriptletPath(String any)でオーバーロードしてanyをsuper#getScriptletPathに付加する
	 * @param session
	 * @return
	 */
	public String getScriptletPath(Session session) {
		String path=null;
		try {
			path = getRealPath(session) + scriptletPath;
			if(properties.getProperty(SCRIPTLET_PATH) != null && properties.getProperty(SCRIPTLET_PATH).length() > 2) {
				path = properties.getProperty(SCRIPTLET_PATH);
			}
			if(!path.endsWith(File.separator)) {
				path += File.separator;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return path;
	}
	
	/**
	 * 
	 * @param context
	 * @return
	 */
	public String getScriptletPath(ServletContext context) {
		String realPath = context.getRealPath("/");
		if(!realPath.endsWith("/")) {
			realPath += "/";
		}
		
		String path = realPath+scriptletPath;
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
	
	public void log(String message) {
		System.out.println(message);
	}
	
	public void log(String message, Throwable t) {
		System.out.println(message);
		t.printStackTrace(System.err);
	}
}
