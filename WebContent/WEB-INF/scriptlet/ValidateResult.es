<html>
<head>
<META http-equiv="Content-Type" content="text/html; charset=utf-8">
</head>
<body>
<servervalidation>
numtext=\d{3},true
knjitext=[\p{InCJKUnifiedIdeographs}\p{InHiragana}]{4}
katakanatext=[\p{InKatakana}\p{InHalfwidthAndFullwidthForms}]{1,3}
</servervalidation>
数字<br>
<serverscript>
var numvalue = request.getParameter("numtext");
serverout.write(numvalue);
</serverscript>
<br><br>漢字＆ひらがな<br>
<serverscript>
var kanjivalue = request.getParameter("knjitext");
serverout.write(helper.to8859(kanjivalue));
</serverscript>
<br><br>カタカナ<br>
<serverscript>
var katakanavalue = request.getParameter("katakanatext");
serverout.write(helper.to8859(katakanavalue));
</serverscript>
<br><br>
<a href='ValidateForm.es'>戻る</a>
</body>
</html>