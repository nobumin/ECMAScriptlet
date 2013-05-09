package info.dragonlady.scriptlet;

public class BaseJsonRequest {
	public String path;
	public String query;
	public String response;
	public String excute;
	public String session;
	
	public void setResult(String value) {
		response = new String(value);
	}
	public void clearResult() {
		response = null;
	}
	public void setSession(String value) {
		session = new String(value);
	}
}