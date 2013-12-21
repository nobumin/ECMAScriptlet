<html>
<serverscript>
response.addHeader("Pragma","no-cache");
response.addHeader("Cache-Control","no-store, no-cache, must-revalidate, max-age=0");
response.addHeader("Expires","-1");
</serverscript>
<head>
<meta http-equiv=Content-Type content="text/html; charset=shift_jis">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Cache-Control" content="no-cache">
<meta http-equiv="Cache-Control" content="no-store">
<meta http-equiv="Expires" content="-1">
<title>はじめに</title>
</head>
<body>
<h3>ECMAScriptlet β版</h3>
<a href="https://github.com/nobumin/ECMAScriptlet" target="_blank">プロジェクトホーム</a><br><br><br>
このWebアプリケーションには、以下のライブラリが含まれています。<br>
Rhino 1.7R1<br>
<br><br>
Rhinoは MPL 1.1/GPL 2.0で保護されたライブラリです。<br>
全ての権利は、MoziLla Foundationが保有します。<br>
<a href='http://www.mozilla-japan.org/rhino/' target="_blank">Rhino</a><br><br>
E4X サポートは、Java 1.5 でネイティブにサポートされた DOM3 API にのみ対応しています。<br>
以前のバージョンとは異なりますので、ご注意ください。(E4X機能デモを参照)<br><br>
スクリプトレットの文字コード(METAタグ設定)を認識し、コンテンツの文字コードを自動で設定するようにしました。<br>
シーケンス制御に関して改変しました。サイトマップを設定ファイル化してシーケンスを監視する仕様に変更されています。<br>
汎用的なDBアクセス部をじっそうしました。ただし、HTML5.0仕様における、クライアントサイドDBの実装はまだできておりません。<br>
<br><br>
ECMAScriptletは未完成のアプリケーションフレームワークです。<br>
クリティカルな業務での利用はされませんよう、よろしくお願いします。<br>
万が一、当フレームワークを利用し、障害等発生した場合、<br>
当方では、いっさいの責任は負いませんので、ご利用に際しては、十分にご考慮ください。<br>
LAMPからの移行を意識した、アプリケーション形態が最終的な目標です。<br>
「PHPよりも気軽に！」を目指し、デザイナーさんの環境選択肢に入る目論みもあります。<br>
また、Ruby on Railsも意識しなくてはならないですが、まだまだの状態です。<br>
<br><br>
<!--
<h3>バイナリ</h3>
war:<a href="bin/ecmascriptlet.war" target="_blank">ダウンロード</a><br>
<br>
jar:<a href="bin/ecmascriptlet.jar" target="_blank">ダウンロード</a><br>
<br>
-->
<h3>Scriptlet</h3>
<a href="aboutscriptlet.html" target="_blank">スクリプトレットについて</a><br>
各デモスクリプトレットは、WEB-INF内のscriptletフォルダがデフォルトの格納先になりますが、<br>
config.xmlの設定により任意のフォルダを指定できます。<br>
<br>
<h3>画面遷移</h3>
画面遷移がサイトマップ定義（sitemap.xml）にて管理されています。<br>
サイトマップ定義で許可されていないページへのアクセス（主に直リンク）に関しては、<br>
４０３エラーでアクセスが制限されます。<br>
例外メッセージ：Invalid sequence detected.<br>
<br>
<h3>Javadoc</h3>
<a href="doc/index.html" target="_blank">Javadoc-api</a><br>
<br>
<h3>DEMO</h3>
<a href="HelloWorldScriptlet.es?value=イ尓好">HelloWorld</a><br>
<a href="ValidateForm.es">入力チェックデモ</a><br>
<a href="ExceptionTest.es">例外時動作デモ</a><br>
<serverscript>
serverout.write("<a href='"+helper.createNoCookieURL("ListScroll.es")+"?next=0' >リスト表示デモ</a>\n");
</serverscript>
<br>
<a href="LiveConnect.es">Rhino LiveConnect機能デモ</a><br>
<a href="e4x.es">E4X機能デモ</a><br>
<a href="DynamicLoad.es">外部スクリプトの動的ロード機能デモ</a><br>
<a href="DocumentTest.es">onloadイベントの利用およびdocumentオブジェクトの利用デモ</a><br>
<!-- a href="DBTest.es">汎用DBオブジェクト”dbaccesser”の利用デモ</a><br -->
<!-- a href="MongoDemo.es">MongoDBオブジェクト”mongodb”の利用デモ</a><br -->
<a href="WebSocket.es">WebSocketとMongoDBの連携デモ</a><br>
<!-- cache control for safari -->
<iframe style="height:0px;width:0px;visibility:hidden" src="about:blank">
    this frame prevents back forward cache
</iframe>
</body>
</html>
