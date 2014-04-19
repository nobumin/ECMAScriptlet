<serverscript>

try {
	if(wsstatus == "open") { //接続時
		sysout.println("open");
	}else if(wsstatus == "close") { //切断時
		sysout.println("close");
	}else if(wsstatus == "error") { //障害時
		sysout.println("error");
	}else if(wsstatus == "exec") { //ユーザ実行時
		var msg = wssession.getUserProperties().get('userdata').toString();
		if(msg == 'start') {
			var endpoint = wssession.getAsyncRemote();
			var fh = new java.io.File("/Applications/ServerApplication/tomcat/webapps/ecmascriptlet/WEB-INF/scriptlet/images/test4.jpg");
			var inBuf = new java.io.BufferedInputStream(new java.io.FileInputStream(fh));
			var inLen = inBuf.available();
			while(inLen > 0) {
				var byteBuff = java.nio.ByteBuffer.allocate(inLen);
				inBuf.read(byteBuff.array(), 0, inLen);
				endpoint.sendBinary(byteBuff);
				//endpoint.sendObject();
				inLen = inBuf.available();
			}
			inBuf.close();
		}
	}
} 
catch(e) {
//	sysout.println(e.javaException);
//	sysout.println(e.stack);
	e.printStackTrace(sysout);
}

</serverscript>
