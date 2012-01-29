<html>
<META http-equiv="Content-Type" content="text/html; charset=utf-8">
<body>
<servervalidation>
remoteurl=.+,true
</servervalidation>
<serverscript>
var url = request.getParameter("remoteurl");
var net = new java.net.URL(url);
var con = net.openConnection();
var reader = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream(),"utf-8"));
var readData = reader.readLine();
var xmlData = "";
while(readData){
  xmlData += readData;
  readData = reader.readLine();
}
xmlData = xmlData.substring(xmlData.indexOf('?>')+2);
var rssxml = new XML(xmlData);
for(var temp1 in rssxml..item.title) {
  serverout.write(rssxml..item.title[temp1]);
  serverout.write("<br>\n");
}
</serverscript>
<br><br>
<a href='e4x.es'>BACK</a>
</body>
</html>