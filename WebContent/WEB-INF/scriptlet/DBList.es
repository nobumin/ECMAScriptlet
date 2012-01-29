<html>
<head>
<META http-equiv="Content-Type" content="text/html; charset=Shift-jis">
<title>DB List</title>
</head>
<body>
<table border='1'>
<tr><td>no</td><td>name</td><td> </td></tr>
<serverscript>
var con = dbaccesser.getConnection();
var resultList = dbaccesser.selectQuery("listsql", con, null);
var i=0;
for(i=0;i<resultList.size();i++) {
	serverout.write("<tr>\n");
	serverout.write("<form action='DB_DELETE/DBDelete.es' method='post'>\n");
	var record = resultList.get(i);
	var keys = record.keySet().toArray();
	for(var j=0;j<keys.length;j++) {
		var key = keys[j];
		serverout.write("<td>"+helper.HTMLEncode(record.get(key))+"</td>\n");
		var hiddenVal = new java.lang.String(record.get(key));
		hiddenVal = hiddenVal.replaceAll("'","\"");
		serverout.write("<input type='hidden' name='"+helper.HTMLEncode(key).toLowerCase()+"' value='"+hiddenVal+"' />\n");
	}
	serverout.write("<td><input type='submit' name='submit' value='delete' /></td>\n");
	serverout.write("</form>\n");
	serverout.write("</tr>");
}
con.rollback();
con.close();
</serverscript>
</table>
<br><br>
<br/>
<a href='DBTest.es'>戻る</a>
</body>
</html>