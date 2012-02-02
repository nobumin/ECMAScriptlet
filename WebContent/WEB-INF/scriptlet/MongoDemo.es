<html>
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<body>
<serverscript>
var col1 = mongodb.getCollection("test1");
var col2 = mongodb.getCollection("test2");
var col3 = mongodb.getCollection("test3");
var col4 = mongodb.getCollection("test4");
serverout.write("test1<br/>");
serverout.write(col1.findOne());
serverout.write("<hr/>");
serverout.write("test2<br/>");
serverout.write(col2.findOne());
serverout.write("<hr/>");
serverout.write("test3<br/>");
serverout.write(col3.findOne());
serverout.write("<hr/>");
serverout.write("test4<br/>");
serverout.write(col4.findOne());
</serverscript>
<br>
<a href='.'>戻る</a>
</body>
</html>