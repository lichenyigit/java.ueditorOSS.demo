<!doctype html>
<html>

<link rel="stylesheet" type="text/css" href="static/css/oss-h5-upload-style.css">
<link rel="stylesheet" type="text/css" href="static/css/content.css">
<!-- <script src="https://lib.sinaapp.com/js/jquery/2.0.2/jquery-2.0.2.min.js"></script> -->
<script src="static/assets/js/jquery.min.js"></script>

<script>
    $(document).ready(function () {
        $('#selectfiles').click(function () {
            console.log("selectFiles");
        });
        $('#uploadImg').click(function () {
            $('#selectfiles').click();
        });
    });

    function checkForm() {
        var con = getContent();
        console.log(con);
        $('#content').val(con);
        return true;
    }
</script>
<body data-type="generalComponents">

<div class="tpl-page-container tpl-page-header-fixed">
    <div class="tpl-content-wrapper">
        <div class="tpl-content-page-title">
            上传图片
        </div>

        <div class="tpl-portlet-components">
            <div class="tpl-block ">
                <div class="am-g tpl-amazeui-form">
                    <div class="am-u-sm-12 am-u-md-9">
                        <form action="" method="POST"
                              class="am-form am-form-horizontal" onsubmit="return checkForm()">
                            <div class="am-form-group">
                                <div class="am-u-sm-9">
                                    <!-- 富文本 -->
                                    <script type="text/javascript" charset="utf-8"
                                            src="static/UEditor/ueditor.config.js"></script>
                                    <script type="text/javascript" charset="utf-8"
                                            src="static/UEditor/ueditor.all.min.js"></script>
                                    <script type="text/javascript" charset="utf-8"
                                            src="static/UEditor/lang/zh-cn/zh-cn.js"></script>

                                    <script id="editor" type="text/plain" style="width:1024px;height:500px;"></script>
                                    <script type = "text/javascript" >
                                            //UE.getEditor('editor');
                                    UE.getEditor('editor', {
                                        allowDivTransToP: false
                                        /*autoHeight: false,
                                        imageActionName: "uploadimage", /!* 执行上传图片的action名称 *!/
                                        imageFieldName: "upfile", /!* 提交的图片表单名称 *!/
                                        imageMaxSize: 2048000, /!* 上传大小限制，单位B *!/
                                        imageAllowFiles: [".png", ".jpg", ".jpeg", ".gif", ".bmp"], /!* 上传图片格式显示 *!/*/
                                    });

                                    function getContent() {
                                        var arr = [];
                                        arr.push(UE.getEditor('editor').getContent());
                                        return arr.join("<br>");
                                    }

                                    function setContent(isAppendTo) {
                                        var arr = [];
                                        UE.getEditor('editor').setContent(isAppendTo);
                                    }
                                    </script>
                                </div>
                            </div>
                    </div>
                </div>
            </div>

        </div>

    </div>

</div>

</body>
<script>
    $(document).ready(function () {
        UE.getEditor('editor').addListener("ready", function () {
            setContent('');
        });
    });
</script>
</html>