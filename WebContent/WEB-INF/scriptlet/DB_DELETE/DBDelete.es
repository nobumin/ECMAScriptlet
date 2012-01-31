<html>
<serverscript>
response.addHeader("Pragma","no-cache");
response.addHeader("Cache-Control","no-store, no-cache, must-revalidate, max-age=0");
response.addHeader("Expires","-1");
</serverscript>
<head>
<META http-equiv="Content-Type" content="text/html; charset=Shift-jis">
<META http-equiv="Pragma" content="no-cache">
<META http-equiv="Cache-Control" content="no-cache">
<meta http-equiv="Cache-Control" content="no-store">
<meta http-equiv="Expires" content="-1">
<title>DB Delete</title>
</head>
<body>
以下のレコードを削除します。よろしいですか？<br/>
<form action='DBDeleteResult.es' method='post'>
<serverscript>
var no = helper.to8859(request.getParameter("no"));
var name = helper.to8859(request.getParameter("name"));
name = helper.HTMLEncode(name);

serverout.write("no:"+no+"<br/>\n");
serverout.write("name:"+name+"<br/>\n");
serverout.write("<input type='hidden' name='no' value='"+no+"' />\n");
serverout.write("<input type='hidden' name='name' value='"+name+"' />\n");
</serverscript>
<input type='submit' name='submit' value='OK' />
</form>
<br><br>
<br/>
<a href='../DBList.es'>戻る</a>
<!-- cache control for safari -->
<iframe style="height:0px;width:0px;visibility:hidden" src="about:blank">
    this frame prevents back forward cache
</iframe>
</body>
</html>