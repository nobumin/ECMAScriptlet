<html>
<head>
<META http-equiv="Content-Type" content="text/html; charset=Shift-jis">
<title>�X�N���[���ƃV�[�P���X����h�L�������g</title>
</head>
<servervalidation>
next=\d+
</servervalidation>
<body>
<table border='1'>
<serverscript>
var start=0;
var next = request.getParameter("next");
if(next != null) {
  start = next;
}
csvdatas.setParam(start,3);

while(csvdatas.hasNextTitle()) {
  var tbtitle = csvdatas.nextTitle();
  serverout.write("<tr>");
  serverout.write("<td>"+tbtitle+"</td>");
  while(csvdatas.hasNextValue(tbtitle)) {
    var tbvalue = csvdatas.nextValue(tbtitle);
    serverout.write("<td>"+helper.toUTF8(tbvalue)+"</td>");
  }
  serverout.write("</tr>");
}
</serverscript>
</table>
<br><br>
<serverscript>
serverout.write("<form action='"+helper.createNoCookieURL("ListScroll.es")+"' method='post'>\n");
</serverscript>
<serverscript>
var next = new Number(request.getParameter("next"));
if(!csvdatas.isTop()) {
  next -= 3;
  serverout.write("<input type='hidden' value='");
  serverout.write(next.toString());
  serverout.write("' name='next'>");
  serverout.write("<input type='submit' value='�O��'>"); 
}
</serverscript>
</form>�@�@�@
<serverscript>
serverout.write("<form action='"+helper.createNoCookieURL("ListScroll.es")+"' method='post'>\n");
</serverscript>
<serverscript>
var next = new Number(request.getParameter("next"));
if(next != null && !csvdatas.isLast()) {
  next += 3;
  serverout.write("<input type='hidden' value='");
  serverout.write(next.toString());
  serverout.write("' name='next'>");
  serverout.write("<input type='submit' value='����'>");
}
</serverscript>
</form>
<br><br><br>
<serverscript>
serverout.write("<a href='"+helper.createNoCookieURL("/ses/")+"'>�߂�</a>");
</serverscript>
</body>
</html>