package ueditorOSS.util;

import ueditorOSS.exception.RequestToMapException;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class StringUtil {

	public static String setString(String str) {
		return setString(str, "");
	}

	public static String setString(String str, String defaultStr) {
		if (isNotBlank(str)) {
			return str;
		} else {
			return defaultStr;
		}
	}

	public static boolean isBlank(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(str.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}
	
	public static boolean isNull(String str){
		if(str == null)
			return true;
		return false;
	}
	
	public static boolean isNotNull(String str){
		return !isNull(str);
	}
	
	

	private static final int formatterLineWidth = 32;
	private static final byte[] spaceBytes128 = new byte[]{
			32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,
			32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,
			32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,
			32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,};
	public static String getLogFormatedBytes(byte[] in){
		try {
			return getLogFormatedBytes(in, Charset.defaultCharset().name());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * 获取“日志格式”的字节数组内容
	 * @param in
	 * @param encoding
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String getLogFormatedBytes(byte[] in,String encoding) throws UnsupportedEncodingException{
		StringBuilder sb = new StringBuilder();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int offset = 0;
		for(byte b:in){
			sb.append(String.format("%02X ", b));
			baos.write(b<32&&b>0?'?':b);
			offset++;
			if(offset%(formatterLineWidth/2)==0){
				sb.append("  ");
			}
			if(offset%formatterLineWidth==0){
				sb.append(baos.toString(encoding));
				sb.append('\n');
				baos.reset();
			}
		}
		if(baos.size()>0){
			int len = formatterLineWidth - baos.size();
			len*=3;
			len+=2;
			if(len>formatterLineWidth/2*3){
				len+=2;
			}
			sb.append(getSpace(len));
			sb.append(new String(baos.toString(encoding)));
			sb.append('\n');
			baos.reset();
		}
		return sb.toString();
	}
	
	/**
	 * 获取指定长度的空字符串
	 * @param len
	 * @return
	 */
	public static String getSpace(int len){
		StringBuilder sb = new StringBuilder();
		while(len>0){
			sb.append(new String(spaceBytes128,0,len>spaceBytes128.length?spaceBytes128.length:len));
			len-=spaceBytes128.length;
		}
		return sb.toString();
	}
	
	/**
	 * <p>从输入流中按编码读取字符串</p>
	 * <p style="color:red"><b><i>注意，此方法不关闭输入流！</i></b></p>
	 * @param is
	 * @param encoding
	 * @param limitSize 读取的字节数，小于0无限读取，大于等于0时读取指定字节数
	 * @return
	 * @throws IOException
	 */
	public static String fromInputStream(InputStream is,String encoding,int limitSize) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyStream(is, baos, limitSize);
		return baos.toString(encoding);
	}
	
	/**
	 * <p>将输入流内容复制入输出流</p>
	 * <p style="color:red"><b><i>注意，此方法不关闭输入、输出流！</i></b></p>
	 * @param is 输入流
	 * @param os 输出流
	 * @param limitSize 复制的字节数，小于0无限复制，大于等于0时复制指定字节数
	 * @throws IOException 发生错误时抛出此异常
	 */
	public static void copyStream(InputStream is,OutputStream os,int limitSize) throws IOException{
		int available = 0;
		byte[] buffer = new byte[4096];
		int processedSize = 0;
		while(true){
			int needRead = limitSize>=0?(limitSize-processedSize>buffer.length?buffer.length:limitSize-processedSize):buffer.length;
			available = is.read(buffer,0,needRead);
			if(available==-1){
				break;
			}
			os.write(buffer, 0, available);
			if(limitSize>0&&(processedSize+=available)>=limitSize){
				break;
			}
		}
	}
	
	/**
	 * <p>从输入流中按编码读取字节</p>
	 * <p style="color:red"><b><i>注意，此方法不关闭输入流！</i></b></p>
	 * @param is
	 * @param limitSize
	 * @return
	 * @throws IOException
	 */
	public static byte[] fromInputStream(InputStream is,int limitSize) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyStream(is, baos, limitSize);
		return baos.toByteArray();
	}
	
	/**
	 * 判断字符串是否只包含空格或为null
	 * @param in 源字串
	 * @return 判断结果
	 */
	public static boolean isOnlySpacesOrNull(String in){
		return in==null||in.trim().length()==0;
	}
	
	public static Map<String, Object> resquestParameter2Map(HttpServletRequest request) throws RequestToMapException {
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, String[]> url = request.getParameterMap();
		for (Entry<String, String[]> entry : url.entrySet()) {
			String key = entry.getKey();
			String[] valueArray = entry.getValue();
			String value;
			value = new String(valueArray[0]);
			map.put(key, value);
		}
		return map;
	}
	
}
