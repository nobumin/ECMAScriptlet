<html>
<head>
<meta http-equiv=Content-Type content="text/html; charset=shift_jis">
<title>DB Test</title>
</head>
<body>
サンプルテーブル定義<br/>
<pre>
CREATE TABLE SampleTable 
(
  No    integer,
  Name  varchar(20)
);
CREATE UNIQUE INDEX sampletable_idx
    ON sampletable (no);
</pre>
<br/><br/>
<a href="DBList.es">sample tableのレコードを取得</a><br/>
<a href="DB_INSERT/DBInsert.es">sample tableへレコードを追加</a><br/>
<br/>
<a href='.'>戻る</a>
</body>
</html>
