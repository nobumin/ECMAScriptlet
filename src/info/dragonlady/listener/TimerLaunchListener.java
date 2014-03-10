package info.dragonlady.listener;

import static java.util.concurrent.TimeUnit.SECONDS;
import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.MongoDBAccesser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import info.dragonlady.scriptlet.BaseJsonRequest;
import info.dragonlady.scriptlet.ESEngine;
import info.dragonlady.scriptlet.SystemErrorException;
import info.dragonlady.scriptlet.WSScriptlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class TimerLaunchListener extends WSScriptlet implements ServletContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1217378314744733220L;

	private String SITEMAP_PATH = "sitemap";
	private String DBCONFIG_PATH = "dbconfig";
	private String DBOBJECT_CONTROL = "dbobject";
	private String MONGO_DBOBJECT_CONTROL = "mongodbject";
	private String MONGO_DBCONFIG_PATH = "mongodbconfig";
	private String EXT_NAME = "extendName";
	private String BATCH_PATH = "batchPath";

	protected String sitemapPath = "WEB-INF"+File.separator+"sitemap.xml";
	protected String dbConfigPath = "WEB-INF"+File.separator+"db_config.xml";
	protected String mongoDBConfigPath = "WEB-INF"+File.separator+"mongodb.xml";
	protected String scriptletPath = "WEB-INF"+File.separator+"scriptlet"+File.separator;
	protected String batchPath = scriptletPath+"batch"+File.separator; //スクリプトレット下のbatchディレクトリ
	protected Document sitemapXML = null;
	protected DBAccesser dbaccesser = null;
	protected MongoDBAccesser mongoDBAccesser = null;
	
	protected String extendName = null;
	private Properties properties = new Properties();
	private long sitemapFileModify = -1;
	private ServletContextEvent servletContextEvent= null;
	private HashMap<String, BaseJsonRequest>jsonMap = new HashMap<String, BaseJsonRequest>();

	private static ScheduledExecutorService scheduler = null;
	private static ScheduledFuture<?> timeHandle = null;
	
	protected class EventWatcher implements Runnable {
		WSScriptlet scriptlet = null;
		
		private EventWatcher(WSScriptlet scriptlet) {
			this.scriptlet = scriptlet;
		}
		
		private class FileAcceptFilter implements FilenameFilter {
			public boolean accept(File dir, String name) {
				if(name.endsWith(getScriptExtName())) {
					String path = dir.getAbsolutePath();
					if(path.endsWith(File.separator)) {
						if(name.startsWith(File.separator)) {
							path = path + name.substring(1);
						}else{
							path = path + name;
						}
					}else{
						if(name.startsWith(File.separator)) {
							path = path + name;
						}else{
							path = path + File.separator +name;
						}
					}
					File checkFir = new File(path);
					if(!checkFir.isDirectory()) {
						return true;
					}
				}
				
				return false;
			}
		}

		@Override
		public void run() {
			try {
				String batchDir = getScriptletPath();
				File baseDir = new File(batchDir);
				if(baseDir.exists() && baseDir.isDirectory()) {
					File scripts[] = baseDir.listFiles(new FileAcceptFilter());
					Vector<String> paths = new Vector<String>();
					for(int i=0;i<scripts.length;i++) {
						if(!jsonMap.containsKey(scripts[i].getName())) {
							BaseJsonRequest baseJson = new BaseJsonRequest();
							baseJson.path = "";
							baseJson.excute = File.separator+scripts[i].getName();
							baseJson.response = "";
							baseJson.query = "";
							baseJson.session = null;
							jsonMap.put(scripts[i].getName(), baseJson);
						}
						paths.add(scripts[i].getName());
						try {
							ESEngine.executeScript(scriptlet, jsonMap.get(scripts[i].getName()), true);
						}
						catch(Exception e) {
							e.printStackTrace(System.err);
						}
					}
					Iterator<String> it = jsonMap.keySet().iterator();
					while(it.hasNext()) {
						String key = it.next();
						if(!paths.contains(key)) {
							jsonMap.remove(key);
						}
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace(System.out);
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
	 * 
	 */
	protected void launchTimer() {
		if(scheduler == null) {
			scheduler = Executors.newSingleThreadScheduledExecutor();
			if(timeHandle != null) {
				timeHandle.cancel(true);
			}
			EventWatcher watcher = new EventWatcher(this);
			timeHandle = scheduler.scheduleAtFixedRate(watcher, 1, 1, SECONDS);
		}
	}
	
	/**
	 * @throws IllegalAccessException
	 * @throws SystemErrorException 
	 */
	protected void initialize() throws IllegalAccessException, SystemErrorException{
		FileInputStream fisConfig = null;
		try {
			String paramPath = getRealPath()+"WEB-INF"+File.separator+"config.xml";
			fisConfig = new FileInputStream(paramPath);
			properties.loadFromXML(fisConfig);
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
			launchTimer();
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
	}

	/**
	 * for Jetty Server
	 * @return
	 */
	protected String getRealPath() {
		String realPath = servletContextEvent.getServletContext().getRealPath("/");
		if(!realPath.endsWith("/")) {
			realPath += "/";
		}
		return realPath;
	}
	
	/**
	 * 
	 */
	public String getScriptletPath() {
		String path = getRealPath() + batchPath;
		
		if(properties.getProperty(BATCH_PATH) != null && properties.getProperty(BATCH_PATH).length() > 2) {
			path = properties.getProperty(BATCH_PATH);
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

	@Override
	public void contextDestroyed(ServletContextEvent contextEvent) {
		System.out.println("contextDestroyed ecmascriplet");
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

	@Override
	public void contextInitialized(ServletContextEvent contextEvent) {
		System.out.println("contextInitialized ecmascriplet");
		servletContextEvent = contextEvent;
		try {
			initialize();
		}
		catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public String getServletName() {
		return "TimerLaunchListener";
	}
	
	/**
	 * SecureServletの同名ラッパー関数
	 * @return
	 */
	public ServletContext getServletContext() {
		return servletContextEvent.getServletContext();
	}

	/**
	 * 文字コードを変更します。
	 * @param code
	 */
	public void setCharSet(String code) {
		charset = code;
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
		//暫定
		System.out.println(msg);
	}
	
	/**
	 * GenericServletの同名のラッパー関数
	 * @param message
	 * @param t
	 */
	public void log(String message, Throwable t) {
		//暫定
		System.out.println(message);
		t.printStackTrace(System.out);
	}
	
	@Override
	public void start(BaseJsonRequest json, boolean isCallback)
			throws SystemErrorException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getScriptNewProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequiredParamErrorMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInvalidParamErrorMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEScriptErrorMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInvalidValidationParamErrorMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSerialVersionUID() {
		// TODO Auto-generated method stub
		return 0;
	}

}
