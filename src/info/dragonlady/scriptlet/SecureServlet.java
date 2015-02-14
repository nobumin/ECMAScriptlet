package info.dragonlady.scriptlet;

import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.MongoDBAccesser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 処理シーケンスの妥当性検証を実装した、javax.servlet.http.HttpServletの継承クラスです。
 * Scriptletクラスの基底クラスで、このクラスを直接継承することはありません。
 * @author nobu
 *
 */
public class SecureServlet extends HttpServlet {
	static public enum AccessForScript {
		EXEC_SCRIPT,
		LOAD_STATIC,
		SEND_REDIRECT,
		NOT_FOUND,
		OUT_OF_SERVICE,
	}

	private static final long serialVersionUID = -1783518376805871958L;
//	private static final String DEFAULT_CHARSET = "Shift-jis";
//	private static final String DEFAULT_CONTENT_TYPE = "text/html";
	private String SCRIPTLET_PATH = "scriptlet_path";
	private String SITEMAP_PATH = "sitemap";
	private String DBCONFIG_PATH = "dbconfig";
	private String DBOBJECT_CONTROL = "dbobject";
	private String MONGO_DBOBJECT_CONTROL = "mongodbject";
	private String MONGO_DBCONFIG_PATH = "mongodbconfig";
	private String EXT_NAME = "extendName";
	private String ERROR_PAGE = "common_error";
	private String OUT_OF_SERVICE = "out_of_service";
	protected AccessForScript accessType = AccessForScript.NOT_FOUND; 
//	protected String charset = DEFAULT_CHARSET;
//	protected String contentType = DEFAULT_CONTENT_TYPE;
	protected String scriptletPath = "WEB-INF"+File.separator+"scriptlet"+File.separator;
	protected String sitemapPath = "WEB-INF"+File.separator+"sitemap.xml";
	protected String dbConfigPath = "WEB-INF"+File.separator+"db_config.xml";
	protected String mongoDBConfigPath = "WEB-INF"+File.separator+"mongodb.xml";
	protected Document sitemapXML = null;
	protected String defaultScriptClassName = null;
	protected DBAccesser dbaccesser = null;
	protected MongoDBAccesser mongoDBAccesser = null;
	protected String extendName = null;
	private Properties properties = new Properties();
	private long sitemapFileModify = -1;
	private boolean outOfServer = false;
	
	/**
	 * サイトマップXMLを解析します。
	 * @throws SystemErrorException
	 */
	protected void setupSiteMap(HttpSession session) throws SystemErrorException {
		try {
			String sitemapXMLPath = getRealPath() + sitemapPath;
			if(properties.getProperty(SITEMAP_PATH) != null && properties.getProperty(SITEMAP_PATH).length() > 2) {
				sitemapXMLPath = properties.getProperty(SITEMAP_PATH);
				File sitemapXMLFile = new File(sitemapXMLPath);
				if(sitemapXML == null || sitemapFileModify < 0 || sitemapFileModify != sitemapXMLFile.lastModified()){
					sitemapFileModify = sitemapXMLFile.lastModified();
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
//					sitemapXML = builder.parse(sitemapXMLPath);
					sitemapXML = builder.parse(sitemapXMLFile); //2014.02.06 sitemapXMLPathだと、Parallel deploymentでパースエラー
				}
			}else{
				File sitemapXMLFile = new File(sitemapXMLPath);
				if(sitemapXML == null || sitemapFileModify < 0 || sitemapFileModify != sitemapXMLFile.lastModified()){
					sitemapFileModify = sitemapXMLFile.lastModified();
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
//					sitemapXML = builder.parse(sitemapXMLPath);
					sitemapXML = builder.parse(sitemapXMLFile); //2014.02.06 sitemapXMLPathだと、Parallel deploymentでパースエラー
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
					if(siteNode.hasAttribute("class")) {
						defaultScriptClassName = siteNode.getAttribute("class");
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
	protected void initialize(HttpServletRequest req, HttpServletResponse res) throws IllegalAccessException, SystemErrorException{
		if(properties.getProperty("force_content_length") != null &&
			properties.getProperty("force_content_length").toLowerCase().equals("true")) {
			ESEngine.forceContentLengthHeader = true;
		}
		extendName = properties.getProperty(EXT_NAME);
		if(extendName == null) {
			extendName = new String();
		}
//		setContentType(DEFAULT_CONTENT_TYPE);
		setupSiteMap(req.getSession());
		setupDBObject();
		setupMongoDBObject();
		verifyRequest(req);
	}
	
	/**
	 * Scriptletを解決します。
	 * @return
	 * @throws SystemErrorException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	protected Scriptlet buildScriptlet(HttpServletRequest req) throws SystemErrorException, ClassNotFoundException, InstantiationException, java.lang.IllegalAccessException {
		String myself = getRelativePath(req).replaceAll("//", "/");
		NodeList pageNodes = sitemapXML.getElementsByTagName("page");
		for(int i=0;i<pageNodes.getLength();i++) {
			Element pageNode = (Element)pageNodes.item(i);
			if(pageNode.hasAttribute("path") && pageNode.getAttribute("path").equals(myself)) {
				if(pageNode.hasAttribute("class")) {
					String className = pageNode.getAttribute("class");
					Class<?> loadClass = this.getClass().getClassLoader().loadClass(className);
					Object scriptlet = (Object)loadClass.newInstance();
					if (scriptlet instanceof Scriptlet) {
						return (Scriptlet) scriptlet;
					}
				}
				Class<?> loadClass = this.getClass().getClassLoader().loadClass(defaultScriptClassName);
				Object scriptlet = (Object)loadClass.newInstance();
				if (scriptlet instanceof Scriptlet) {
					return (Scriptlet) scriptlet;
				}
			}
		}
		throw new SystemErrorException(String.format("Scriptlet not found(%s)", myself));
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
	 * {@link HttpServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 */
	protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException{
		FileInputStream fisConfig = null;
		try {
			String paramPath = getRealPath()+"WEB-INF"+File.separator+"config.xml";
			fisConfig = new FileInputStream(paramPath);
			properties.loadFromXML(fisConfig);
			initialize(req, res);
			
			if(accessType == AccessForScript.LOAD_STATIC) {//スクリプト以外の要求
				String staticFilePath = getScriptletPath() + getRelativePath(req);
				File staticFile = new File(staticFilePath);
				if(ESEngine.forceContentLengthHeader){
					res.setContentLength((int)staticFile.length());
				}
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(staticFile));
				BufferedOutputStream bos = new BufferedOutputStream(res.getOutputStream());
				int readLen = 0;
				byte readBuffer[] = new byte[2048];
				while((readLen = bis.read(readBuffer)) > 0) {
					bos.write(readBuffer, 0, readLen);
					bos.flush();
				}
				bos.flush();
				bis.close();
			}else if(accessType == AccessForScript.SEND_REDIRECT) {//リダイレクション要求
				res.sendRedirect(req.getContextPath()+"/");
			}else if(accessType == AccessForScript.EXEC_SCRIPT) {//スクリプト
				Scriptlet scriptlet = buildScriptlet(req);
				scriptlet.setServlet(this, req, res);
				scriptlet.start();
			}else{
				String requestPath = getRelativePath(req);
				String staticFilePath = getScriptletPath() + requestPath;
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
							if(pageNode.hasAttribute("path") && pageNode.getAttribute("path").equals(requestPath)) {
								//SITEMAPに登録済
								newPath = false;
							}
						}
						if(newPath) {
							Element pageNode = sitemapXML.createElement("page");
							pageNode.setAttribute("path", getRelativePath(req));
							siteNode.appendChild(pageNode);
						}
					}
					//
					//TODO この後例外になることがある
					//
					if(staticFilePath.endsWith(extendName)) {
						Scriptlet scriptlet = buildScriptlet(req);
						scriptlet.setServlet(this, req, res);
						scriptlet.start();
					}else{
						if(ESEngine.forceContentLengthHeader){
							res.setContentLength((int)staticFile.length());
						}
						BufferedInputStream bis = new BufferedInputStream(new FileInputStream(staticFile));
						BufferedOutputStream bos = new BufferedOutputStream(res.getOutputStream());
						int readLen = 0;
						byte readBuffer[] = new byte[2048];
						while((readLen = bis.read(readBuffer)) > 0) {
							bos.write(readBuffer, 0, readLen);
							bos.flush();
						}
						bos.flush();
						bis.close();
					}
				}else{
					res.sendError(404, String.format("NOT FOUND PATH(%s)", getRelativePath(req)));
				}
			}
		}
		catch(SystemErrorException e) {
			e.printStackTrace(System.out);
			res.sendError(500, e.getMessage());
		}
		catch(IOException e) {
			e.printStackTrace(System.out);
			res.sendError(404, e.getMessage());
		}
		catch(Exception e) {
			e.printStackTrace(System.out);
			res.sendError(404, e.getMessage());
		}
		finally {
			if(fisConfig!=null) {
				fisConfig.close();
			}
		}
	}
	
	/**
	 * サイトマップXMLにてシーケンスの検証を行なう
	 * @param sessionSeqVal
	 * @param sequenceValue
	 * @return
	 */
	protected boolean checkSitemap(String sessionSeqVal, String sequenceValue) {
		NodeList pageNodes = sitemapXML.getElementsByTagName("page");
		for(int i=0;i<pageNodes.getLength();i++) {
			Element pageNode = (Element)pageNodes.item(i);
			if(pageNode.hasAttribute("path") && pageNode.getAttribute("path").equals(sequenceValue)) {
				Element parentNode = (Element)pageNode.getParentNode();
				//forward
				if(parentNode.hasAttribute("path") && parentNode.getAttribute("path").equals(sessionSeqVal)) {
					return true;
				}
				//reload or same process(ex page scroll)
				if(sequenceValue.equals(sessionSeqVal)) {
					return true;
				}
				//backward
				if(pageNode.hasAttribute("backward") && Boolean.parseBoolean(pageNode.getAttribute("backward"))){
					NodeList children = pageNode.getChildNodes();
					for(int j=0;j<children.getLength();j++) {
						if(children.item(j).getNodeType() == Node.ELEMENT_NODE) {
							Element child = (Element)children.item(j);
							if(child.hasAttribute("path") && child.getAttribute("path").equals(sessionSeqVal)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	protected boolean checkSitemap(String sequenceValue) {
		NodeList pageNodes = sitemapXML.getElementsByTagName("page");
		for(int i=0;i<pageNodes.getLength();i++) {
			Element pageNode = (Element)pageNodes.item(i);
			if(pageNode.hasAttribute("path") && pageNode.getAttribute("path").equals(sequenceValue)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 要求パスを検証する。
	 * @param req
	 */
	protected void verifyRequest(HttpServletRequest req) throws IllegalAccessException {
		if(outOfServer) {
			accessType = AccessForScript.OUT_OF_SERVICE;
		}else{
			if(req.getRequestURI().endsWith(req.getContextPath()) && !req.getRequestURI().endsWith("/")) { //
				accessType = AccessForScript.SEND_REDIRECT;
			}else{
				String requestPath = getRequestPath(req);
				if(requestPath != null) {
					if(!requestPath.endsWith(extendName)) {
						accessType = AccessForScript.LOAD_STATIC;
					}else{
						accessType = AccessForScript.EXEC_SCRIPT;
					}
				}else{
					accessType = AccessForScript.NOT_FOUND;
				}
			}
		}
	}
		
	
	/**
	 * 相対パスを取得する。
	 * @return
	 */
	protected String getRelativePath(HttpServletRequest req) {
		if(accessType == AccessForScript.OUT_OF_SERVICE) {//サービス停止中
			return properties.getProperty(OUT_OF_SERVICE);
		}
		
		String path = req.getRequestURI();
		String contextPath = req.getContextPath();
		if(path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		if(path.lastIndexOf(";jsessionid") > 0) {
			path = path.substring(0, path.lastIndexOf(";jsessionid"));
		}
		if(path == null || path.length() < 2) {
			path = ESEngine.defaultScriptletName+getScriptExtName();
		}
		return path;
	}
	
	/**
	 * 要求パスとサイトマップを比較して相対パスを抽出する。
	 * @param req
	 * @return
	 */
	public String getRequestPath(HttpServletRequest req) {
		String myself = getRelativePath(req);
		NodeList pageNodes = sitemapXML.getElementsByTagName("page");
		for(int i=0;i<pageNodes.getLength();i++) {
			Element pageNode = (Element)pageNodes.item(i);
			if(pageNode.hasAttribute("path") && pageNode.getAttribute("path").equals(myself)) {
				return myself;
			}
		}
		if(!myself.endsWith(extendName)) {
			return myself;
		}
		
		return null;
	}

	/**
	 * HttpSessionクラスのインスタンスを応答します。
	 * @return：HttpSessionクラスのインスタンス
	 */
	public HttpSession getSession(HttpSession session) {
		return session;
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
	 * 共通エラースクリプトの応答
	 * @return
	 */
	public String getCommonErrorScript() {
		return properties.getProperty(ERROR_PAGE);
	}
}
