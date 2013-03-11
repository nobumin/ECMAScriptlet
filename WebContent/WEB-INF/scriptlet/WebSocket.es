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

            	var sendData = '{"path":"/ws/request.es","query":"test message","response":"","excute":"/ws/event.es"}';
                ws.send(sendData);
            }
            
            function closeConnect(){
                ws.close();
            }
        </script>
    </head>
    <body>
        <textarea id="event" readonly style="widht:600px;height:300px;"></textarea><br/>
        <button type="submit" id="sendButton" onClick="postToServer()">SendText</button>　
        <button type="submit" id="closeButton" onClick="closeConnect()">Close</button>
    </body>
</html>