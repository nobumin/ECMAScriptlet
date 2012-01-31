<html>
<META http-equiv="Content-Type" content="text/html; charset=shift-jis">
<body>
外部スクリプトファイルを読み込むサンプル<br/>
@importで読み込んだ場合は、外部スクリプトにもサーバーサイドスクリプトを記述できます。<br/>
@importScriptで読み込んだ場合は、外部スクリプト内の定義を反映するのみです。（旧版）<br/>
<serverscript>
@import 'DynamicLoad2.js';
@importScript 'DynamicLoad.js';
serverout.write(getValue());
</serverscript>
<br>
<a href='DynamicLoad.es'>戻る</a>
</body>
</html>