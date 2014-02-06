package info.dragonlady.scriptlet.mail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
//import java.io.PrintWriter;
//import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import info.dragonlady.scriptlet.ESEngine;
import info.dragonlady.util.DBAccesser;
import info.dragonlady.util.SmtpParser;

public class Main {
	protected BufferedInputStream bis = null;
	protected static String defualtSupportMailAddr = "nobu@dragonlady.info";
	public final static String _debug = "__DEBUG__";

	/**
	 * �f�t�H���g�R���X�g���N�^
	 * @param is
	 */
	protected Main(InputStream is) {
		//.forward�L�q�T���v��(postfix)
		//"|IFS=' ' && exec /opt/escriptlet/bin/mail.sh"
		bis = new BufferedInputStream(is);
	}
	
	/**
	 * SMTP����͂��āA�{�f�B�ɋL�q���ꂽ�X�N���v�g�����s����B
	 * @throws MailReceivedException
	 */
	protected void start(Map<String, Object> jsObjectMap, Properties properties, Vector<String> mimeList) throws MailReceivedException {
		StringBuffer readData = new StringBuffer();
		byte readBuff[] = new byte[1024];
		int readLen = 0;
		byte allReadData[] = null;
		
		try {
			DBAccesser dba = new DBAccesser(properties);
			while((readLen=bis.read(readBuff, 0, 1024)) > 0) {
				if(allReadData == null) {
					allReadData = new byte[readLen];
					System.arraycopy(readBuff, 0, allReadData, 0, readLen);
				}else{
					byte tempBuff[] = new byte[allReadData.length];
					System.arraycopy(allReadData, 0, tempBuff, 0, tempBuff.length);
					allReadData = new byte[allReadData.length+readLen];
					System.arraycopy(tempBuff, 0, allReadData, 0, tempBuff.length);
					System.arraycopy(readBuff, 0, allReadData, tempBuff.length, readLen);
				}
			}
			readData.append(new String(allReadData, 0, allReadData.length, "ISO-2022-JP"));
			//
			SmtpParser smtp = SmtpParser.parse(readData.toString(), properties, mimeList);
			//SCRIPT EXEC
			ESEngine.executeScript(smtp, jsObjectMap, dba, new DefaultMailScriptlet());
		}
		catch (Exception e) {
			throw new MailReceivedException(e);
		} 
	}
	
	/**
	 * main�֐�
	 * @param args
	 */
	public static void main(String[] args) {
//All coment out in Google app engin
 /*
 		Main main = new Main(System.in);
		//DEBUG
		File debugFile = new File("/var/tmp/"+Calendar.getInstance().getTimeInMillis()+"_moblog.log");
		FileOutputStream fos = null;
		try {
			Vector<String> mimeList = new Vector<String>();
			//mime csv load
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			URL propertiesResource = cl.getResource("info/dragonlady/scriptlet/mail/resources/mime_type.csv");
			BufferedReader reader = new BufferedReader(new InputStreamReader(propertiesResource.openStream()));
			String line;
			while((line=reader.readLine()) != null) {
				if(line != null && line.length() > 0 && line.indexOf(",") > 0) {
					mimeList.add(line);
				}
			}

			//Sandbox
			propertiesResource = cl.getResource("info/dragonlady/scriptlet/mail/resources/mail_config.xml");
			Properties properties = new Properties();
			properties.loadFromXML(propertiesResource.openStream());
			System.setProperty("java.security.policy", properties.getProperty("default_policy"));
			System.setSecurityManager(new SecurityManager());
			
			//DEBUG
			fos = new FileOutputStream(debugFile);
			System.setOut(new PrintStream(fos));

			//main
			String objectMapXml = args[0];
			HashMap<String, Object> jsObjectMap = new HashMap<String, Object>();
			if(objectMapXml != null && objectMapXml.length() > 1){
				Properties prop = new Properties();
				prop.loadFromXML(new BufferedInputStream(new FileInputStream(objectMapXml)));
				Set<Object> keys = prop.keySet();
				for(Object key : keys) {
					if(key != null && key.toString().length() > 0) {
						String className = prop.getProperty(key.toString());
						Class<?> loadClass = ClassLoader.getSystemClassLoader().loadClass(className);
						jsObjectMap.put(key.toString(), (Object)loadClass.newInstance());
					}
				}
			}
			main.start(jsObjectMap, properties, mimeList);
			//TODO SMTP BODY�̏�������
		}
		catch(Exception e) {
//			System.err.println(e.getMessage());
//			e.printStackTrace(System.err);
//			System.exit(75); //EX_TEMPFAIL�Ƃ��đ��s
//			System.exit(64); //EX_USAGE
//			System.exit(65); //EX_DATAERR
//			StringWriter sw = new StringWriter();
//			e.printStackTrace(new PrintWriter(sw));
			
			try {
				e.printStackTrace(new PrintStream(fos));
				fos.close();
			} catch (Exception e1) {
			}
			
			System.exit(0); //SUCCESS
		}
		//DEBUG
		try {
			fos.close();
			debugFile.delete();
		} catch (Exception e) {
		}
*/
		System.exit(0);
	}
}
