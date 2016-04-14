package top.devgo.tchunter;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
	private static Logger logger = Logger.getLogger(App.class.getName());
	private PoolingHttpClientConnectionManager connectionManager;
	/**
	 * 设置PoolingHttpClientConnectionManager的connection re-validate time，默认50ms
	 */
	private int connectionValidateInterval = 50;
	/**
	 * 设置PoolingHttpClientConnectionManager的connection数量，默认100个
	 */
	private int maxConnections = 10;
	/**
	 * tcHunterThreadPool线程池
	 */
	private ExecutorService tcHunterThreadPool; 
	private static int threadPoolNumber = 20;
	
	private CloseableHttpClient httpclient;
	private ObjectMapper mapper;
	/**
	 * 保存无图、无词的mp3名
	 */
	private Map<String, Vector<String>> badResult;
	
	public App() {
		// Create an HttpClient with the ThreadSafeClientConnManager.
		// This connection manager must be used if more than one thread will
		// be using the HttpClient.
		connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setValidateAfterInactivity(connectionValidateInterval);
		connectionManager.setMaxTotal(maxConnections);
		RequestConfig config = RequestConfig.custom()
				  .setSocketTimeout(5000)
				  .setConnectTimeout(5000)
				  .setConnectionRequestTimeout(5000)
				  .build();
		httpclient = HttpClients.custom()
//				.setDefaultRequestConfig(config)
				.setConnectionManager(connectionManager).build();
		
		mapper = new ObjectMapper();
		mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true) ;  
		mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
		
		badResult = new ConcurrentHashMap<String, Vector<String>>();
		badResult.put("badSearchList", new Vector<String>());
		badResult.put("errorList", new Vector<String>());
		badResult.put("noPicList", new Vector<String>());
		badResult.put("noLrcList", new Vector<String>());
		
		tcHunterThreadPool = Executors.newFixedThreadPool(threadPoolNumber);
	}
	

	public static void main( String[] args ) throws IOException, InterruptedException {
        App app = new App();
        
        String path = "D:\\test\\WHITE ALBUM 2 OST";
//        String path = args[0];
        long begin = System.currentTimeMillis();
        int count = app.tcHuntAll(path);
        app.tcHunterThreadPool.shutdown(); 
        while (true) {  
            if (app.tcHunterThreadPool.isTerminated()) {  
                logger.info("Result: ");  
                break;  
            }  
            Thread.sleep(500);  
        }  
        app.httpclient.close();
		app.connectionManager.close();
        long duration = System.currentTimeMillis() - begin;
        logger.info("总耗时: "+duration/1000+" s");
        logger.info("扫描: "+count+" 首歌曲");
        logger.info("badSearchList : "+ app.badResult.get("badSearchList"));
        logger.info("errorList : "+ app.badResult.get("errorList"));
        logger.info("noPicList : "+ app.badResult.get("noPicList"));
        logger.info("noLrcList : "+ app.badResult.get("noLrcList"));
        
    }
	
	
	public void tcHunt(String mp3file) {
		tcHunterThreadPool.execute(new TcHunter(httpclient, mapper, badResult, mp3file));
	}


	public int tcHuntAll(String path) {
		int count = 0;
		File d = new File(path);
        if(d.isDirectory()){
        	File[] files = d.listFiles();
        	for (File file : files) {
        		String filename = file.getAbsolutePath();
        		if (file.isDirectory()) {
        			count += tcHuntAll(filename);
				}else{
					String extension = filename.substring(filename.lastIndexOf(".")+1);
					if ("mp3".equals(extension.toLowerCase())) {
						tcHunt(filename);
						count++;
					}
				}
			}
        }else{
        	String extension = path.substring(path.lastIndexOf(".")+1);
        	if ("mp3".equals(extension.toLowerCase())) {
    			tcHunt(path);
    			count++;
        	}
        }
		return count;
	}
	
}
