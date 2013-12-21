<serverscript>

var queryString = jsonReq.query.replaceAll("'", '"');
var queryObj = JSON.parse(queryString);
jsonReq.setSession(queryString); //event.esでsidをセッションとして利用する場合
//jsonReq.setResult('{"message":"'+queryObj.msg+'"}');

mongodb.open();
try {
	var chatMessageCol = mongodb.getCollection("chat_message_col");

	var now = new Date();
	var before15min = now.getTime() - (5*60*1000);
	var removeNode = mongodb.createDBQuery();
	var removeSetDateNode = mongodb.createDBQuery();
	removeSetDateNode.put("$lte", before15min);
	removeNode.put("regist_ts", removeSetDateNode);
	chatMessageCol.remove(removeNode);

	var newMessage = mongodb.createDBObject();
	if(queryObj.msg != null && queryObj.msg.length > 0) {
		newMessage.put('comment',queryObj.msg);
		newMessage.put('regist_ts',now.getTime());
		chatMessageCol.insert(newMessage);
	}else if(queryObj.hello != null && queryObj.hello.length > 0) {
		newMessage.put('comment',queryObj.hello+" is connect");
		newMessage.put('regist_ts',now.getTime());
		chatMessageCol.insert(newMessage);
	}
} catch(e) {
	sysout.println(e);
}
mongodb.close();

</serverscript>
