package ueditor.upload;


import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ueditor.PathFormat;
import ueditor.define.AppInfo;
import ueditor.define.BaseState;
import ueditor.define.FileType;
import ueditor.define.State;
import ueditorOSS.util.ApiUrl;
import ueditorOSS.util.UploadOSSUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BinaryUploader {
	private static Logger logger = LogManager.getLogger(BinaryUploader.class);

	public static final State save(HttpServletRequest request, Map<String, Object> conf, String rootPath) {
		FileItemStream fileStream = null;
		boolean isAjaxUpload = request.getHeader("X_Requested_With") != null;

		if (!ServletFileUpload.isMultipartContent(request)) {
			return new BaseState(false, AppInfo.NOT_MULTIPART_CONTENT);
		}

		ServletFileUpload upload = new ServletFileUpload(
				new DiskFileItemFactory());

		if (isAjaxUpload) {
			upload.setHeaderEncoding("UTF-8");
		}

		try {
			FileItemIterator iterator = upload.getItemIterator(request);

			while (iterator.hasNext()) {
				fileStream = iterator.next();

				if (!fileStream.isFormField())
					break;
				fileStream = null;
			}

			if (fileStream == null) {
				return new BaseState(false, AppInfo.NOTFOUND_UPLOAD_DATA);
			}

			String savePath = (String) conf.get("savePath");// 保存路径【imagePathFormat】
			String originFileName = fileStream.getName(); // 图片流
			String suffix = FileType.getSuffixByFilename(originFileName);// 取到后缀名

			originFileName = originFileName.substring(0,
					originFileName.length() - suffix.length());
			savePath = savePath + suffix;

			long maxSize = ((Long) conf.get("maxSize")).longValue();

			if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
				return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
			}

			savePath = PathFormat.parse(savePath, originFileName);// 保存路径真实生成的路径

			String physicalPath = (String) conf.get("rootPath") + savePath;

			InputStream is = fileStream.openStream();
			// State storageState = StorageManager.saveFileByInputStream(is,
			// physicalPath, maxSize); //此行注释掉
			State storageState = null;

			/**
			 * 上传到阿里云OSS by lichenyi
			 * 
			 */
			// *******************开始***********************
			String fileName = new StringBuffer().append(new Date().getTime()).append(fileStream.getName().substring(fileStream.getName().indexOf("."))).toString();
			try {

				// 创建上传Object的Metadata
				UploadOSSUtil.createTempFile(rootPath+fileName, is);
				UploadOSSUtil.uploadImgAliyun(new File(rootPath+fileName), fileName);
				UploadOSSUtil.removeTempFile(rootPath+fileName);
				storageState = StorageManager.saveFileByInputStream(is, physicalPath, maxSize);
				//storageState.putInfo("state", "SUCCESS");// UEDITOR的规则:不为SUCCESS则显示state的内容
				// 注意：下面的url是返回到前端访问文件的路径，请自行修改
				String host = "http://" + ApiUrl.bucket + "." + ApiUrl.endpoint;
				storageState.putInfo("url", host + "/"+ ApiUrl.myPath + fileName);
				storageState.putInfo("title", fileName);
				storageState.putInfo("original", fileName);
			} catch (Exception e) {
				// TODO: handle exception
				logger.error(e.getMessage());
				storageState.putInfo("state", "文件上传失败!");
				storageState.putInfo("url", "");
				storageState.putInfo("title", "");
				storageState.putInfo("original", "");
				logger.error("文件 " + fileName + " 上传失败!");
			}
			// ********************结束**********************

			is.close();
			if (storageState.isSuccess()) {
				//storageState.putInfo("url", PathFormat.format(savePath));
				storageState.putInfo("type", suffix);
				storageState.putInfo("original", originFileName + suffix);
			}

			return storageState;
		} catch (FileUploadException e) {
			return new BaseState(false, AppInfo.PARSE_REQUEST_ERROR);
		} catch (IOException e) {
		}
		return new BaseState(false, AppInfo.IO_ERROR);
	}

	private static boolean validType(String type, String[] allowTypes) {
		List<String> list = Arrays.asList(allowTypes);

		return list.contains(type);
	}

	public static final String getContentType(String fileName) {
		String fileExtension = fileName.substring(fileName.lastIndexOf("."));
		if ("bmp".equalsIgnoreCase(fileExtension))
			return "image/bmp";
		if ("gif".equalsIgnoreCase(fileExtension))
			return "image/gif";
		if ("jpeg".equalsIgnoreCase(fileExtension)
				|| "jpg".equalsIgnoreCase(fileExtension)
				|| "png".equalsIgnoreCase(fileExtension))
			return "image/jpeg";
		if ("html".equalsIgnoreCase(fileExtension))
			return "text/html";
		if ("txt".equalsIgnoreCase(fileExtension))
			return "text/plain";
		if ("vsd".equalsIgnoreCase(fileExtension))
			return "application/vnd.visio";
		if ("ppt".equalsIgnoreCase(fileExtension)
				|| "pptx".equalsIgnoreCase(fileExtension))
			return "application/vnd.ms-powerpoint";
		if ("doc".equalsIgnoreCase(fileExtension)
				|| "docx".equalsIgnoreCase(fileExtension))
			return "application/msword";
		if ("xml".equalsIgnoreCase(fileExtension))
			return "text/xml";
		return "text/html";
	}
}
