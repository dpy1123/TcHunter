package top.devgo.tchunter;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import top.devgo.tchunter.api.BaiduMusicApi;
import top.devgo.tchunter.api.DongtingApi;
import top.devgo.tchunter.api.KuwoApi;
import top.devgo.tchunter.api.Music163Api;
import top.devgo.tchunter.api.QQMusicApi;
import top.devgo.tchunter.file.FlacOper;
import top.devgo.tchunter.file.Mp3Oper;
import top.devgo.tchunter.file.MusicFileOper;
import top.devgo.tchunter.util.IOUtil;
import top.devgo.tchunter.util.StringUtil;

/**
 * hunt任务主体逻辑
 * @author dd
 *
 */
public class TcHunter implements Runnable {
	private static Logger logger = Logger.getLogger(TcHunter.class.getName());
	
	private Map<String, Vector<String>> badResult;
	private String filePath;
	private MusicFileOper musicFileOper;

	private Music163Api music163;
	private BaiduMusicApi baiduMusic;
	private DongtingApi dongting;
	private QQMusicApi qqMusic;
	private KuwoApi kuwo;
	
	public TcHunter(CloseableHttpClient httpClient, ObjectMapper mapper, Map<String, Vector<String>> badResult, String filePath) {
		super();
		this.badResult = badResult;
		this.filePath = filePath;
		
		
		String extension = filePath.substring(filePath.lastIndexOf('.')+1);
    	if ("mp3".equalsIgnoreCase(extension)) {
    		this.musicFileOper = new Mp3Oper();
    	}else if ("flac".equalsIgnoreCase(extension)) {
    		this.musicFileOper = new FlacOper();
    	}
		
		music163 = new Music163Api(httpClient, mapper);
		baiduMusic = new BaiduMusicApi(httpClient, mapper);
		dongting = new DongtingApi(httpClient, mapper);
		qqMusic = new QQMusicApi(httpClient, mapper);
		kuwo = new KuwoApi(httpClient, mapper);
	}
	
	private Map<String, Object> musicInfo;
	private List<Map<String, Object>> searchResult_music163;
	private List<Map<String, Object>> searchResult_baiduMusic;
	private List<Map<String, Object>> searchResult_dongting;
	private List<Map<String, Object>> searchResult_qqMusic;
	private List<Map<String, Object>> searchResult_kuwo;
	
	private void init() {
		//get musicInfo 
		try {
        	musicInfo = musicFileOper.getInfo(filePath);
		} catch (Exception e) {
			logger.error("获取"+filePath+"信息失败", e);
			return;
		}
		logger.info("musicInfo: " + musicInfo);
		//search
		try {
			searchResult_music163 = music163.searchMusic((String)musicInfo.get("title"), (String)musicInfo.get("artist"), (String)musicInfo.get("album"));
			searchResult_baiduMusic = baiduMusic.searchMusic((String)musicInfo.get("title"), (String)musicInfo.get("artist"), (String)musicInfo.get("album"));
			searchResult_dongting = dongting.searchMusic((String)musicInfo.get("title"), (String)musicInfo.get("artist"), (String)musicInfo.get("album"));
			searchResult_qqMusic = qqMusic.searchMusic((String)musicInfo.get("title"), (String)musicInfo.get("artist"), (String)musicInfo.get("album"));
			searchResult_kuwo = kuwo.searchMusic((String)musicInfo.get("title"), (String)musicInfo.get("artist"), (String)musicInfo.get("album"));
		} catch (Exception e) {
			logger.error("search "+filePath+" 失败", e);
		}
	}

	/**
	 * 更新歌曲信息
	 * @throws Exception
	 */
	public void updateMusicInfo() throws Exception {
		List<Map<String, Object>> searchResult = new ArrayList<Map<String,Object>>();
		if (searchResult_music163 != null) {
			searchResult.addAll(searchResult_music163);
		}
		if (searchResult_baiduMusic != null) {
			searchResult.addAll(searchResult_baiduMusic);
		}
		if (searchResult_dongting != null) {
			searchResult.addAll(searchResult_dongting);
		}
		if (searchResult_qqMusic != null) {
			searchResult.addAll(searchResult_qqMusic);
		}
		if (searchResult_kuwo != null) {
			searchResult.addAll(searchResult_kuwo);
		}
			
		if(searchResult != null &&searchResult.size() > 0) {
			//get bestfit
			Map<String, Object> best = getBestFit(searchResult, musicInfo);
			logger.info("bestFit: " + best);
			//rank不足20分，则记录到badSearchList，并返回
			if((Double)best.get("rank") < 20){
				badResult.get("badSearchList").add((String) musicInfo.get("title"));
				return;
			}
			//update musicInfo
        	musicFileOper.updateInfo(filePath, best);
		}
	}
	
	/**
	 * 下载歌词，选择 网易163->百度云音乐 中最优的，天天动听补充
	 * @throws Exception
	 */
	public void downloadLrc() throws Exception {
		String path = filePath.substring(0, filePath.lastIndexOf("."));
		if (new File(path + ".lrc").exists()) {
			return;
		}
		//search music163 baiduMusic dongting
		List<Map<String, Object>> searchResult = new ArrayList<Map<String,Object>>(searchResult_music163);
		searchResult.addAll(searchResult_baiduMusic);
		if(searchResult != null && searchResult.size() > 0) {
			//get bestfit
			Map<String, Object> best = getBestFit(searchResult, musicInfo);
			logger.info("bestFit: " + best);
			//download lrc
			switch ((String)best.get("api")) {
			case Music163Api.API_ID:
				String lyric = music163.getLyric(String.valueOf(best.get("id")));
				if(StringUtil.isNotBlank(lyric)){
					IOUtil.Writer(path + ".lrc", IOUtil.UTF8, lyric);
					return;
				}
			case BaiduMusicApi.API_ID:
				lyric = baiduMusic.getLyric((String) best.get("lrc_url"));
				if(StringUtil.isNotBlank(lyric)){
					IOUtil.Writer(path + ".lrc", IOUtil.UTF8, lyric);
					return;
				}
			default:
				break;
			}
		}
		//search dongting
		//download lrc
		String lyric = dongting.getLyric((String)musicInfo.get("title"), (String)musicInfo.get("artist"), (String)musicInfo.get("id"));
		if(StringUtil.isNotBlank(lyric)){
			IOUtil.Writer(path + ".lrc", IOUtil.UTF8, lyric);
			return;
		}
		
		badResult.get("noLrcList").add((String) musicInfo.get("title"));
	}
	
	/**
	 * 更新图片，选择 网易163->qq音乐->酷我->百度云音乐 中最优的
	 * @throws Exception
	 */
	public void updateAlbumPic() throws Exception {
		if (musicInfo==null || musicInfo.containsKey("has_album_pic") && (boolean) musicInfo.get("has_album_pic")) {
			return;
		}
		
		List<Map<String, Object>> searchResult = new ArrayList<Map<String,Object>>();
		if (searchResult_music163 != null) {
			searchResult.addAll(searchResult_music163);
		}
		if (searchResult_baiduMusic != null) {
			searchResult.addAll(searchResult_baiduMusic);
		}
		if (searchResult_qqMusic != null) {
			searchResult.addAll(searchResult_qqMusic);
		}
		if (searchResult_kuwo != null) {
			searchResult.addAll(searchResult_kuwo);
		}
		
		if(searchResult != null && searchResult.size() > 0) {
			//get bestfit
			Map<String, Object> best = getBestFit(searchResult, musicInfo);
			logger.info("bestFit: " + best);
			//add album_pic
			byte[] albumImageData = null;
			String pic_url = null;
			switch ((String)best.get("api")) {
			case Music163Api.API_ID:
				pic_url = (String) best.get("album_pic");
				albumImageData = music163.downloadPic(pic_url);
				if (albumImageData != null) break;
			case QQMusicApi.API_ID:
				pic_url = (String) best.get("img_id");
				albumImageData = qqMusic.downloadPic(pic_url);
				if (albumImageData != null) break;
			case KuwoApi.API_ID:
				pic_url = (String) best.get("artist_pic240");
				albumImageData = kuwo.downloadPic(pic_url);
				if (albumImageData != null) break;
			case BaiduMusicApi.API_ID:
				pic_url = (String) best.get("album_pic_url");
				albumImageData = baiduMusic.downloadPic(pic_url);
				if (albumImageData != null) break;
			default:
				break;
			}
			if (albumImageData != null) {
				musicFileOper.updateAlbumImg(filePath, albumImageData, getMimeType(pic_url));
				return;
			}
		}
		
		badResult.get("noPicList").add((String) musicInfo.get("title"));
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
			if (mp3Info.containsKey("album") && searchResult.get("album")!=null) {
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
//				return a > b ? -1:1;//【注意】JDK7中的Collections.Sort方法实现TimSort中，如果两个值是相等的，那么compare方法需要返回0，否则 可能 会在排序时抛错
				if (a == b)
					return 0;
				else
					return a > b ? -1 : 1;
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
	

	public void run() {
		try {
			init();
			updateMusicInfo();
			downloadLrc();
			updateAlbumPic();
		} catch (Exception e) {
			logger.error("处理过程中发生错误", e);
			badResult.get("errorList").add((String) musicInfo.get("title"));
		}
	}
	
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true) ;  
		mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
		
		String mp3file = "D:\\test\\01. black bullet.flac";
		
		ConcurrentHashMap<String, Vector<String>> badResult = new ConcurrentHashMap<String, Vector<String>>();
		badResult.put("badSearchList", new Vector<String>());
		badResult.put("errorList", new Vector<String>());
		badResult.put("noPicList", new Vector<String>());
		badResult.put("noLrcList", new Vector<String>());
		
		TcHunter hunter = new TcHunter(httpClient, mapper, badResult, mp3file);
		hunter.run();
	}
}
