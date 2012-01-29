<html>
<META http-equiv="Content-Type" content="text/html; charset=shift-jis">
<body>
<servervalidation>
remoteurl=.+,true
</servervalidation>
<serverscript>
var url = request.getParameter("remoteurl");
var net = new java.net.URL(url);
var con = net.openConnection();
var reader = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream()));
var readData = reader.readLine();
while(readData){
  serverout.write(readData);
  serverout.write("\n");
  readData = reader.readLine();
}
</serverscript>
<br><br>
<a href='LiveConnect.es'>Back</a>
</body>
</html>