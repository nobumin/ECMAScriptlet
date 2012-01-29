<html>
<META http-equiv="Content-Type" content="text/html; charset=shift-jis"/>
<script>
	function addCookie() {
		var d = new Date();
		var value = d.getHours() + "h" + d.getMinutes() + "m" + d.getSeconds() + "s";
		document.cookie="start="+value;
	}
</script>
<body onload="addCookie()">
body要素のonload属性に対応する属性”onloadserver”<br/>
およびscript要素のsrc属性に対応する、serverscrip要素のsrc属性をサポートしました。<br/>
またグローバルオブジェクトにHTML自身を示す"document"オブジェクトが追加されました。<br/>
この"document"オブジェクトはonloadserverで呼出される関数内のでのみ利用可能です<br/>
onloadserverイベントは、生成されたHTMLに対する処理を実施する場合に利用します。<br/>
例えば、document操作により、任意の要素に子要素を追加する場合や<br/>
任意の要素の属性の値を変更する際に利用します。<br/>
<a href="DocumentTestResult.es?test=test">サンプルを実行</a>
<br><br>
<a href='.'>戻る</a>
</body>
</html>