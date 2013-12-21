<serverscript>

if(jsonReq.session && jsonReq.session.length() > 0) {
	var sessionObj = JSON.parse(jsonReq.session);
	var now = new Date();

	mongodb.open();
	try {
		var chatMessageCol = mongodb.getCollection("chat_message_col");
//		var queryNode = mongodb.createDBQuery();
//		queryNode.put("sid", sessionObj.sid);

		var sortObject = mongodb.createDBObject();
		sortObject.put('regist_ts',-1);
		var cur = chatMessageCol.find().sort(sortObject);

		var resultMessages = null;
		var count = 0;
		while(cur.hasNext() && count < 10) { //10件まで
			count++;
			var evetData = JSON.parse(cur.next());
			if(resultMessages != null) {
				resultMessages += ",";
				resultMessages += evetData.comment;
			}else{
				resultMessages = evetData.comment;
			}
		}
		if(resultMessages != null) {
			jsonReq.setResult('{"message":"'+resultMessages+'"}');
		}
		
//		var removeNode = mongodb.createDBQuery();
//		removeNode.put("sid", sessionObj.sid);
//		var removeCondition = mongodb.createDBQuery();
//		removeCondition.put("$lte", dateYYYYMMDDHHMI);
//		removeNode.put("etime", removeCondition);
//		notificationCol.remove(removeNode);
	} catch(e) {
		sysout.println(e);
	}
	
	mongodb.close();
}
</serverscript>
