<!--
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
-->
<!-- html xmlns="http://www.w3.org/1999/xhtml" -->
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=shift-jis"/>
<title>test</title>
<serverscript language="JavaScript" type="text/javascript" src="DocumentTest.js"/>
</head>
<script>
	function init() {
		alert(document.cookie);
	}
</script>
<body onload="init()" onloadserver="getValue()">
Document test<br/>
<br/>
<serverscript>
var testString = new java.lang.String("getValue()�ɂ��Adiv�v�f�Ƀe�L�X�g�m�[�h���ǉ�����܂��B");
serverout.write(testString);
serverout.write("<br/>");
</serverscript>
<br/>
<a href='DocumentTest.es'>�߂�</a>
<serverscript>
serverout.write("<div id='doctest'>document���T�|�[�g���܂���</div>\n");
</serverscript>
</body>
</html>