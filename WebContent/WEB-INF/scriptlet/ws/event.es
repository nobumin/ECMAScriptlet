<serverscript>

if(jsonReq.session && jsonReq.session.length() > 0) {
	var sessionObj = JSON.parse(jsonReq.session);
	var now = new Date();

	mongodb.open();
	var notificationCol = mongodb.getCollection("notification_col");
	var queryNode = mongodb.createDBQuery();
	queryNode.put("sid", sessionObj.sid);
	var dateYYYYMMDDHHMI = now.getFullYear() + ("00"+(now.getMonth()+1)).slice(-2) + ("00"+now.getDate()).slice(-2) + ("00"+now.getHours()).slice(-2) + ("00"+now.getMinutes()).slice(-2);
	queryNode.put("etime", dateYYYYMMDDHHMI);
	var cur = notificationCol.find(queryNode);

	var resultMessage = null;
	while(cur.hasNext()) {
		var evetData = JSON.parse(cur.next());
		if(resultMessage != null) {
			resultMessage += ",";
			resultMessage += evetData.message;
		}else{
			resultMessage = evetData.message;
		}
	}
	if(resultMessage != null) {
		jsonReq.setResult('{"message":"'+resultMessage+'"}');
	}
	
	var removeNode = mongodb.createDBQuery();
	removeNode.put("sid", sessionObj.sid);
	var removeCondition = mongodb.createDBQuery();
	removeCondition.put("$lte", dateYYYYMMDDHHMI);
	removeNode.put("etime", removeCondition);
	notificationCol.remove(removeNode);
	
	mongodb.close();
}
</serverscript>
