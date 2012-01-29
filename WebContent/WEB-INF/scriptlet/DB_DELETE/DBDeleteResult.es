<html>
<head>
<META http-equiv="Content-Type" content="text/html; charset=Shift-jis">
<title>DB Delete Result</title>
</head>
<body>
<serverscript>
var no = helper.to8859(request.getParameter("no"));
var name = helper.to8859(request.getParameter("name"));
var param = dbaccesser.createIntDBParam(new Number(no));
var params = new Array();
params[0] = param;
var con = dbaccesser.getConnection();
dbaccesser.updateQuery("deletesql", con, params);
con.commit();
con.close();
name = helper.HTMLEncode(name);
serverout.write("no:"+no+"<br/>\n");
serverout.write("name:"+name+"<br/>\n");
</serverscript>
上記を削除しました。
<br><br>
<br/>
<a href='../DBTest.es'>戻る</a>
</body>
</html>