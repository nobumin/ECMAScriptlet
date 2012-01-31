function getValue() {
//getElmenetByIdは現在のところ利用不可
//	var targetNode = document.getElmenetById("doctest");
	var targetNode = document.getElementsByTagName("div").item(0);
	if(targetNode){
		var testValue = targetNode.getFirstChild().getNodeValue();
		//
		//呼び出し側HTMLの文字コードに関係なく、DOM操作は全てUNICODEであることを注意する。
		targetNode.appendChild(document.createElement("br"));

		if(document.getCookie("start")) {
			targetNode.appendChild(document.createTextNode(document.getCookie("start").getValue()));
		}else{
			targetNode.appendChild(document.createTextNode("No Cookie"));
		}

		var d = new Date();
		var value = d.getHours() + "h" + d.getMinutes() + "m" + d.getSeconds() + "s";
		document.setCookie("end", value, -1);
	}
}