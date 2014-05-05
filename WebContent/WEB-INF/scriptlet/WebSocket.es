<!DOCTYPE html>
<html>
    <head>
        <meta charset=UTF-8>
        <title>Tomcat WebSocket Chat</title>
        <script>
        <serverscript>
//        serverout.write("            var ip='"+request.getLocalAddr()+"';\n");
//        serverout.write("            var host='"+request.getServerName()+"';");
        serverout.write("            var host='dragonlady.info';");
        </serverscript>
        	var sid = (new Date()).getTime();
        	var ws = null;
        	openConnect();
            
            function postToServer(){
            	var msg = document.getElementById("msg").value;
                ws.send(msg);
            }
            
            function closeConnect(){
            	if(ws) {
            		try {
                        ws.close();
            		} catch(e){
                    	console.log(e);
            		}            		
                    ws = null;
            	}
            }
            function openConnect(){
            	closeConnect();
//            	ws = new WebSocket("ws://"+ip+":8080/ecmascriptlet/ws/WebSocketScriptlet");
//            	ws = new WebSocket("ws://"+host+":8080/ecmascriptlet/ws?script=ws/request.es");
            	ws = new WebSocket("ws://"+host+"/ecmascriptlet/ws?script=ws/request.es");
//            	ws = new WebSocket("ws://"+host+"/ecmascriptlet/ws/WebSocketScriptlet.es");
                ws.onopen = function(){
                	console.log(new Date() + " onopen");
                	wsSetup();
                };
            }
            function wsSetup() {
                ws.onmessage = function(event) {
                	document.getElementById("event").value += event.data + "\n";
                };
                ws.onclose = function(){
                	console.log(new Date() + " onclose");
                };
            }
        </script>
    </head>
    <body>
        受信メッセージ:<textarea id="event" readonly style="width:600px;height:300px;"></textarea><br/><br/>
        
        送信メッセージ:<input type="text" id="msg" style="width:600px;height:30px;" value=""><br><br>
        <button type="submit" id="sendButton" onClick="postToServer()">データ送信</button>　
        <button type="submit" id="closeButton" onClick="closeConnect()">WebSocket切断</button>
        <button type="submit" id="openButton" onClick="openConnect()">WebSocket再接続</button>
    </body>
</html>