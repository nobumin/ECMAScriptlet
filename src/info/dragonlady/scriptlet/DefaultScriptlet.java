package info.dragonlady.scriptlet;

import java.util.Map;

public class DefaultScriptlet extends Scriptlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1146984607853863011L;

	/**
	 * POSTメソッドに応答する関数<br>
	 * javax.servlet.http.HttpServletクラスのオーバーライド関数<br>
	 * HttpServlet#service関数はSecureServletクラスで実装しているので、<br>
	 * オーバーライドできません。
	 */
//	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
//		try {
//			//サーバサイドスクリプトを実行する前に処理する必要がある場合、ここに実装してください。
//			//例えば、DBへアクセスしてサーバサイドスクリプトで利用する場合、
//			//業務処理を実装し、getScriptNewProperties関数で応答するMapオブジェクトに
//			//サーバスクリプト内グローバル名をキーとして、業務処理後結果オブジェクトを格納することで、
//			//サーバサイドスクリプトで操作が可能となります。
//			
//			//サーバサイドスクリプトの実行
//			//必ず関数の最後に呼出してください。
//			ESEngine.executeScript(this);
//		}
//		catch(ESException e) {
//			//TODO
//			e.printStackTrace(System.out);
//			res.sendError(403, e.getMessage());
//		}
//	}
	
	/**
	 * GETメソッドに応答する関数<br>
	 * javax.servlet.http.HttpServletクラスのオーバーライド関数<br>
	 * HttpServlet#service関数はSecureServletクラスで実装しているので、<br>
	 * オーバーライドできません。
	 */
//	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
//		doPost(req, res);
//	}

	@Override
	public String getEScriptErrorMsg() {
		return "サーバスクリプトに問題があります。";
	}

	@Override
	public String getInvalidParamErrorMsg() {
		return "パラメータに不備があります。";
	}

	@Override
	public String getInvalidValidationParamErrorMsg() {
		return "servervalidationパラメータに不備があります。";
	}

	@Override
	public String getRequiredParamErrorMsg() {
		return "パラメータは必須です。";
	}

	@Override
	public Map<String, Object> getScriptNewProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSerialVersionUID() {
		return serialVersionUID;
	}

	@Override
	public void start() throws SystemErrorException {
		try {
			//サーバサイドスクリプトを実行する前に処理する必要がある場合、ここに実装してください。
			//例えば、DBへアクセスしてサーバサイドスクリプトで利用する場合、
			//業務処理を実装し、getScriptNewProperties関数で応答するMapオブジェクトに
			//サーバスクリプト内グローバル名をキーとして、業務処理後結果オブジェクトを格納することで、
			//サーバサイドスクリプトで操作が可能となります。
			
			//サーバサイドスクリプトの実行
			//必ず関数の最後に呼出してください。
			ESEngine.executeScript(this);
		}
		catch(Exception e) {
			e.printStackTrace(System.out);
			throw new SystemErrorException(e);
		}
	}

}
