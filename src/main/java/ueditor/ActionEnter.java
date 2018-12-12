package ueditor;

import com.alibaba.fastjson.JSON;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import ueditor.define.*;
import ueditorOSS.util.UploadOSSUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionEnter {
    private HttpServletRequest request = null;

    private String rootPath = null;
    private String contextPath = null;
    private String source = null;//截图上传
    private String actionType = null;

    private ConfigManager configManager = null;

    public ActionEnter(HttpServletRequest request, String rootPath, String configJsonPath) {
        this.request = request;
        this.rootPath = rootPath;
        this.actionType = request.getParameter("action");
        this.contextPath = request.getContextPath();
        this.configManager = ConfigManager.getInstance(this.rootPath, this.contextPath, configJsonPath);
    }

    public ActionEnter(HttpServletRequest request, String source, String rootPath, String configJsonPath) {
        this.request = request;
        this.source = source;
        this.rootPath = rootPath;
        this.actionType = request.getParameter("action");
        this.contextPath = request.getContextPath();
        this.configManager = ConfigManager.getInstance(this.rootPath, this.contextPath, configJsonPath);
    }

    public String exec() {
        return this.invoke();
    }

    public String invoke() {

        if (actionType == null || !ActionMap.mapping.containsKey(actionType)) {
            return new BaseState(false, AppInfo.INVALID_ACTION).toJSONString();
        }

        if (this.configManager == null || !this.configManager.valid()) {
            return new BaseState(false, AppInfo.CONFIG_ERROR).toJSONString();
        }

        int actionCode = ActionMap.getType(this.actionType);

        Map<String, Object> resultMap = new HashMap<String, Object>();
        String url;
        switch (actionCode) {

            case ActionMap.CONFIG:
                return this.configManager.getAllConfig().toString();

            case ActionMap.UPLOAD_IMAGE:
            case ActionMap.UPLOAD_SCRAWL:
            case ActionMap.UPLOAD_VIDEO:
            case ActionMap.UPLOAD_FILE:
                FileItemStream fileItemStream = getFileItemStream(request);
                url = UploadOSSUtil.uploadImgAliyun(fileItemStream);
                resultMap.put("type", fileItemStream.getContentType());//文件类型
                resultMap.put("original", fileItemStream.getFieldName());//文件名称
                resultMap.put("url", url);//oss访问连接
                break;

            case ActionMap.CATCH_IMAGE:
                /* 此处代码根据文档写是错误的，所以自己手动解析了js，重新按照js解析的格式进行拼装 */
                List<Map<String, String>> list = new ArrayList<>();
                Map<String, String> itemMap = new HashMap<>();

                Map<String, String> map = UploadOSSUtil.download(source,  rootPath);//将网络端文件下载到本地
                String filename = map.get("filename");//文件名
                String localPath = map.get("localPath");//本地路径
                String suffix = map.get("suffix");//文件后缀
                File file = new File(localPath);
                url = UploadOSSUtil.uploadImgAliyun(file);//本地文件上传到阿里云，并返回oss连接
                UploadOSSUtil.removeTempFile(localPath);
                itemMap.put("url", url);//阿里云上传路径
                itemMap.put("source", source);//截图原路径
                itemMap.put("state", "SUCCESS");
                list.add(itemMap);
                resultMap.put("list", list);
                break;

            case ActionMap.LIST_IMAGE:
            case ActionMap.LIST_FILE:
                break;

        }

        /**
         * add by lichenyi
         */
        resultMap.put("state", "SUCCESS");
        return JSON.toJSONString(resultMap);
    }

    public int getStartIndex() {

        String start = this.request.getParameter("start");

        try {
            return Integer.parseInt(start);
        } catch (Exception e) {
            return 0;
        }

    }

    /**
     * callback参数验证
     */
    public boolean validCallbackName(String name) {

        if (name.matches("^[a-zA-Z_]+[\\w0-9_]*$")) {
            return true;
        }

        return false;

    }

    private FileItemStream getFileItemStream(HttpServletRequest request){
        FileItemStream fileStream = null;
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());

        try {
            FileItemIterator iterator = upload.getItemIterator(request);

            while (iterator.hasNext()) {
                fileStream = iterator.next();

                if (!fileStream.isFormField())
                    break;
                fileStream = null;
            }
            return  fileStream;

        }catch (Exception e){
            e.printStackTrace();
        }
        return  fileStream;
    }

}