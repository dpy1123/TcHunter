package top.devgo.tchunter;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
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
	private PriorityBlockingQueue<Map<String, Object>> badResult;
	
	public App() {
		// Create an HttpClient with the ThreadSafeClientConnManager.
		// This connection manager must be used if more than one thread will
		// be using the HttpClient.
		connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setValidateAfterInactivity(connectionValidateInterval);
		connectionManager.setMaxTotal(maxConnections);
		httpclient = HttpClients.custom().setConnectionManager(connectionManager).build();
		
		mapper = new ObjectMapper();
		
		badResult = new PriorityBlockingQueue<Map<String, Object>>(100, new Comparator<Map<String, Object>>() {

			@SuppressWarnings("unchecked")
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				if(o1.containsKey("bestFit") && o2.containsKey("bestFit")){
					Map<String, Object> bad1 = (Map<String, Object>) o1.get("bestFit");
					Map<String, Object> bad2 = (Map<String, Object>)o2.get("bestFit");
					return (int) ((Double)bad1.get("rank") - (Double)bad2.get("rank"));
				}
				return 0;
			}
			
		});
		
		tcHunterThreadPool = Executors.newFixedThreadPool(threadPoolNumber);
	}
	

	public static void main( String[] args ) throws IOException, InterruptedException {
        App app = new App();
        
        String path = "D:\\test\\";
//        String path = args[0];
        long begin = System.currentTimeMillis();
        int count = app.tcHuntAll(path);
        app.tcHunterThreadPool.shutdown(); 
        while (true) {  
            if (app.tcHunterThreadPool.isTerminated()) {  
                System.out.println("Result: ");  
                break;  
            }  
            Thread.sleep(500);  
        }  
        app.httpclient.close();
		app.connectionManager.close();
        long duration = System.currentTimeMillis() - begin;
        System.out.println("总耗时: "+duration/1000+" s");
        System.out.println("扫描: "+count+" 首歌曲");
//        System.out.println("bad list: "+ app.badResult.size());
        
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
						try {
							tcHunt(filename);
							count++;
						} catch (Exception e) {
							e.printStackTrace();
							System.err.println("处理["+filename+"]报错: "+e.getMessage());
						}
					}
				}
			}
        }else{
        	String extension = path.substring(path.lastIndexOf(".")+1);
        	if ("mp3".equals(extension.toLowerCase())) {
        		try {
        			tcHunt(path);
        			count++;
        		} catch (Exception e) {
        			e.printStackTrace();
        			System.err.println("处理["+path+"]报错: "+e.getMessage());
        		}
        	}
        }
		return count;
	}
	
}
