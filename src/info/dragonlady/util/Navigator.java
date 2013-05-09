package info.dragonlady.util;

import info.dragonlady.scriptlet.Scriptlet;

public class Navigator {
	Scriptlet scriptlet = null;
	
	public String userAgent = null;
	public String referer = null;
	public String remoteAddr = null;
	public String termID = null;
	
	public Navigator(Scriptlet script) {
		scriptlet = script;
		initProperties();
	}
	
	protected void initProperties() {
		userAgent = scriptlet.getRequest().getHeader("User-Agent");
		referer = scriptlet.getRequest().getHeader("referer");
		remoteAddr = scriptlet.getRequest().getRemoteAddr();
		if(userAgent != null && userAgent.toLowerCase().matches(".*kddi.+up.browser.+")) {
			termID = scriptlet.getRequest().getHeader("X-Up-Subno");
		}else
		if(userAgent != null && userAgent.toLowerCase().matches(".+docomo.+")) { //?guid=ON
			termID = scriptlet.getRequest().getHeader("X-DCMGUID");
		}
	}
}
