<!DOCTYPE html>
<html>
    <head>
        <meta charset=UTF-8>
        <title>Tomcat WebSocket Chat</title>
        <script>
        <serverscript>
    	serverout.write("            var ip='"+request.getLocalAddr()+"';");
        </serverscript>
            var ws = new WebSocket("ws://"+ip+":8080/ecmascriptlet/ws/WebSocketScriptlet");
            ws.onopen = function(){
            	console.log("onopen");
            };
            ws.onmessage = function(event) {
            	console.log(event.data);
            	var jobj = JSON.parse(event.data);
            	document.getElementById("event").value += jobj.message + "\n";
            };
            ws.onclose = function(){
            	console.log("onclose");
            };
            
            function postToServer(){
            	var sid = document.getElementById("sid").value;
            	var sendData = '{"path":"/ws/request.es","query":"{\'sid\':\''+sid+'\'}","response":"","excute":"/ws/event.es"}';
                ws.send(sendData);
            }
            
            function closeConnect(){
                ws.close();
            }
        </script>
    </head>
    <body>
        受信メッセージ:<textarea id="event" readonly style="widht:600px;height:300px;"></textarea><br/><br/>
        
        ユニークID:<input type="text" id="sid" style="widht:600px;height:30px;" value=""><br>
        <button type="submit" id="sendButton" onClick="postToServer()">データ送信</button>　
        <button type="submit" id="closeButton" onClick="closeConnect()">WebSocket切断</button>
    </body>
</html>