package ueditorOSS.util;

import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UploadOSSUtil {
    private static Logger logger = LogManager.getLogger(UploadOSSUtil.class);

    public UploadOSSUtil() {
    }

    public static String uploadImgAliyun(File file) {
        String endpoint = ApiUrl.endpoint;
        String accessId = ApiUrl.accessId;
        String accessKey = ApiUrl.accessKey;
        String bucket = ApiUrl.bucket;
        String dir = ApiUrl.myPath;
        String fileName = file.getName();
        OSSClient client = new OSSClient(endpoint, accessId, accessKey);
        client.putObject(bucket, dir + "/" + fileName, file);
        client.shutdown();
        String url = "http://" + bucket + "." + endpoint + "/" + dir + "/" + fileName;
        return url;
    }

    public static String uploadImgAliyun(FileItemStream fileItemStream) {
        String endpoint = ApiUrl.endpoint;
        String accessId = ApiUrl.accessId;
        String accessKey = ApiUrl.accessKey;
        String bucket = ApiUrl.bucket;
        String dir = ApiUrl.myPath;
        String fileName = fileItemStream.getName();
        ;
        logger.info("开始上传" + fileName);
        fileName = dir + "/" + fileName;
        InputStream inputStream = null;
        try {
            inputStream = fileItemStream.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Long fileSize = inputStream.;
        //创建上传object的metadata
        ObjectMetadata metadata = new ObjectMetadata();
        //metadata.setContentLength(fileSize);
        metadata.setCacheControl("no-cache");
        metadata.setHeader("Pragma", "no-cache");
        metadata.setContentEncoding("utf-8");
        metadata.setContentType(fileItemStream.getContentType());
        metadata.setContentDisposition("filename=" + fileName);
        try {
            //上传文件
            OSSClient client = new OSSClient(endpoint, accessId, accessKey);
            PutObjectResult putresult = client.putObject(bucket, fileName, inputStream, metadata);
            logger.info(putresult.getETag());
            logger.info(JSON.toJSONString(putresult));
            String url = "http://" + bucket + "." + endpoint + "/" + fileName;
            return url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void createTempFile(String path, InputStream inputStream) {
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(path));
            int byteCount = 0;
            //1M逐个读取  
            byte[] bytes = new byte[1024 * 1024];
            while ((byteCount = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, byteCount);
            }
            inputStream.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    public static void removeTempFile(String path) {
        File file = new File(path);
        file.delete();
    }

    public static Map<String, String> download(String urlString, String savePath) {
        try {
            Map<String, String> resultMap = new HashMap<>();
            String filename = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            String suffix = urlString.substring(urlString.lastIndexOf("."), urlString.length());
            // 构造URL
            URL url = new URL(urlString);
            // 打开连接
            URLConnection con = url.openConnection();
            //设置请求超时为5s
            con.setConnectTimeout(5 * 1000);
            // 输入流
            InputStream is = con.getInputStream();

            // 1K的数据缓冲
            byte[] bs = new byte[1024];
            // 读取到的数据长度
            int len;
            // 输出的文件流
            File sf = new File(savePath);
            if (!sf.exists()) {
                sf.mkdirs();
            }
            OutputStream os = new FileOutputStream(sf.getPath() + "\\" + filename + suffix);
            // 开始读取
            while ((len = is.read(bs)) != -1) {
                os.write(bs, 0, len);
            }
            // 完毕，关闭所有链接
            os.close();
            is.close();
            resultMap.put("suffix", suffix);
            resultMap.put("filename", filename);
            resultMap.put("localPath", savePath + filename + suffix);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        //String url = download("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544438917172&di=038ee0f48a111c4270c779103d497642&imgtype=0&src=http%3A%2F%2Fpic1.win4000.com%2Fwallpaper%2F8%2F5121d1c073778.jpg", "1.jpg","d:\\image\\");
        //System.out.println(url);
        String a = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1544438917172&di=038ee0f48a111c4270c779103d497642&imgtype=0&src=http%3A%2F%2Fpic1.win4000.com%2Fwallpaper%2F8%2F5121d1c073778.jpg";
        String b = a.substring(a.lastIndexOf("."), a.length());
        System.out.println(b);
        String string = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        System.out.println(string);
    }


}
