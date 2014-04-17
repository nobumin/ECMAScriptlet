package info.dragonlady.scriptlet;


import java.util.Map;

import javax.websocket.Session;

public class DefaultWSScriptlet extends WSScriptlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4444840273875879854L;

	@Override
	public void start(String path, Session session, WSScriptlet.WS_STATUS wsStatus) throws SystemErrorException {
		try {
			ESEngine.executeScript(this, path, session, wsStatus);
		}
		catch(Exception e) {
			e.printStackTrace(System.out);
			throw new SystemErrorException(e);
		}
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
