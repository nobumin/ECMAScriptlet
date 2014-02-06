<!DOCTYPE html>
<html>
    <head>
        <meta charset=UTF-8>
        <title>Tomcat WebSocket Chat</title>
        <script>
        <serverscript>
        serverout.write("            var ip='"+request.getLocalAddr()+"';\n");
        //serverout.write("            var host='"+request.getServerName()+"';");
        serverout.write("            var host='dragonlady.info';");
        </serverscript>
        	var sid = (new Date()).getTime();
        	var ws = null;
        	openConnect();
            
            function postToServer(){
            	var msg = document.getElementById("msg").value;
            	//path:要求時に呼び出すスクリプトレット　→　送信
            	//excute:バックグラウンドで処理する（タイマーで処理される）スクリプトレット　→　受信
            	var sendData = '{"path":"/ws/request.es","query":"{\'sid\':\''+sid+'\', \'msg\':\''+msg+'\'}","response":"","excute":"/ws/event.es"}';
                ws.send(sendData);
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
            	//var ws = new WebSocket("ws://"+ip+":8080/ecmascriptlet/ws/WebSocketScriptlet");
            	ws = new WebSocket("ws://"+host+"/ecmascriptlet/ws/WebSocketScriptlet.es");
                ws.onopen = function(){
                	console.log(new Date() + " onopen");
                	wsSetup();
                	var sendData = '{"path":"/ws/request.es","query":"{\'sid\':\''+sid+'\', \'hello\':\''+sid+'\'}","response":"","excute":"/ws/event.es"}';
                    ws.send(sendData);
                };
            }
            function wsSetup() {
                ws.onmessage = function(event) {
                	var jobj = JSON.parse(event.data);
                	if(jobj.message && jobj.message.length > 0) {
                		var messages = jobj.message.split(',');
                		var v = "";
                		for(var m in messages) {
                			v += messages[m] + "\n";
                		}
                    	document.getElementById("event").value = v;
                	}
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