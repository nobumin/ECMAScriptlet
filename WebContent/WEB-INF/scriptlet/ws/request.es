<serverscript>

var queryString = jsonReq.query.replaceAll("'", '"');
var queryObj = JSON.parse(queryString);
jsonReq.setSession(queryString);
jsonReq.setResult('{"message":"accept sid '+queryObj.sid+'"}');

</serverscript>
