<html>
<META http-equiv="Content-Type" content="text/html; charset=shift-jis">
<body>
HelloWorld<br>
����ɂ��́B<br>
<serverscript>
sysout.println(helper.toUTF8("�V�X������"));
syserr.println(helper.toUTF8("�V�X�G���["));
var chHello = request.getParameter("value");
if(chHello) {
serverout.write(helper.to8859(chHello));
}else{
serverout.write("NO PARAM");
}
serverout.write("<br>");
var testString = new java.lang.String("�e�X�g���b�Z�[�W�ł��B");
serverout.write(testString);
serverout.write("<br>");
serverout.write("USER AGENT:"+navigator.userAgent);
</serverscript>
<br><br>
<img src='images/test_image.gif'><br><br>
<a href='.'>�߂�</a>
</body>
</html>