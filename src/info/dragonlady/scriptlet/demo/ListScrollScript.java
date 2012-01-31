package info.dragonlady.scriptlet.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import info.dragonlady.scriptlet.ESEngine;
import info.dragonlady.scriptlet.Scriptlet;
import info.dragonlady.scriptlet.SystemErrorException;

public class ListScrollScript extends Scriptlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -277321953941608373L;

	private HashMap<String, Object> globalObjMap = new HashMap<String, Object>();
	
	private TreeMap<String, Vector<String>> tableMap = new TreeMap<String, Vector<String>>();

	@Override
	public String getEScriptErrorMsg() {
		return "スクリプトエラー";
	}

	@Override
	public String getInvalidParamErrorMsg() {
		return "パラメータエラー";
	}

	@Override
	public String getInvalidValidationParamErrorMsg() {
		return "検証ルールエラー";
	}

	@Override
	public String getRequiredParamErrorMsg() {
		return "必須パラメータ無指定";
	}

	@Override
	public Map<String, Object> getScriptNewProperties() {
		return globalObjMap;
	}

	@Override
	public long getSerialVersionUID() {
		return serialVersionUID;
	}

	public class CsvDataObject {
		private TreeMap<String, Vector<String>> csvMap = null;
		private int current = 0;
		private int left = 0;
		private int last = 0;
		private int length = 0;
		private int dis = 0;
		
		public CsvDataObject(TreeMap<String, Vector<String>> map) {
			csvMap = map;
			last = csvMap.keySet().size();
		}
		
		public void setParam(int s, int len) {
			current = s;
			left = 0;
			length = s+len;
			dis = len;
		}
		
		public String nextTitle() {
			String result = null;
			if(current < last) {
				result =  csvMap.keySet().toArray()[current].toString();
				current++;
			}
			return result;
		}
		
		public boolean hasNextTitle() {
			if(current < last && current < length) {
				return true;
			}
			return false;
		}
		
		public String nextValue(String title) {
			String result = null;
			Vector<String> values = csvMap.get(title);
			if(left < values.size()) {
				result = values.get(left);
				left++;
			}
			return result;
		}
		
		public boolean hasNextValue(String title) {
			Vector<String> values = csvMap.get(title);
			if(left < values.size()) {
				return true;
			}
			left = 0;
			return false;
		}
		
		public boolean isLast() {
			if(current < last) {
				return false;
			}
			return true;
		}
		
		public boolean isTop() {
			if(current-dis > 0) {
				return false;
			}
			return true;
		}
	}

	/**
	 * リソース内のListScroll.csvのオブジェクト化クラス
	 * @author nobu
	 *
	 */
//	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
//		try {
//			//リソースからListScroll.csvを読み込み、CsvDataObjectのインスタンスを生成
//			URL propertiesResource = this.getClass().getClassLoader().getResource("/info/dragonlady/scriptlet/demo/ListScroll.csv");
//			BufferedReader br = new BufferedReader(new InputStreamReader(propertiesResource.openStream(), "UTF-8"));
//			String line = null;
//			String key = new String();
//			while((line = br.readLine()) != null) {
//				Vector<String> vals = new Vector<String>();
//				int i=0;
//				String datas[] = line.split(",");
//				for(String data : datas) {
//					if(i==0) {
//						key = data;
//					}else{
//						vals.add(data);
//					}
//					i++;
//				}
//				tableMap.put(key, vals);
//			}
//			CsvDataObject dataObj = new CsvDataObject(tableMap);
//			globalObjMap.put("csvdatas", dataObj);
//			
//			//スクリプト実行
//			ESEngine.executeScript(this);
//		}
//		catch(ESException e) {
//			e.printStackTrace(System.out);
//			res.sendError(403, e.getMessage());
//		}
//	}
	
//	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
//		doPost(req, res);
//	}

	@Override
	public void start() throws SystemErrorException {
		try {
			//リソースからListScroll.csvを読み込み、CsvDataObjectのインスタンスを生成
			URL propertiesResource = this.getClass().getClassLoader().getResource("/info/dragonlady/scriptlet/demo/ListScroll.csv");
			BufferedReader br = new BufferedReader(new InputStreamReader(propertiesResource.openStream(), "UTF-8"));
			String line = null;
			String key = new String();
			while((line = br.readLine()) != null) {
				Vector<String> vals = new Vector<String>();
				int i=0;
				String datas[] = line.split(",");
				for(String data : datas) {
					if(i==0) {
						key = data;
					}else{
						vals.add(data);
					}
					i++;
				}
				tableMap.put(key, vals);
			}
			CsvDataObject dataObj = new CsvDataObject(tableMap);
			globalObjMap.put("csvdatas", dataObj);
			
			//スクリプト実行
			ESEngine.executeScript(this);
		}
		catch(Exception e) {
			e.printStackTrace(System.out);
			throw new SystemErrorException(e);
		}
	}
}
