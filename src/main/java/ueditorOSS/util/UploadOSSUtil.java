package ueditorOSS.util;

import com.aliyun.oss.OSSClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class UploadOSSUtil {
	private static Logger logger = LogManager.getLogger(UploadOSSUtil.class);
	
	public UploadOSSUtil() {
	}

	public static void uploadImgAliyun(File file, String fileName) throws FileNotFoundException {
		String endpoint = ApiUrl.endpoint;
		String accessId = ApiUrl.accessId;
		String accessKey = ApiUrl.accessKey;
		String bucket = ApiUrl.bucket;
		String dir = ApiUrl.myPath;
		OSSClient client = new OSSClient("http://"+endpoint, accessId, accessKey);
		client.putObject(bucket, dir + fileName, file);
		client.shutdown();
	}
	
	public static void createTempFile(String path, InputStream inputStream){
		try {
			DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(path));  
            int byteCount = 0;  
            //1M逐个读取  
            byte[] bytes = new byte[1024*1024];  
            while ((byteCount = inputStream.read(bytes)) != -1){  
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
	
	public static void removeTempFile(String path){
		File file = new File(path);
		file.delete();
	}
	
}
