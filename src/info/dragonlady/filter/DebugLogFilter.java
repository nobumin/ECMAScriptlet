package info.dragonlady.filter;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

/**
 * サーブレットフィルター定義で使用するデバッグ用ログ機能クラス
 * @author nobu
 */
public class DebugLogFilter implements Filter {
//	public static final int NO_LOG    = 0;
//	public static final int SHORT_LOG = 1;
//	public static final int MID_LOG   = 2;
//	public static final int FULL_LOG  = 3;
//	public static final int DEBUG_LOG = 4;
	private String smartLogs[] = {"getTimestamp", "stopTimer", "getRequestHeader", "getResponseHeader"};
	private String middleLogs[] = {"getTimestamp", "stopTimer", "getBeforeAttributes", "getAfterAttributes", "getRequestHeader", "getResponseHeader"};
	private String fullLogs[] = {"getTimestamp", "stopTimer", "getBeforeAttributes", "getAfterAttributes", "getRequestHeader", "getResponseHeader", "getRequest"};
	private String debugLogs[] = {"getTimestamp", "stopTimer", "getBeforeAttributes", "getAfterAttributes", "getRequestHeader", "getResponseHeader", "getRequest", "getResponse"};
	private List<String> targetLogs = null;
	private int responseLogMaxSize = 128;
	private long   startTime   = 0l;
	final private String logFilePrefix = "filterlog_";
	final private String logFileSuffix = ".log";
	protected LogLevel debugMode   = LogLevel.none;
	protected File logDirPath      = null;
	protected boolean logRotate    = true;
	protected int maxGeneration    = 7;
	protected String sessionBefore = null;
	protected HashMap<String, String> logInfoMap = new HashMap<String, String>();
	
	/**
	 * クラスで利用するログファイルを特定する為のファイル名フィルター
	 * @author nobu
	 */
	private class LogFilenameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if(name.startsWith(logFilePrefix) && name.endsWith(logFileSuffix)) {
				return true;
			}
			return false;
		}
	}

	/**
	 * デバッグモードで利用する際、HTTP応答（出力ストリーム）の内容をキャッシュする為の出力ストリーム拡張クラス
	 * @author nobu
	 */
	private class CachedServletOutputStream extends ServletOutputStream {
		private ByteArrayOutputStream baos = new ByteArrayOutputStream();
		private ServletOutputStream superOut = null;
		
		/**
		 * コンストラクター
		 * @param sos
		 */
		public CachedServletOutputStream(ServletOutputStream sos) {
			superOut = sos;
		}
		
		@Override
		public void write(int b) throws IOException {
			baos.write(b);
			superOut.write(b);
		}
		
		/**
		 * writeメソッドのオーバーロード
		 * @param c
		 * @throws IOException
		 */
		public void write(char c) throws IOException {
			baos.write(String.valueOf(c).getBytes());
			superOut.write(c);
		}
		
		/**
		 * writeメソッドのオーバーロード
		 * @param s
		 * @param off
		 * @param len
		 * @throws IOException
		 */
	    public void write(String s, int off, int len) throws IOException {
	    	for(int i=off;i<len;i++) {
	    		write(s.charAt(i));
	    	}
	    }

	    /**
	     * writeメソッドのオーバーロード
	     * @param buf
	     * @param off
	     * @param len
	     * @throws IOException
	     */
	    public void write(char buf[], int off, int len) throws IOException {
	    	for(int i=off;i<len;i++) {
	    		write(buf[i]);
	    	}
	    }
	    
	    /**
	     * キャッシュした出力ストリームのbyte配列を応答する。
	     * @return
	     */
	    public byte[] toByteArray() {
			return baos.toByteArray();
		}
	    
	    /**
	     * ServletOutputStreamのFlushを行う。
	     */
	    public void flush() throws IOException{
	    	superOut.flush();
	    }
	}
	
	/**
	 * HTTPレスポンスをキャッシュする為のHttpServletResponseのラッパークラス
	 * @author nobu
	 */
	private class DebuglogResponseWrapper extends HttpServletResponseWrapper {
		private HashMap<String, String> headerCache = new HashMap<String, String>();
		private CachedServletOutputStream csos = null;
		private PrintWriter defaultWriter = null;
		private StringWriter sw = null;
		private PrintWriter pw = null;
		private HttpServletResponse response = null;
		private int statusCode = 200;
		
		/**
		 * コンストラクター
		 * @param response
		 * @throws IOException
		 */
		public DebuglogResponseWrapper(HttpServletResponse response) throws IOException {
			super(response);
			this.response = response;
		}
		
		/**
		 * HTTP応答コードの設定
		 */
		public void setStatus(int sc) {
			super.setStatus(sc);
			statusCode = sc;
		}
		
		/**
		 * HTTP応答コードの設定
		 */
		public void setStatus(int sc, String sm) {
			super.setStatus(sc, sm);
			statusCode = sc;
		}
		
		/**
		 * キャッシュしたHTTPレスポンスの内容を応答する。
		 * @return
		 */
		public String getResult() {
			if(responseLogMaxSize < 1) {
				
				if(csos == null) {
					if(getWriterBuffer() == null) {
						return "null";
					}
					return getWriterBuffer().toString();
				}
				return new String(csos.toByteArray());
			}
			int length = 0;
			if(csos == null) {
				if(getWriterBuffer() == null) {
					return "null";
				}
				length = getWriterBuffer().toString().length() > responseLogMaxSize ? responseLogMaxSize : getWriterBuffer().toString().length();
				return getWriterBuffer().toString().substring(0, length);
			}
			length = csos.toByteArray().length > responseLogMaxSize ? responseLogMaxSize : csos.toByteArray().length;
			return new String(csos.toByteArray(), 0, length);
		}

		@Override
		public void addDateHeader(String name, long date) {
			headerCache.put(name, String.valueOf(date));
			super.addDateHeader(name, date);
		}
		
		@Override
		public void addHeader(String name, String value) {
			headerCache.put(name, value);
			super.addHeader(name, value);
		}
		
		@Override
		public void addIntHeader(String name, int value) {
			headerCache.put(name, String.valueOf(value));
			super.addIntHeader(name, value);
		}
		
		@Override
		public void setDateHeader(String name, long date) {
			headerCache.put(name, String.valueOf(date));
			super.setDateHeader(name, date);
		}
		
		@Override
		public void setHeader(String name, String value) {
			headerCache.put(name, value);
			super.setHeader(name, value);
		}
		
		@Override
		public void setIntHeader(String name, int value) {
			headerCache.put(name, String.valueOf(value));
			super.setIntHeader(name, value);
		}
		
		/**
		 * HTTPヘッダーの内容を応答する。
		 * @return 
		 */
		public String toString() {
			StringBuffer result = new StringBuffer("{");
			Iterator<String> it = headerCache.keySet().iterator();
			
			result.append("[RESULT_CODE=");
			result.append(statusCode+"]");
			if(it.hasNext()) {
				result.append(",");
			}
			while(it.hasNext()) {
				String key = it.next();
				String val = headerCache.get(key);
				result.append("["+key);
				result.append("=");
				result.append(val+"]");
				if(it.hasNext()) {
					result.append(",");
				}
			}
			result.append("}");
			return result.toString();
		}
		
		@Override
		public ServletOutputStream getOutputStream() throws IOException{
			if(debugMode != LogLevel.debug) {
				return response.getOutputStream();
			}
			csos = new CachedServletOutputStream(response.getOutputStream());
			return csos;
	    }
		
		/**
		 * 出力ストリームの内容をFLUSHする。
		 * @throws IOException
		 */
		public void writerFlush() throws IOException {
			if(debugMode != LogLevel.debug) {
				response.flushBuffer();
			}else{
				if(pw != null && sw != null) {
					pw.flush();
//					response.getOutputStream().print(getWriterBuffer().toString());
//					response.getOutputStream().flush();
					defaultWriter.write(getWriterBuffer().toString());
					defaultWriter.flush();
				}
			}
		}
		
		/**
		 * 出力ストリームの内容を応答する。
		 * ただし、DEBUGモードかつgetWriter()を利用して出力した内容のみ
		 * @return
		 */
		public StringBuffer getWriterBuffer() {
			if(sw != null) {
				return sw.getBuffer();
			}
			return null;
		}

		@Override
	    public PrintWriter getWriter() throws IOException{
			if(debugMode != LogLevel.debug) {
				return response.getWriter();
			}
			
	    	if(sw == null && pw == null) {
		    	sw = new StringWriter();
				pw = new PrintWriter(sw);
				defaultWriter = response.getWriter();
	    	}
			return pw;
		}
	}
	
	/**
	 * 呼び出し元の関数名を取得する。
	 * @return この関数を呼出した、元の関数名
	 */
	protected String getMthodName() {
		Exception e = new Exception();
		if(e.getStackTrace().length > 1) {
			StackTraceElement stack = e.getStackTrace()[1];
			return stack.getMethodName();
		}
		return "unknown";
	}
	
	/**
	 * ログのタイムスタンプを生成する。
	 */
	protected void getTimestamp() {
		Calendar cal = Calendar.getInstance();
		Object date[] = {cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND)};
		MessageFormat mf = new MessageFormat("{0,number,0000}/{1,number,00}/{2,number,00} {3,number,00}:{4,number,00}:{5,number,00}.{6,number,000}");
		logInfoMap.put(getMthodName(), mf.format(date));
	}
	
	/**
	 * サーブレットの処理時間を測る為のタイマー開始要求
	 */
	protected void startTimer() {
		startTime = System.nanoTime();
	}
	
	/**
	 * サーブレットの処理時間を測る為のタイマー停止要求
	 * 計測したナノ秒単位の処理時間が生成される。
	 */
	protected void stopTimer() {
		long procTime = System.nanoTime() - startTime;
		logInfoMap.put(getMthodName(), "PROCESS TIME:" + String.valueOf(procTime) + " nano sec");
	}
	
	/**
	 * サーブレット処理前のセッション属性内容を取得する。
	 * @param session
	 */
	protected void getBeforeAttributes(HttpSession session) {
		if(session == null){
			logInfoMap.put(getMthodName(), "SESSION ATTRIBUTE(before):none");
			return;
		}
		StringBuffer sessionInfo = new StringBuffer("SESSION ATTRIBUTE(before)["+session.getId()+"]:");
		Enumeration<?> keys = session.getAttributeNames();
		sessionInfo.append("{");
		while(keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			String val = session.getAttribute(key) == null ? new String() : session.getAttribute(key).toString();
			sessionInfo.append("["+key);
			sessionInfo.append("=");
			sessionInfo.append(val+"]");
			if(keys.hasMoreElements()) {
				sessionInfo.append(",");
			}
		}
		sessionInfo.append("}");
		
		logInfoMap.put(getMthodName(), sessionInfo.toString());
	}
	
	/**
	 * サーブレット処理後のセッション属性内容を取得する。
	 * @param session
	 */
	protected void getAfterAttributes(HttpSession session) {
		if(session == null){
			logInfoMap.put(getMthodName(), "SESSION ATTRIBUTE(after):none");
			return;
		}
		StringBuffer sessionInfo = new StringBuffer("SESSION ATTRIBUTE(after)["+session.getId()+"]:");
		Enumeration<?> keys = session.getAttributeNames();
		sessionInfo.append("{");
		while(keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			String val = session.getAttribute(key) == null ? new String() : session.getAttribute(key).toString();
			sessionInfo.append("["+key);
			sessionInfo.append("=");
			sessionInfo.append(val+"]");
			if(keys.hasMoreElements()) {
				sessionInfo.append(",");
			}
		}
		sessionInfo.append("}");
		
		logInfoMap.put(getMthodName(), sessionInfo.toString());
	}

	/**
	 * HTTP要求ヘッダの内容を取得する。
	 * @param request
	 */
	protected void getRequestHeader(HttpServletRequest request) {
		StringBuffer headerInfo = new StringBuffer();
		Enumeration<?> keys = request.getHeaderNames();
		headerInfo.append("REQUEST HEADERS:{");
		headerInfo.append("[REQUEST_PATH=");
		headerInfo.append(request.getRequestURI()+"]");
		if(keys.hasMoreElements()) {
			headerInfo.append(",");
		}
		while(keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			String val = request.getHeader(key) == null ? new String() : request.getHeader(key);
			headerInfo.append("["+key);
			headerInfo.append("=");
			headerInfo.append(val+"]");
			if(keys.hasMoreElements()) {
				headerInfo.append(",");
			}
		}
		headerInfo.append("}");
		
		logInfoMap.put(getMthodName(), headerInfo.toString());
	}

	/**
	 * HTTP応答ヘッダの内容を取得する。
	 * @param response
	 */
	protected void getResponseHeader(DebuglogResponseWrapper response) {
		logInfoMap.put(getMthodName(), "RESPONSE HEADERS:"+response.toString());
	}

	/**
	 * 要求パラメータの内容を取得する。
	 * @param request
	 */
	protected void getRequest(ServletRequest request) {
		logInfoMap.put(getMthodName(), "REQUEST VALUE:"+getRequest(request, request.getParameterNames()));
	}
	
	/**
	 * 要求パラメータの内容を解析
	 * @param paramMap
	 * @return
	 */
	private String getRequest(ServletRequest request, Enumeration<?> keys) {
		StringBuffer result = new StringBuffer("{");
		while(keys.hasMoreElements()) {
			Object keyObj = keys.nextElement();
			String key = keyObj != null ? keyObj.toString() : "";
			String val = "";
			try {
				val = new String(request.getParameter(key).getBytes("iso-8859-1"));
			}
			catch(Exception e) {
			}
			result.append("["+key);
			result.append("=");
			result.append(val+"]");
			if(keys.hasMoreElements()) {
				result.append(",");
			}
		}
		result.append("}");
		return result.toString();
	}
	
	/**
	 * HTTP応答ボディを取得する。
	 * @param response
	 */
	protected void getResponse(DebuglogResponseWrapper response) {
		logInfoMap.put(getMthodName(), "RESPONSE VALUDE:"+response.getResult()+"++"+response.getWriterBuffer());
	}
	
	/**
	 * 現在の時刻をYYYYMMDDHH24MISSMILLIフォーマットで生成する。
	 * @return
	 */
	protected String get_yyyymmdd() {
		Calendar cal = Calendar.getInstance();
		Object date[] = {cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND)};
		MessageFormat mf = new MessageFormat("{0,number,0000}{1,number,00}{2,number,00}{3,number,00}{4,number,00}{5,number,00}{6,number,000}");
		return mf.format(date);
	}
	
	/**
	 * logDirPath抽象ファイルクラスが示すディレクトリを生成する。
	 * ディレクトリツリーが存在しない場合、ディレクトリツリーごと生成する。
	 */
	protected void mkDir() {
		if(!logDirPath.exists()) {
			logDirPath.mkdirs();
		}
	}
	
	/**
	 * 例外トレースを1ファイルに出力する。
	 * @param ex
	 */
	protected void writeException(Exception ex) {
		if(logDirPath == null) {
			return;
		}
		try {
			mkDir();
			if(logDirPath.isDirectory()) {
				String canPath = logDirPath.getCanonicalPath();
				canPath = canPath.endsWith("/") ? canPath : canPath + "/";
				canPath += "exception_"+get_yyyymmdd()+".log";
				//FOR normal
				PrintStream ps = new PrintStream(new FileOutputStream(canPath));
				ex.printStackTrace(ps);
				ps.close();
				//FOR GOOGLE
//				ex.printStackTrace(System.err);
			}
		}
		catch(Exception e) {}
	}

	/**
	 * 指定されたファイル名から、ログファイルの生成された時間を抽出する。
	 * @param fileName
	 * @return
	 */
	protected int logDate(String fileName) {
		int result = Integer.MAX_VALUE;
		try {
			String dateString = fileName.substring(logFilePrefix.length(), fileName.lastIndexOf(logFileSuffix));
			if(dateString.length() == 8) {
				result = Integer.parseInt(dateString);
			}
		}
		catch(Exception e){
			result = Integer.MAX_VALUE;
		}
		return result;
	}
	
	/**
	 * 指定された世代数を超えた、古いログファイルを削除する。
	 * @param logFilesList
	 * @param basePath
	 * @throws IOException
	 */
	protected void deleteLogFile(Vector<String> logFilesList, String basePath) throws IOException{
		String oldestFile = new String();
		int oldtest = Integer.MAX_VALUE;

		for(int i=0;i<logFilesList.size();i++) {
			int logDate = logDate(logFilesList.get(i));
			if(logDate < oldtest) {
				oldestFile = logFilesList.get(i);
				oldtest = logDate;
			}
		}
		if(logFilesList.size() >= maxGeneration) {
			logFilesList.remove(oldestFile);
			oldestFile = basePath + oldestFile;
			File deleteFile = new File(oldestFile);
			deleteFile.delete();
//			String deleteCmd[] = {"unlink",oldestFile};
//			Runtime.getRuntime().exec(deleteCmd);
			deleteLogFile(logFilesList, basePath);
		}
	}
	
	/**
	 * 出力するログファイルを応答する。
	 * @return
	 * @throws IOException
	 */
	protected File selectLogFile() throws IOException {
		String targetfileName = logFilePrefix + get_yyyymmdd().substring(0, 8) + logFileSuffix;
		List<String> logFilesList = Arrays.asList(logDirPath.list(new LogFilenameFilter()));
		String basePath = logDirPath.getCanonicalPath();
		basePath = basePath.endsWith("/") ? basePath : basePath + "/";
		if(!logRotate) {
			targetfileName = basePath + logFilePrefix + "debug" + logFileSuffix;
			return new File(targetfileName);
		}
		if(logFilesList.contains(targetfileName)) {
			targetfileName = basePath + targetfileName;
			return new File(targetfileName);
		}
		
		deleteLogFile(new Vector<String>(logFilesList), basePath);
		
		targetfileName = basePath + targetfileName;
		return new File(targetfileName);
	}
	
	/**
	 * 指定されたログモードに従ったログをログファイルに出力する。
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected void writeLog(HttpServletRequest request, DebuglogResponseWrapper response) throws IOException{
		getRequestHeader(request);
		getResponseHeader(response);
		if(debugMode == LogLevel.full || debugMode == LogLevel.debug) {
			getRequest(request);
			if(debugMode == LogLevel.debug) {
				getResponse(response);
			}
		}
		//FOR normal
		File targetFile = selectLogFile();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(targetFile, true))));
		//FOR google
//		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(System.out)));
		StringBuffer logData = new StringBuffer();
		for(int i=0;i<targetLogs.size();i++) {
			String temp = logInfoMap.get(targetLogs.get(i)) == null ? targetLogs.get(i)+":NONE" : logInfoMap.get(targetLogs.get(i));
			logData.append(temp.replaceAll("[\r|\n|\r\n]", "\\\\n"));
//			if(i == 0) {
//				logData.append("\n");
//			}else
			if(i<targetLogs.size()-1) {
				logData.append("\n\t");
			}
		}
		pw.println(logData.toString());
		pw.close();
	}
	

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if(debugMode == LogLevel.none) { //no logging
			chain.doFilter(request, response);
		}else{
			try {
				getBeforeAttributes(((HttpServletRequest)request).getSession(false));
				DebuglogResponseWrapper responseWrpper = new DebuglogResponseWrapper((HttpServletResponse)response);
				startTimer();
				chain.doFilter(request, responseWrpper);
				responseWrpper.writerFlush();
				stopTimer();
				getTimestamp();
				getAfterAttributes(((HttpServletRequest)request).getSession(false));
				writeLog((HttpServletRequest)request, responseWrpper);
			}
			catch(ServletException e) {
				writeException(e);
				throw e;
			}
			catch(IOException e) {
				writeException(e);
				throw e;
			}
			catch(Exception e) {
				writeException(e);
				throw new ServletException(e);
			}
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		String pathParam  = filterConfig.getInitParameter("path");
		String debugParam = filterConfig.getInitParameter("level");
		String rotate     = filterConfig.getInitParameter("rotate");
		String logMax     = filterConfig.getInitParameter("logmax");
		String resultSize = filterConfig.getInitParameter("resultSize");
		if(debugParam != null) {
			debugMode = LogLevel.valueOf(debugParam.toLowerCase());
			if(debugMode.equals(LogLevel.debug)) {
				targetLogs = Arrays.asList(debugLogs);
			}
			if(debugMode.equals(LogLevel.full)) {
				targetLogs = Arrays.asList(fullLogs);
			}
			if(debugMode.equals(LogLevel.middle)) {
				targetLogs = Arrays.asList(middleLogs);
			}
			if(debugMode.equals(LogLevel.smart)) {
				targetLogs = Arrays.asList(smartLogs);
			}
		}
		if(debugMode == LogLevel.none) { //no logging
			return;
		}

		if(pathParam != null) {
			try {
				logDirPath = new File(pathParam);
				mkDir();
				if(!logDirPath.isDirectory()) {
					logDirPath= null;
				}
			}
			catch(Exception e) {}
		}
		
		if(rotate != null) {
			try {
				logRotate = Boolean.parseBoolean(rotate);
			}
			catch(Exception e) {
				logRotate = true;
			}
		}

		if(logMax != null) {
			try {
				maxGeneration = Integer.parseInt(logMax);
			}
			catch(Exception e) {
				maxGeneration = 7;
			}
		}

		if(resultSize != null) {
			try {
				responseLogMaxSize = Integer.parseInt(resultSize);
			}
			catch(Exception e) {
				responseLogMaxSize = 128;
			}
		}
	}
}
