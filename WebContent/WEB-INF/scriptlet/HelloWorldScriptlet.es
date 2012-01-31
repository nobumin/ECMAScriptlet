<html>
<META http-equiv="Content-Type" content="text/html; charset=shift-jis">
<body>
HelloWorld<br>
こんにちは。<br>
<serverscript>
sysout.println(helper.toUTF8("シスあうと"));
syserr.println(helper.toUTF8("シスエラー"));
var chHello = request.getParameter("value");
if(chHello) {
serverout.write(helper.to8859(chHello));
}else{
serverout.write("NO PARAM");
}
serverout.write("<br>");
var testString = new java.lang.String("テストメッセージです。");
serverout.write(testString);
serverout.write("<br>");
serverout.write("USER AGENT:"+navigator.userAgent);
</serverscript>
<br><br>
<img src='images/test_image.gif'><br><br>
<a href='.'>戻る</a>
</body>
</html>