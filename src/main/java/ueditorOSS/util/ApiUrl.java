package ueditorOSS.util;

import ueditorOSS.listener.Listenner;

public class ApiUrl {

	public static String endpoint = Listenner.getJndiString("ueditorOSS.endpoint");
	public static String accessId = Listenner.getJndiString("ueditorOSS.accessId");
	public static String accessKey = Listenner.getJndiString("ueditorOSS.accessKey");
	public static String bucket = Listenner.getJndiString("ueditorOSS.bucket");
	public static String myPath = Listenner.getJndiString("ueditorOSS.myPath");//如果有值一定要加【/】
	
}
