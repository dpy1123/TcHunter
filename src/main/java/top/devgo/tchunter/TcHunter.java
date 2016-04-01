package top.devgo.tchunter;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;

import top.devgo.tchunter.api.BaiduMusicApi;
import top.devgo.tchunter.api.DongtingApi;
import top.devgo.tchunter.api.KuwoApi;
import top.devgo.tchunter.api.Music163Api;
import top.devgo.tchunter.api.QQMusicApi;
import top.devgo.tchunter.util.IOUtil;
import top.devgo.tchunter.util.StringUtil;

/**
 * hunt任务主体逻辑
 * @author dd
 *
 */
public class TcHunter implements Runnable {
	
	private CloseableHttpClient httpClient;
	private ObjectMapper mapper;
	private PriorityBlockingQueue<Map<String, Object>> badResult;
	private String mp3file;

	
	public TcHunter(CloseableHttpClient httpClient, ObjectMapper mapper, PriorityBlockingQueue<Map<String, Object>> badResult, String mp3file) {
		super();
		this.httpClient = httpClient;
		this.mapper = mapper;
		this.badResult = badResult;
		this.mp3file = mp3file;
	}

	public void run() {
		try {
			tcHunt(mp3file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取图词
	 * @param mp3file
	 * @throws Exception
	 */
	public void tcHunt(String mp3file) throws Exception {
		String extension = mp3file.substring(mp3file.lastIndexOf(".")+1);
		if(!"mp3".equals(extension.toLowerCase())){
			throw new IllegalArgumentException("暂时只接受mp3文件!");
		}
		
		Music163Api music163 = new Music163Api(httpClient, mapper);
		//get mp3info 
		Map<String, Object> mp3Info = Mp3Helper.getMp3Info(mp3file);
		System.out.println("mp3Info: "+mp3Info);
		//search
		List<Map<String, Object>> searchResult = music163.searchMusic((String)mp3Info.get("title"), (String)mp3Info.get("artist"), (String)mp3Info.get("album"));
		if(searchResult.size() < 1) return;
		//get bestfit
		Map<String, Object> best = getBestFit(searchResult, mp3Info);
//		for (int i = 0; i < searchResult.size(); i++) {
//			System.out.println(searchResult.get(i));
//		}
		System.out.println("bestFit: " + best);
		//rank不足20分，则记录到badResult，并返回
		if((Double)best.get("rank") < 20){
			Map<String, Object> bad = new HashMap<String, Object>();
			bad.put("mp3Info", mp3Info);
			bad.put("bestFit", best);
			badResult.put(bad);
			return;
		}
		
		//download lrc
		String path = mp3file.substring(0, mp3file.lastIndexOf("."));
		String lyric = music163.getLyric(String.valueOf(best.get("id")));
		if(StringUtil.isNotBlank(lyric) && !new File(path + ".lrc").exists()){
			IOUtil.Writer(path + ".lrc", IOUtil.UTF8, lyric);
//			System.out.println(lyric);
		}
		//update mp3info
		TcMp3File file = new TcMp3File(mp3file);
		Mp3Helper.updateMp3Info(file, best);
		//add album_pic
		String pic_url = (String) best.get("album_pic");//http://p3.music.126.net/FtUrdiJ_4xqA9r24cVPzpA==/730075720865799.jpg
		byte[] albumImageData = music163.downloadPic(pic_url);
		if (albumImageData != null) {
			Mp3Helper.updateAlbumImg(file, albumImageData, getMimeType(pic_url));
		}
		
	}

	public Map<String, Object> getBestFit(List<Map<String, Object>> searchResults, Map<String, Object> mp3Info){
    	Map<String, Object> bestFits = null;
    	//Rank
    	for (Map<String, Object> searchResult : searchResults) {
    		//title_rank = 20*相似度，相似度=1/编辑距离+1
    		String testStr = searchResult.get("title").toString().trim().toLowerCase();
    		String targetStr = mp3Info.get("title").toString().trim().toLowerCase();
    		double title_rank = 20 * 1.0 / (1 + StringUtil.editDistance(testStr, targetStr));
    		//duration_rank = 20*相似度
			int testInt = 0;
			if(searchResult.get("duration") instanceof Integer){
				testInt = (Integer) searchResult.get("duration");
			}else{
				testInt = ((Long) searchResult.get("duration")).intValue();
			}
			int targetInt = ((Long) mp3Info.get("duration")).intValue();
			double duration_rank = 20 * 1.0 / (1 + Math.abs(testInt-targetInt));//1/1+x
//			double duration_rank = 20 * (1 - 1.0*Math.abs(testInt-targetInt)/targetInt);
			
			double track_rank = 0, album_rank = 0, artist_rank = 0;
    		//track = 20*相似度
			if (mp3Info.containsKey("track") && searchResult.containsKey("track")) {
				testStr = searchResult.get("track").toString().trim();
				targetStr = mp3Info.get("track").toString().trim();
				track_rank = 20 * 1.0 / (1 + StringUtil.editDistance(testStr, targetStr));
			}
			//album = 20*相似度
			if (mp3Info.containsKey("album")) {
				testStr = searchResult.get("album").toString().trim();
				targetStr = mp3Info.get("album").toString().trim();
				album_rank = 20 * 1.0 / (1 + StringUtil.editDistance(testStr, targetStr));
			}
			//artist = 20*相似度
			if (mp3Info.containsKey("artist")) {
				testStr = searchResult.get("artist").toString().trim();
				targetStr = mp3Info.get("artist").toString().trim();
				artist_rank = 20 * 1.0 / (1 + StringUtil.editDistance(testStr, targetStr));
			}
			searchResult.put("rank", title_rank + duration_rank + track_rank
					+ album_rank + artist_rank);
		}
    	//sort 倒序
    	searchResults.sort( new Comparator<Map<String,Object>>() {
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				double a = (Double) o1.get("rank");
				double b = (Double) o2.get("rank");
				return a > b ? -1:1;
			}
		});
    	//get top 1
    	bestFits = searchResults.get(0);
    	return bestFits;
    }
	
	
    /**
     * The MIME mappings for this web application, keyed by extension.
     */
	private static final HashMap<String, String> mimeMappings = new HashMap<String, String>();
    
    static{
    	mimeMappings.put("jpg", "image/jpeg");
    	mimeMappings.put("jpeg", "image/jpeg");
    	mimeMappings.put("png", "image/png");
    	mimeMappings.put("gif", "image/gif");
    }
    
	/**
	 * Return the MIME type of the specified file, or <code>null</code> if the
	 * MIME type cannot be determined.
	 *
	 * @param file
	 *            Filename for which to identify a MIME type
	 */
	public String getMimeType(String file) {
		if (file == null)
			return (null);
		int period = file.lastIndexOf(".");
		if (period < 0)
			return (null);
		String extension = file.substring(period + 1);
		if (extension.length() < 1)
			return (null);
		return (mimeMappings.get(extension));
	}
	
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		ObjectMapper mapper = new ObjectMapper();
		String mp3file = "D:\\test\\Fallen.mp3";
		Music163Api music163 = new Music163Api(httpClient, mapper);
		BaiduMusicApi baiduMusic = new BaiduMusicApi(httpClient, mapper);
		DongtingApi dongting = new DongtingApi(httpClient, mapper);
		QQMusicApi qqMusic = new QQMusicApi(httpClient, mapper);
		KuwoApi kuwo = new KuwoApi(httpClient, mapper);
		//get mp3info 
		Map<String, Object> mp3Info = Mp3Helper.getMp3Info(mp3file);
		System.out.println("mp3Info: "+mp3Info);
		//search
		List<Map<String, Object>> searchResult = music163.searchMusic((String)mp3Info.get("title"), (String)mp3Info.get("artist"), (String)mp3Info.get("album"));
		searchResult.addAll(baiduMusic.search((String)mp3Info.get("title")));
		searchResult.addAll(dongting.searchMusic((String)mp3Info.get("title"), (String)mp3Info.get("artist"), (String)mp3Info.get("album")));
		searchResult.addAll(qqMusic.searchMusic((String)mp3Info.get("title"), (String)mp3Info.get("artist"), (String)mp3Info.get("album")));
		searchResult.addAll(kuwo.searchMusic((String)mp3Info.get("title"), (String)mp3Info.get("artist"), (String)mp3Info.get("album")));
		if(searchResult.size() < 1) return;
		for (int i = 0; i < searchResult.size(); i++) {
			System.out.println(searchResult.get(i));
		}
		//get bestfit
		try {
			
			Map<String, Object> best = new TcHunter(httpClient, mapper,null,mp3file).getBestFit(searchResult, mp3Info);
			System.out.println("bestFit: " + best);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
