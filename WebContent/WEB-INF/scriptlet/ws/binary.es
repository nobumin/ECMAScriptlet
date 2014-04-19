<serverscript>

try {
	if(wsstatus == "open") { //接続時
		sysout.println("open");
	}else if(wsstatus == "close") { //切断時
		sysout.println("close");
	}else if(wsstatus == "error") { //障害時
		sysout.println("error");
	}else if(wsstatus == "exec") { //ユーザ実行時
		var bytdata = wssession.getUserProperties().get('userdata');
		sysout.println("data length:"+bytdata.array().length);
		var endpoint = wssession.getAsyncRemote();
		endpoint.sendText('next');
	}
} 
catch(e) {
	sysout.println(e.javaException);
//	sysout.println(e.stack);
//	e.printStackTrace(sysout);
}

</serverscript>
