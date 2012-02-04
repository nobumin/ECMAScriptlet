<html>
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<body>
<serverscript>
var col1 = mongodb.getCollection("test1");
serverout.write("test1<br/>");
serverout.write(col1.findOne());
serverout.write("<hr/>");

var col2 = mongodb.getCollection("test2");
serverout.write("test2<br/>");
var docRemove = mongodb.createDBObject();
docRemove.put("b",66);
col2.remove(docRemove);
docRemove = mongodb.createDBObject();
docRemove.put("b",77);
col2.remove(docRemove);
docRemove = mongodb.createDBObject();
docRemove.put("b",88);
col2.remove(docRemove);
var cur = col2.find();
while(cur.hasNext()) {
	serverout.write(cur.next());
	serverout.write("<br/>");
}
var doc = mongodb.createDBObject();
doc.put("b",66);
col2.insert(doc);
doc = mongodb.createDBObject();
doc.put("b",77);
col2.insert(doc);
doc = mongodb.createDBObject();
doc.put("b",88);
col2.insert(doc);
serverout.write("--------insert--------<br/>");

var query = mongodb.createDBQuery();
query.put("b", 77);
var cur = col2.find(query);

while(cur.hasNext()) {
	serverout.write(cur.next());
	serverout.write("<br/>");
}


</serverscript>
<br>
<a href='.'>戻る</a>
</body>
</html>