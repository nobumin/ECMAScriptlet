<html>
<head>
<META http-equiv="Content-Type" content="text/html; charset=Shift-jis">
<title>DB Delete Result</title>
</head>
<body>
<servervalidation>
no=\d{1,5},true
name=.*,true
</servervalidation>
下記を追加しました。<br/>
<serverscript>
var no = request.getParameter("no");
var name = helper.to8859(request.getParameter("name"));
var paramNo = dbaccesser.createIntDBParam(new Number(no));
var paramName = dbaccesser.createStringDBParam(name);
var params = new Array();
params[0] = paramNo;
params[1] = paramName;
var con = dbaccesser.getConnection();
dbaccesser.updateQuery("insertsql", con, params);
con.commit();
con.close();
name = helper.HTMLEncode(name);
serverout.write("no:"+no+"<br/>\n");
serverout.write("name:"+name+"<br/>\n");
</serverscript>
<br><br>
<br/>
<a href='../DBTest.es'>戻る</a>
</body>
</html>