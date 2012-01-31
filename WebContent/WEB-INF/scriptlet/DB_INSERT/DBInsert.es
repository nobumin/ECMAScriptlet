<html>
<head>
<META http-equiv="Content-Type" content="text/html; charset=Shift-jis">
<title>DB List</title>
</head>
<body>
<table border='1'>
<tr><td>no</td><td>name</td></tr>
<serverscript>
var con = dbaccesser.getConnection();
var resultList = dbaccesser.selectQuery("listsql", con, null);
var i=0;
for(i=0;i<resultList.size();i++) {
	serverout.write("<tr>\n");
	var record = resultList.get(i);
	var keys = record.keySet().toArray();
	for(var j=0;j<keys.length;j++) {
		var key = keys[j];
		serverout.write("<td>"+helper.HTMLEncode(record.get(key))+"</td>\n");
	}
	serverout.write("</tr>");
}
con.rollback();
con.close();
</serverscript>
</table>
<form action="DBInsertResult.es" method="post">
<table border='0'>
<tr>
<td>no:</td><td><input type='text' name='no' value=''/></td>
</tr><tr>
<td>name:</td><td><input type='text' name='name' value=''/></td>
</tr>
</table>
<input type='submit' name='submit' value='登録'/>
</form>
<br><br>
<br/>
<a href='../DBTest.es'>戻る</a>
</body>
</html>