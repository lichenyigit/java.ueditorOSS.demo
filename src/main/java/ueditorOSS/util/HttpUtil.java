package ueditorOSS.util;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ueditorOSS.exception.BadHttpStatusException;
import ueditorOSS.exception.HttpRequestFailedException;

import java.io.*;
import java.net.*;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class HttpUtil {
	private static final Logger logger = LogManager.getLogger(HttpUtil.class);
	private static int defaultRequestTimeout = 30000;//30s
	private static int defaultConnectTimeout = 30000;//30s
	private static int defaultReadTimeout = 30000;//30s
	private static int defaultIdleCloseTimeout = 1200000;//120s
	private static int defaultMaxTotalPerRoute = 20;
	private static int defaultMaxTotal = 100;
	private static String defaultEncoding = "UTF-8";
	
	public static int getDefaultRequestTimeout() {
		return defaultRequestTimeout;
	}
	
	public static int getDefaultConnectTimeout() {
		return defaultConnectTimeout;
	}
	
	public static int getDefaultReadTimeout() {
		return defaultReadTimeout;
	}
	
	public static String getDefaultEncoding() {
		return defaultEncoding;
	}

	public static void setDefaultRequestTimeout(int defaultRequestTimeout) {
		HttpUtil.defaultRequestTimeout = defaultRequestTimeout;
	}

	public static void setDefaultConnectTimeout(int defaultConnectTimeout) {
		HttpUtil.defaultConnectTimeout = defaultConnectTimeout;
	}

	public static void setDefaultReadTimeout(int defaultReadTimeout) {
		HttpUtil.defaultReadTimeout = defaultReadTimeout;
	}

	public static void setDefaultEncoding(String defaultEncoding) {
		HttpUtil.defaultEncoding = defaultEncoding;
	}

	private static CloseableHttpAsyncClient client = null;
	
	public static class Parameter implements NameValuePair{
		public final String name;
		public final String value;
		public Parameter(String name,String value) {
			this.name = name;
			this.value = value;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public String getValue() {
			return value;
		}
	}
	
	private static class DummyFutureCallback implements FutureCallback<HttpResponse>{
		@Override
		public void completed(HttpResponse result) {}
		@Override
		public void failed(Exception ex) {}
		@Override
		public void cancelled() {}
	}
	
	public static interface HttpCallback extends FutureCallback<HttpResponse>{
		
	}
	
	public static synchronized void init() throws IOException{
		init(defaultConnectTimeout, defaultReadTimeout, defaultRequestTimeout, 2, defaultMaxTotal, defaultMaxTotalPerRoute);
	}
	
	public static synchronized void init(int connectTimeout,int readTimeout,int requestTimeout,int ioThreadCount,int maxTotal,int maxTotalPerRoute) throws IOException{
		if(client==null){
			IOReactorConfig defaultIoReactorConfig = IOReactorConfig.custom().setConnectTimeout(connectTimeout)
					.setSoTimeout(readTimeout).setIoThreadCount(ioThreadCount).setSoReuseAddress(true).build();
			ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(defaultIoReactorConfig);
			final PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);
			connManager.setMaxTotal(maxTotal);
			ExecutorUtil.scheduleJobAtFixRate(new ExecutorUtil.Job() {
				@Override
				public void run() {
					connManager.closeExpiredConnections();//Keep-alive超时
					connManager.closeIdleConnections(defaultIdleCloseTimeout, TimeUnit.MILLISECONDS);//连接超时
				}
				@Override
				public String getRefKey() {
					return "HttpConnManager-auto-release-repeat-job";
				}
			}, 1000, 1000);//定时任务，每秒检查一次过期的Http连接和连接超时的Http连接，进行释放
			connManager.setDefaultMaxPerRoute(maxTotalPerRoute);
			RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectionRequestTimeout(requestTimeout).setRedirectsEnabled(false).build();//不处理304转发
			client = HttpAsyncClients.custom()
					.setConnectionManager(connManager)
					.setDefaultRequestConfig(defaultRequestConfig)
					.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {//Keep-alive时间：5000ms
						@Override
						public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
							return 5000;
						}
					})
					.setConnectionReuseStrategy(new ConnectionReuseStrategy() {//HTTP连接重用机制，允许
						@Override
						public boolean keepAlive(HttpResponse response, HttpContext context) {
							return true;
						}
					})
					.build();
			client.start();
		}
	}
	
	public static synchronized void shutdown() throws IOException{
		if(client!=null){
			client.close();
			client = null;
		}
	}
	
	
	@Deprecated
	public static Future<HttpResponse> curlAsyncPost(String url,List<Parameter> parameters,String paramEncode,int timeout,FutureCallback<HttpResponse> callback) throws HttpRequestFailedException {
		HttpPost post = new HttpPost(url);
		RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(timeout).build();
		post.setConfig(config);
		post.setEntity(new UrlEncodedFormEntity(parameters, Charset.forName(paramEncode)));
		Future<HttpResponse> future = client.execute(post, callback);
		return future;
	}
	
	
	@Deprecated
	public static Future<HttpResponse> curlAsyncPostJson(String url,String json,String paramEncode,int timeout,FutureCallback<HttpResponse> callback) throws HttpRequestFailedException{
		HttpPost post = new HttpPost(url);
		RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(timeout).build();
		post.setConfig(config);
		post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		Future<HttpResponse> future = client.execute(post,callback);
		return future;
	}
	
	/**
	 * 将参数写入outstream中
	 * @param os 指定的输出流
	 * @param parameters 参数列表
	 * @throws IOException 发生异常时抛出此异常
	 */
	private static void _writeParameterOutputStream(OutputStream os,List<Parameter> parameters) throws IOException{
		if(parameters!=null&&parameters.size()>0){
			boolean first = true;
			for(Parameter parameter:parameters){
				try {
					if(parameter.name!=null&&parameter.value!=null){
						if(first){
							first = false;
						}else{
							os.write('&');
						}
						os.write(URLEncoder.encode(parameter.name, "UTF-8").getBytes());
						os.write('=');
						os.write(URLEncoder.encode(parameter.value, "UTF-8").getBytes());
					}
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
	}

	/**
	 * 处理http请求
	 * @param method http请求的方法
	 * @param url 对应的URL
	 * @param parameters 请求的参数
	 * @param timeout 超时时间
	 * @return http请求的返回原始值
	 * @throws BadHttpStatusException http返回状态不正确时抛出此异常(不为200)
	 * @throws HttpRequestFailedException 当请求失败时抛出此异常 
	 */
	@Deprecated
	public static String getRawData(String method,String url,List<Parameter> parameters) throws BadHttpStatusException, HttpRequestFailedException {
		return getRawData(method, url, parameters, 30000, -1);
	}

	
	public static byte[] postBytes(String url,List<Parameter> parameters,String entityEncode,int limitSize) throws HttpRequestFailedException{
		try {
			List<Parameter> entryList = getNoneNullList(parameters);
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(entryList, entityEncode);
			return _postBytes(url, entity, limitSize);
		} catch (UnsupportedEncodingException e) {
			throw new HttpRequestFailedException(e);
		}
	}
	
	/**
	 * 以get方式进行http请求
	 * @param url
	 * @param parameters
	 * @param timeout
	 * @return
	 * @throws HttpRequestFailedException 
	 */
	public static byte[] postBytes(String url,List<Parameter> parameters,int timeout) throws HttpRequestFailedException{
		return postBytes(url, parameters, defaultEncoding, -1);
	}
	
	public static byte[] postBytes(String url,List<Parameter> parameters) throws HttpRequestFailedException{
		return postBytes(url, parameters, -1);
	}
	
	public static byte[] postBytes(String url,byte[] data,ContentType contentType,int limitSize) throws HttpRequestFailedException {
		ByteArrayEntity entity;
		entity = new ByteArrayEntity(data,contentType);
		return _postBytes(url, entity, limitSize);
	}
	
	public static byte[] postBytes(String url,byte[] data,int limitSize) throws HttpRequestFailedException {
		return postBytes(url, data, ContentType.APPLICATION_JSON, limitSize);
	}
	
	public static byte[] postBytes(String url,byte[] data) throws HttpRequestFailedException {
		return postBytes(url, data, -1);
	}
	
	public static String postString(String url, List<Parameter> parameters,String entityEncode, int limitSize) throws HttpRequestFailedException {
		try {
			List<Parameter> entryList = getNoneNullList(parameters);
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(entryList, entityEncode);
			HttpEntity responseEntity = postEntity(url, entity, null);
			Header contentTypeHeader = responseEntity.getContentType();
			String contentEncoding = getCharsetFromContentTypeHeader(contentTypeHeader);
			InputStream is = responseEntity.getContent();
			return StringUtil.fromInputStream(is, contentEncoding, limitSize);
		} catch (IOException e) {
			throw new HttpRequestFailedException(e);
		}
	}
	
	public static String postString(String url,List<Parameter> parameters,String entityEncode) throws HttpRequestFailedException{
		return postString(url, parameters, entityEncode, -1);
	}
	
	public static String postString(String url,List<Parameter> parameters) throws HttpRequestFailedException{
		return postString(url, parameters, defaultEncoding);
	}
	
	public static String postString(String url, byte[] data, ContentType contentType) throws HttpRequestFailedException {
		return postString(url, data, contentType, -1);
	}
	
	public static String postString(String url, byte[] data, ContentType contentType, int limitSize) throws HttpRequestFailedException {
		ByteArrayEntity entity;
		entity = new ByteArrayEntity(data, contentType);
		HttpEntity responseEntity = postEntity(url, entity, null);
		try {
			String contentEncoding = getCharsetFromContentTypeHeader(responseEntity.getContentType());
			InputStream is = responseEntity.getContent();
			return StringUtil.fromInputStream(is, contentEncoding, limitSize);
		} catch (IOException e) {
			throw new HttpRequestFailedException(e);
		}
	}
	
	public static String postString(String url, byte[] data, int limitSize) throws HttpRequestFailedException {
		return postString(url, data, ContentType.APPLICATION_JSON , limitSize);
	}
	
	public static String postString(String url, byte[] data) throws HttpRequestFailedException {
		return postString(url, data, -1);
	}
	
	private static byte[] _postBytes(String url,HttpEntity entity,int limitSize) throws HttpRequestFailedException{
		try (InputStream is = postInputStream(url, entity, null)){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StringUtil.copyStream(is, baos, limitSize);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new HttpRequestFailedException(e);
		}
	}
	
	public static InputStream postInputStream(String url,HttpEntity entity,RequestConfig requestConfig) throws UnsupportedOperationException, IOException, HttpRequestFailedException{
		HttpEntity responseEntity = postEntity(url, entity, requestConfig);
		return responseEntity.getContent();
	}
	
	public static HttpEntity postEntity(String url,HttpEntity entity,RequestConfig requestConfig) throws HttpRequestFailedException{
		Future<HttpResponse> responseFuture =  asyncPostFuture(url, entity, new DummyFutureCallback(), requestConfig);
		HttpResponse response;
		try {
			response = responseFuture.get();
		} catch (InterruptedException e) {
			throw new HttpRequestFailedException(e);
		} catch (ExecutionException e) {
			throw new HttpRequestFailedException(e);
		}
		int status = response.getStatusLine().getStatusCode();
		if(status==HttpStatus.SC_OK){
			return response.getEntity();
		}else{
			HttpEntity responseEntity = response.getEntity();
			String encoding = getCharsetFromContentTypeHeader(responseEntity.getContentType());
			try {
				throw new BadHttpStatusException(status, StringUtil.fromInputStream(responseEntity.getContent(), encoding, -1));
			} catch (UnsupportedOperationException | IOException e) {
				throw new HttpRequestFailedException(e);
			}
		}
	}
	
	/**
	 * 异步发送post请求
	 * <p>如果日志级别为Trace，则记录日志</p>
	 * @param url 请求URL
	 * @param entity 请求体
	 * @param callback 请求状态回调
	 * @param requestConfig 请求配置
	 * @return
	 */
	public static Future<HttpResponse> asyncPostFuture(String url, HttpEntity entity, FutureCallback<HttpResponse> callback, RequestConfig requestConfig){
		try {
			init();
		} catch (IOException e) {
			e.printStackTrace();
		}
		HttpPost post = new HttpPost(url);
		post.setEntity(entity);
		if(requestConfig!=null){
			post.setConfig(requestConfig);
		}
		if(logger.isTraceEnabled()){
			if(entity.isRepeatable()){
				try(InputStream is = entity.getContent()){
					logger.trace("post data to url["+url+"],entityData:\n"+StringUtil.getLogFormatedBytes(StringUtil.fromInputStream(is, -1)));						
				}catch(IOException e){
					logger.error(e.getMessage(), e);
				}
			}else{
				logger.trace("post data to url["+url+"],but post entity is not repeatable,can not be logged.");
			}
		}
		return client.execute(post, callback);
	}
	
	public static Future<HttpResponse> asyncPostFuture(String url, byte[] data, HttpCallback callback){
		ByteArrayEntity byteArrayEntity = new ByteArrayEntity(data, ContentType.APPLICATION_JSON);
		return asyncPostFuture(url, byteArrayEntity, callback, null);
	}
	
	public static Future<HttpResponse> asyncPostFuture(String url, List<Parameter> parameters, HttpCallback callback) throws UnsupportedEncodingException{
		UrlEncodedFormEntity formEntity;
		try {
			formEntity = new UrlEncodedFormEntity(parameters, defaultEncoding);
		} catch (UnsupportedEncodingException e) {
			throw e;
		}
		return asyncPostFuture(url, formEntity, callback, null);
	}
	
	public static String getString(String url,List<Parameter> parameters) throws HttpRequestFailedException {
		return getString(url, parameters, defaultEncoding);
	}
	
	public static String getString(String url,List<Parameter> parameters,String charset) throws HttpRequestFailedException {
		return getString(url, parameters, charset, -1);
	}
	
	public static String getString(String url, List<Parameter> parameters, String charset, int limitSize) throws HttpRequestFailedException {
		HttpEntity entity = getEntity(url, parameters, charset, null);
		Header contentTypeHeader = entity.getContentType();
		String encoding = getCharsetFromContentTypeHeader(contentTypeHeader);
		try(InputStream is = entity.getContent()){
			return StringUtil.fromInputStream(is, encoding, limitSize);
		} catch (UnsupportedOperationException e) {
			throw new HttpRequestFailedException(e);
		} catch (IOException e) {
			throw new HttpRequestFailedException(e);
		}
	}
	
	public static byte[] getBytes(String url,List<Parameter> parameters) throws HttpRequestFailedException {
		return getBytes(url, parameters, defaultEncoding);
	}
	
	public static byte[] getBytes(String url, List<Parameter> parameters, String charset) throws HttpRequestFailedException {
		return getBytes(url, parameters, charset, -1);
	}
	
	public static byte[] getBytes(String url, List<Parameter> parameters, String charset, int limitSize) throws HttpRequestFailedException {
		return _getBytes(url, parameters, charset, limitSize);
	}
	
	
	private static byte[] _getBytes(String url,List<Parameter> parameters,String charset,int limitSize) throws HttpRequestFailedException{
		try(InputStream is = _getInputStream(url, parameters, charset)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StringUtil.copyStream(is, baos, limitSize);
			return baos.toByteArray();			
		} catch (IOException e) {
			throw new HttpRequestFailedException(e);
		}
	}
	
	private static InputStream _getInputStream(String url,List<Parameter> parameters,String charset) throws HttpRequestFailedException, UnsupportedOperationException, IOException{
		HttpEntity returnEntity = getEntity(url, parameters, charset, null);
		return returnEntity.getContent();
	}
	
	/**
	 * 异步发送Get请求
	 * <p>若日志级别为trace，则记录日志</p>
	 * @param url 请求URL
	 * @param parameters 请求参数
	 * @param charset 请求字符集
	 * @param callback 请求状态回调
	 * @param requestConfig 请求配置
	 * @return
	 * @throws HttpRequestFailedException
	 */
	public static Future<HttpResponse> asyncGetFuture(String url, List<Parameter> parameters, String charset, FutureCallback<HttpResponse> callback, RequestConfig requestConfig) throws HttpRequestFailedException{
		try {
			init();
		} catch (IOException e) {
			e.printStackTrace();
		}
		HttpGet get;
		try {
			URIBuilder builder = new URIBuilder(url);
			List<Parameter> entryList = getNoneNullList(parameters);
			if(entryList.size()>0){
				builder.addParameters(new Vector<NameValuePair>(entryList));
				builder.setCharset(Charset.forName(charset));
			}
			get = new HttpGet(builder.build());
		} catch (URISyntaxException e) {
			throw new HttpRequestFailedException(e);
		}
		if(requestConfig!=null){
			get.setConfig(requestConfig);
		}
		if(logger.isTraceEnabled()){
			logger.trace("get data from url["+get.getURI().toString()+"]");
		}
		return client.execute(get, callback);
	}
	
	public static Future<HttpResponse> asyncGetFuture(String url, List<Parameter> parameters, HttpCallback callback) throws HttpRequestFailedException{
		return asyncGetFuture(url, parameters, defaultEncoding, callback, null);
	}
	
	/**
	 * 通过Get方法发送Http请求并获取结果
	 * @param url 请求地址
	 * @param parameters 请求参数
	 * @param charset 参数编码
	 * @param requestConfig 请求配置
	 * @return 请求结果
	 * @throws HttpRequestFailedException 请求失败时抛出此异常
	 * @throws BadHttpStatusException 请求状态码不为200（SC_OK）时抛出此异常
	 */
	public static HttpEntity getEntity(String url, List<Parameter> parameters, String charset, RequestConfig requestConfig) throws HttpRequestFailedException{
		Future<HttpResponse> futureResponse = asyncGetFuture(url, parameters, charset, new DummyFutureCallback(), requestConfig);
		HttpResponse response;
		try {
			response = futureResponse.get();
		} catch (InterruptedException e) {
			throw new HttpRequestFailedException(e);
		} catch (ExecutionException e) {
			throw new HttpRequestFailedException(e);
		}
		int status = response.getStatusLine().getStatusCode();
		if(status==HttpStatus.SC_OK){
			return response.getEntity();
		}else{
			HttpEntity responseEntity = response.getEntity();
			Header header = responseEntity.getContentType();
			String encoding = getCharsetFromContentTypeHeader(header);
			try {
				throw new BadHttpStatusException(status, StringUtil.fromInputStream(responseEntity.getContent(), encoding, -1));
			} catch (UnsupportedOperationException | IOException e) {
				throw new HttpRequestFailedException(e);
			}
		}
	}
	
	@Deprecated
	public static String getRawData(String method,String url,List<Parameter> parameters,int timeout) throws BadHttpStatusException, HttpRequestFailedException {
		return getRawData(method, url, parameters, timeout, -1);
	}
	
	/**
	 * 处理http请求
	 * @param method http请求的方法
	 * @param url 对应的URL
	 * @param parameters 请求的参数
	 * @param timeout 超时时间
	 * @param resultLimitSize 结果最大字节数
	 * @return http请求的返回原始值
	 * @throws BadHttpStatusException http返回状态不正确时抛出此异常(不为200)
	 * @throws HttpRequestFailedException 当请求失败时抛出此异常
	 */
	@Deprecated
	public static String getRawData(String method,String url,List<Parameter> parameters,int timeout,int resultLimitSize) throws BadHttpStatusException, HttpRequestFailedException {
		URL urlUrl = null;
		method = method.toUpperCase();
		//如果是GET方法，把所有参数拼接到URL里面
		if("GET".equals(method)){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				_writeParameterOutputStream(baos, parameters);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(baos.size()>0){
				String extParamStr = new String(baos.toByteArray());
				if(url.contains("?")){
					url+="&";
				}else{
					url+="?";
				}
				url+=extParamStr;
			}
		}
		try {
			urlUrl = new URL(url);
		} catch (MalformedURLException e) {
			logger.error("malformed url:"+url,e);
		}
		HttpURLConnection conn = null;
		try{
			conn = (HttpURLConnection) urlUrl.openConnection();
			try {
				conn.setRequestMethod(method);
			} catch (ProtocolException e) {
				return null;
			}
			conn.setUseCaches(false);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			if("POST".equals(method)){
				if(parameters!=null&&parameters.size()>0){
					conn.setDoOutput(true);
					conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				}
				conn.connect();//POST需要先打开连接才能发送内容
				if(parameters!=null&&parameters.size()>0){
					OutputStream os = conn.getOutputStream();
					_writeParameterOutputStream(os, parameters);
					os.flush();
				}
			}
			int status = conn.getResponseCode();
			if(status!=200){
				InputStream is = conn.getErrorStream();
				throw new BadHttpStatusException(status, StringUtil.fromInputStream(is, getCharsetFromContentType(conn.getContentType()), -1));
			}
			InputStream is = conn.getInputStream();
			String retStr = StringUtil.fromInputStream(is, "UTF-8", -1);
			return retStr;
		}catch(IOException e){
			throw new HttpRequestFailedException(e);
		}finally{
			if(conn!=null){
				conn.disconnect();
			}
		}
	}
	
	private static String getCharsetFromContentTypeHeader(Header contentTypeHeader){
		String contentType = null;
		if(contentTypeHeader!=null){
			contentType = contentTypeHeader.getValue();
		}
		return getCharsetFromContentType(contentType);
	}
	private static String getCharsetFromContentType(String contentType){
		return getCharsetFromContentType(contentType, "ISO8859-1");
	}
	private static String getCharsetFromContentType(String strContentType,String defVal){
		String result = defVal;
		try{
			ContentType contentType = ContentType.parse(strContentType);
			result = contentType.getCharset().name();
		}catch(Exception e){}
		return result;
	}
	

//	private static class CloseableHttpEntityWrapper implements HttpEntity,AutoCloseable{
//		private final HttpEntity entity; 
//		private final CloseableHttpClient client;
//		public CloseableHttpEntityWrapper(HttpEntity entity,CloseableHttpClient client) {
//			this.entity = entity;
//			this.client = client;
//		}
//		public boolean isRepeatable() {
//			return entity.isRepeatable();
//		}
//		public boolean isChunked() {
//			return entity.isChunked();
//		}
//		public long getContentLength() {
//			return entity.getContentLength();
//		}
//		public Header getContentType() {
//			return entity.getContentType();
//		}
//		public Header getContentEncoding() {
//			return entity.getContentEncoding();
//		}
//		public InputStream getContent() throws IOException, UnsupportedOperationException {
//			return entity.getContent();
//		}
//		public void writeTo(OutputStream outstream) throws IOException {
//			entity.writeTo(outstream);
//		}
//		public boolean isStreaming() {
//			return entity.isStreaming();
//		}
//		@SuppressWarnings("deprecation")
//		@Deprecated
//		public void consumeContent() throws IOException {
//			entity.consumeContent();
//		}
//		@Override
//		public void close() throws IOException {
//			client.close();
//		}
//	}
	
	private static List<Parameter> getNoneNullList(List<Parameter> parameters){
		if(parameters==null){
			return new Vector<>(0);
		}
		List<Parameter> entryList = new Vector<>(parameters.size());
		for(Parameter parameter:parameters){
			if(parameter.value==null){
				continue;
			}
			entryList.add(parameter);
		}
		return entryList;
	}
	
//	private static class HttpInputStreamWrapper extends InputStream{
//		private final InputStream is;
//		private final CloseableHttpEntityWrapper httpEntity;
//		public HttpInputStreamWrapper(CloseableHttpEntityWrapper httpEntity) throws UnsupportedOperationException, IOException {
//			this.is = httpEntity.getContent();
//			this.httpEntity = httpEntity;
//		}
//		public int read() throws IOException {
//			return is.read();
//		}
//		public int hashCode() {
//			return is.hashCode();
//		}
//		public int read(byte[] b) throws IOException {
//			return is.read(b);
//		}
//		public boolean equals(Object obj) {
//			return is.equals(obj);
//		}
//		public int read(byte[] b, int off, int len) throws IOException {
//			return is.read(b, off, len);
//		}
//		public long skip(long n) throws IOException {
//			return is.skip(n);
//		}
//		public String toString() {
//			return is.toString();
//		}
//		public int available() throws IOException {
//			return is.available();
//		}
//		public void close() throws IOException {
//			try{
//				is.close();
//			}finally{
//				httpEntity.close();
//			}
//		}
//		public void mark(int readlimit) {
//			is.mark(readlimit);
//		}
//		public void reset() throws IOException {
//			is.reset();
//		}
//		public boolean markSupported() {
//			return is.markSupported();
//		}
//	}
}
