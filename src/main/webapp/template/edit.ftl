<!doctype html>
<html>

	<@base.header  title="文章内容" ></@>
	<link rel="stylesheet" type="text/css" href="static/css/oss-h5-upload-style.css">
	<link rel="stylesheet" type="text/css" href="static/css/content.css">
	<!-- <script src="https://lib.sinaapp.com/js/jquery/2.0.2/jquery-2.0.2.min.js"></script> -->
	<script src="static/assets/js/jquery.min.js"></script>
	
	<script>
		$(document).ready(function(){
			$('#selectfiles').click(function(){
				console.log("selectFiles");
			});
			$('#uploadImg').click(function(){
				$('#selectfiles').click();
			});
		});
		
		function checkForm(){
			var con = getContent();
			console.log(con);
			$('#content').val(con);
			return true;
		}
	</script>
	<body data-type="generalComponents">
		<#include "base/headerBody.ftl"/>

		<div class="tpl-page-container tpl-page-header-fixed">
			
			<#include "base/nav.ftl"/> 

			<div class="tpl-content-wrapper">
				<div class="tpl-content-page-title">
					文章详情
				</div>

				<div class="tpl-portlet-components">
					<div class="tpl-block ">
						<div class="am-g tpl-amazeui-form">
							<div class="am-u-sm-12 am-u-md-9">
								<form action="${contextPath!""}manager/article/CreateArticle" method="POST" class="am-form am-form-horizontal" onsubmit="return checkForm()">
									<input type="text" style="display:none" name="id" value="${articleData.id!""}" />
									<div class="am-form-group">
										<label for="user-name" class="am-u-sm-3 am-form-label">标题(*)</label>
										<div class="am-u-sm-9">
											<input type="text" name="title" value="${articleData.title!""}" placeholder="标题" required />
										</div>
									</div>

									<div class="am-form-group">
										<label for="user-name" class="am-u-sm-3 am-form-label">封面图片</label>
										<div class="am-u-sm-9">
											<div class="inputImg">
                                            	<input type="text" id="inputImg" name="titlePic" value="${articleData.titlePic!""}" placeholder="点击此处上传封面图片" required oninvalid="setCustomValidity('请上传图片')"/>
												<div class="uploadImg">
	                                            	<img class="am-radius" id="uploadImg" alt="" src="${articleData.titlePic!""}" width="140" height="140" required/>
												</div>
											</div>
											<div style="display:none1;">
                                                <div id="ossfile"></div>
                                                <div id="container">
                                                	<a id="selectfiles" href="javascript:void(0);" class='am-btn am-btn-warning am-btn-sm'>选择图片</a>
                                                	<a id="postfiles" href="javascript:void(0);" class='am-btn am-btn-danger am-btn-sm'><i class="am-icon-cloud-upload"></i>开始上传</a>
                                                </div>
                                            </div>
										</div>
									</div>
 
									<div class="am-form-group">
										<label for="user-name" class="am-u-sm-3 am-form-label">作者</label>
										<div class="am-u-sm-9">
											<input type="text" name="author" value="${articleData.author!""}" placeholder="作者" required oninvalid="setCustomValidity('请填写作者')"/>
										</div>
									</div>

									<div class="am-form-group">
										<label for="user-name" class="am-u-sm-3 am-form-label">来源</label>
										<div class="am-u-sm-9">
											<input type="text" name="source" value="${articleData.source!""}" placeholder="来源" />
										</div>
									</div>

									<div class="am-form-group">
										<label for="user-name" class="am-u-sm-3 am-form-label">标签</label>
										<div class="am-u-sm-9">
											<#assign tagg=""/>
											<#if articleData.tags??>
												<#list articleData.tags as tag>
													<#if tag_index &gt; 0>
														<#assign  tagg=tagg+","/>
													</#if> 
													<#assign  tagg=tagg+tag/>
												</#list>
											</#if>
											<input type="text" name="tag" value="${tagg!""}" placeholder="文章标签请用英文逗号隔开" required oninvalid="setCustomValidity('请填写标签')"/>
										</div>
									</div>
									
									<div class="am-form-group">
										<label for="user-intro" class="am-u-sm-3 am-form-label">简介</label>
										<div class="am-u-sm-9">
											<textarea class="" rows="5" id="user-intro" name="summary" placeholder="输入简介" >${articleData.summary!""}</textarea>
										</div>
									</div>
									
									<div class="am-form-group">
										<label for="user-name" class="am-u-sm-3 am-form-label">文章链接地址</label>
										<div class="am-u-sm-9">
											<input type="text" name="url" value="${articleData.url!""}" placeholder="链接地址" />
										</div>
									</div>
									
									<div class="am-form-group">
										<label for="user-name" class="am-u-sm-3 am-form-label">文章内容</label>
										<div class="am-u-sm-9">
											<input type="text" name="content" id="content" value='${articleData.content!""}' style="display:none;"/>
											<!-- 富文本 -->
											<script type="text/javascript" charset="utf-8" src="static/UEditor/ueditor.config.js"></script>
										    <script type="text/javascript" charset="utf-8" src="static/UEditor/ueditor.all.min.js"> </script>
										    <script type="text/javascript" charset="utf-8" src="static/UEditor/lang/zh-cn/zh-cn.js"></script>
										    
											<script id="editor" type="text/plain" style="width:1024px;height:500px;"></script>
											<script type="text/javascript">
												var um = UE.getEditor('editor', {
													    autoHeight: false,
													    imageActionName: "uploadimage", /* 执行上传图片的action名称 */
													    imageFieldName: "upfile", /* 提交的图片表单名称 */
													    imageMaxSize: 2048000, /* 上传大小限制，单位B */
													    imageAllowFiles: [".png", ".jpg", ".jpeg", ".gif", ".bmp"], /* 上传图片格式显示 */
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

									<div class="am-form-group">
										<div class="am-u-sm-9 am-u-sm-push-3">
											<button type="submit" id="subForm" class="am-btn am-btn-primary">保存修改</button>
										</div>
									</div>
								</form>
							</div>
						</div>
					</div>

				</div>

			</div>

		</div>
		
		<script src="static/assets/js/amazeui.min.js"></script>
		<script src="static/assets/js/app.js"></script>
		<script type="text/javascript" src="static/js/plupload.full.min.js"></script>
		<script type="text/javascript" src="static/js/upload.js"></script>
	
	</body>
	<script>
		$(document).ready(function(){
			um.addListener("ready", function () {
		        setContent('${articleData.content!""}');
			});
		});
	</script>
</html>