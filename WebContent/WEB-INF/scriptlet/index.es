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
<title>�͂��߂�</title>
</head>
<body>
<h3>ECMAScriptlet ����</h3>
<a href="https://github.com/nobumin/ECMAScriptlet" target="_blank">�v���W�F�N�g�z�[��</a><br><br><br>
����Web�A�v���P�[�V�����ɂ́A�ȉ��̃��C�u�������܂܂�Ă��܂��B<br>
Rhino 1.7R1<br>
<br><br>
Rhino�� MPL 1.1/GPL 2.0�ŕی삳�ꂽ���C�u�����ł��B<br>
�S�Ă̌����́AMoziLla Foundation���ۗL���܂��B<br>
<a href='http://www.mozilla-japan.org/rhino/' target="_blank">Rhino</a><br><br>
E4X �T�|�[�g�́AJava 1.5 �Ńl�C�e�B�u�ɃT�|�[�g���ꂽ DOM3 API �ɂ̂ݑΉ����Ă��܂��B<br>
�ȑO�̃o�[�W�����Ƃ͈قȂ�܂��̂ŁA�����ӂ��������B(E4X�@�\�f�����Q��)<br><br>
�X�N���v�g���b�g�̕����R�[�h(META�^�O�ݒ�)��F�����A�R���e���c�̕����R�[�h�������Őݒ肷��悤�ɂ��܂����B<br>
�V�[�P���X����Ɋւ��ĉ��ς��܂����B�T�C�g�}�b�v��ݒ�t�@�C�������ăV�[�P���X���Ď�����d�l�ɕύX����Ă��܂��B<br>
�ėp�I��DB�A�N�Z�X���������������܂����B�������AHTML5.0�d�l�ɂ�����A�N���C�A���g�T�C�hDB�̎����͂܂��ł��Ă���܂���B<br>
<br><br>
ECMAScriptlet�͖������̃A�v���P�[�V�����t���[�����[�N�ł��B<br>
�N���e�B�J���ȋƖ��ł̗��p�͂���܂���悤�A��낵�����肢���܂��B<br>
������A���t���[�����[�N�𗘗p���A��Q�����������ꍇ�A<br>
�����ł́A���������̐ӔC�͕����܂���̂ŁA�����p�ɍۂ��ẮA�\���ɂ��l�����������B<br>
LAMP����̈ڍs���ӎ������A�A�v���P�[�V�����`�Ԃ��ŏI�I�ȖڕW�ł��B<br>
�uPHP�����C�y�ɁI�v��ڎw���A�f�U�C�i�[����̊��I�����ɓ���ژ_�݂�����܂��B<br>
�܂��ARuby on Rails���ӎ����Ȃ��Ă͂Ȃ�Ȃ��ł����A�܂��܂��̏�Ԃł��B<br>
<br><br>
<!--
<h3>�o�C�i��</h3>
war:<a href="bin/ecmascriptlet.war" target="_blank">�_�E�����[�h</a><br>
<br>
jar:<a href="bin/ecmascriptlet.jar" target="_blank">�_�E�����[�h</a><br>
<br>
-->
<h3>Scriptlet</h3>
<a href="aboutscriptlet.html" target="_blank">�X�N���v�g���b�g�ɂ���</a><br>
�e�f���X�N���v�g���b�g�́AWEB-INF����scriptlet�t�H���_���f�t�H���g�̊i�[��ɂȂ�܂����A<br>
config.xml�̐ݒ�ɂ��C�ӂ̃t�H���_���w��ł��܂��B<br>
<br>
<h3>��ʑJ��</h3>
��ʑJ�ڂ��T�C�g�}�b�v��`�isitemap.xml�j�ɂĊǗ�����Ă��܂��B<br>
�T�C�g�}�b�v��`�ŋ�����Ă��Ȃ��y�[�W�ւ̃A�N�Z�X�i��ɒ������N�j�Ɋւ��ẮA<br>
�S�O�R�G���[�ŃA�N�Z�X����������܂��B<br>
��O���b�Z�[�W�FInvalid sequence detected.<br>
<br>
<h3>Javadoc</h3>
<a href="doc/index.html" target="_blank">Javadoc-api</a><br>
<br>
<h3>DEMO</h3>
<a href="HelloWorldScriptlet.es?value=�C���D">HelloWorld</a><br>
<a href="ValidateForm.es">���̓`�F�b�N�f��</a><br>
<a href="ExceptionTest.es">��O������f��</a><br>
<serverscript>
serverout.write("<a href='"+helper.createNoCookieURL("ListScroll.es")+"?next=0' >���X�g�\���f��</a>\n");
</serverscript>
<br>
<a href="LiveConnect.es">Rhino LiveConnect�@�\�f��</a><br>
<a href="e4x.es">E4X�@�\�f��</a><br>
<a href="DynamicLoad.es">�O���X�N���v�g�̓��I���[�h�@�\�f��</a><br>
<a href="DocumentTest.es">onload�C�x���g�̗��p�����document�I�u�W�F�N�g�̗��p�f��</a><br>
<!-- a href="DBTest.es">�ėpDB�I�u�W�F�N�g�hdbaccesser�h�̗��p�f��</a><br -->
<!-- a href="MongoDemo.es">MongoDB�I�u�W�F�N�g�hmongodb�h�̗��p�f��</a><br -->
<a href="WebSocket.es">WebSocket��MongoDB�̘A�g�f��</a><br>
<!-- cache control for safari -->
<iframe style="height:0px;width:0px;visibility:hidden" src="about:blank">
    this frame prevents back forward cache
</iframe>
</body>
</html>
