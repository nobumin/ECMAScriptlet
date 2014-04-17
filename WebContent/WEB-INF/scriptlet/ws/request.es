<serverscript>

try {
	var sessions = wssession.getOpenSessions();
	if(sessions.size() > 0) {
		var it = sessions.iterator();
		while(it.hasNext()) {
			var session = it.next();
			var endpoint = session.getAsyncRemote();
			if(wsstatus == "open") { //接続時
				endpoint.sendText("Hello from "+wssession.getId());
			}else if(wsstatus == "close") { //切断時
				endpoint.sendText("Bye from "+wssession.getId());
			}else if(wsstatus == "error") { //エラー時
				//nop
			}else if(wsstatus == "exec") { //ユーザ実行時
				endpoint.sendText(wssession.getId()+":"+wssession.getUserProperties().get('userdata').toString());
			}
		}
	}
	var now = new Date();
	var before15min = now.getTime() - (5*60*1000);
	var removeData = "{'regist_ts':{'$lte':"+before15min+"}}";
	mongodbjson.removeDB(mongodb, 'chat_message_col', removeData);
	
	var jsonData = {};
	jsonData.sid = wssession.getId();
	jsonData['regist_ts'] = now.getTime();
	if(wsstatus == "open") { //接続時
		jsonData['comment'] = "connect now";
		mongodbjson.insertDB(mongodb, 'chat_message_col', JSON.stringify(jsonData));
	}else if(wsstatus == "close") { //切断時
		jsonData['comment'] = "disconnect now";
		mongodbjson.insertDB(mongodb, 'chat_message_col', JSON.stringify(jsonData));
	}else if(wsstatus == "exec") { //ユーザ実行時
		jsonData['comment'] = wssession.getUserProperties().get('userdata').toString();
		mongodbjson.insertDB(mongodb, 'chat_message_col', JSON.stringify(jsonData));
	}
} 
catch(e) {
	sysout.println(e.javaException);
//	sysout.println(e.stack);
//	e.printStackTrace(sysout);
}

</serverscript>
