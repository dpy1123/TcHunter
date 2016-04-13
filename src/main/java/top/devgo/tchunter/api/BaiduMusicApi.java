package top.devgo.tchunter.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import top.devgo.tchunter.util.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaiduMusicApi {
	private static Logger logger = Logger.getLogger(BaiduMusicApi.class.getName());
	public static final String API_ID = "BaiduMusicApi";
	
	private CloseableHttpClient httpclient;
	private ObjectMapper mapper; 
	
	public BaiduMusicApi(CloseableHttpClient httpclient, ObjectMapper mapper) {
		this.httpclient = httpclient;
		this.mapper = mapper;
	}
	
	/**
	 * 
	 * @param keywords
	 * @return LinkedList<br> 
     * Map={api=BaiduMusicApi, duration=264000, album_pic_url=http://musicdata.baidu.com/data2/pic/43742380/43742380.jpg, lrc_url=http://musicdata.baidu.com/data2/lrc/65445557/65445557.lrc, artist=angela, album=ZERO, id=51974213, title=僕じゃない}
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public List<Map<String, Object>> searchMusic(String... keywords) throws ClientProtocolException, IOException{
		if(keywords.length < 1)
			throw new IllegalArgumentException("至少提供一个keywords") ;
		
		String keyword = "";
		for (int i = 0; i < keywords.length; i++) {
			if(StringUtil.isNotBlank(keywords[i]))
				keyword += keywords[i];
			if(i<keywords.length-1) keyword += " ";
		}
		logger.info("[百度音乐]keyword: "+keyword);
		
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://musicmini.baidu.com/app/search/searchList.php")
				.addParameter("qword", keyword)
				.setHeader("Host", "musicmini.baidu.com")
				.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36")
				.build();
		
		CloseableHttpResponse response = httpclient.execute(request);
		String htmlStr = null;
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String encoding = "utf-8";
				
				htmlStr = EntityUtils.toString(entity, encoding);
			}	
		} finally {
			if (response != null) {
				response.close();
			}
		}
		
		//parse result
		return parseSearchResult(htmlStr);
	}
	
	private List<Map<String, Object>> parseSearchResult(String htmlStr) {
    	Document doc = Jsoup.parse(htmlStr);
		//#sc-table > tbody > tr:nth-child(3) > th > i
		Elements lists = doc.select("#sc-table > tbody > tr");
		List<String> ids = new ArrayList<String>();
		for (Element element : lists) {
			Element title = element.select("th > i > input.sCheckBox").first();
			if(title != null){
				ids.add(title.attr("id"));
			}
		}
		
		logger.info("[百度音乐]search results count: "+ids.size());
		
		List<Map<String, Object>> songs = new LinkedList<Map<String, Object>>();
		for (String id : ids) {
			try {
				songs.add(getMusicInfo(id));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	return songs;
    }
	
	/**
	 * 
	 * @param songId
	 * @return {api=BaiduMusicApi, duration=264000, album_pic_url=http://musicdata.baidu.com/data2/pic/43742380/43742380.jpg, lrc_url=http://musicdata.baidu.com/data2/lrc/65445557/65445557.lrc, artist=angela, album=ZERO, id=51974213, title=僕じゃない}
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getMusicInfo(String songId) throws ClientProtocolException, IOException{
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://music.baidu.com/data/music/links")
				.addParameter("songIds", songId)
				.setHeader("Host", "music.baidu.com")
				.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36")
				.build();
		
		CloseableHttpResponse response = httpclient.execute(request);
		String jsonResult = null;
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Header contentType = entity.getContentType();
				String encoding = "utf-8";
				if(contentType != null){
					String type = contentType.getValue();
					if(type != null && type.startsWith("text/")){//Content-Type:text/html;charset=utf-8;
						encoding = type.substring(type.lastIndexOf("charset=") + "charset=".length());
						int pos = encoding.lastIndexOf(";");
						if(pos > -1){
							encoding = encoding.substring(0, pos);
						}
					}
				}
				
				jsonResult = EntityUtils.toString(entity, encoding);
				EntityUtils.consume(entity);
			}	
		} finally {
			if (response != null) {
				response.close();
			}
		}
		
		//parse result
		Map<String, Object> searchResult = mapper.readValue(jsonResult, new TypeReference<Map<String, Object>>() { } );
    	
		Map<String, Object> mp3 = new HashMap<String, Object>();
		Integer resultCode = (Integer) searchResult.get("errorCode");
		if(resultCode == 22000){
			List<Map<String, Object>> songs = (List<Map<String, Object>>) ((Map<String, Object>)searchResult.get("data")).get("songList");//对null值进行强制转换后的返回值为null
			if(songs == null || songs.size() < 1){
				return mp3;
			}
			Map<String, Object> song = songs.get(0);
			mp3.put("api", API_ID);
			mp3.put("title", song.get("songName"));
			mp3.put("id", song.get("songId"));
			mp3.put("artist", song.get("artistName"));
			mp3.put("album", song.get("albumName"));
			mp3.put("album_pic_url", song.get("songPicBig"));
			mp3.put("lrc_url", song.get("lrcLink"));
			mp3.put("duration", 1000 * (Integer) song.get("time"));
		}
    	return mp3;
	}
	
	public String getLyric(String url) throws IOException {
		String lrcStr = null;
		if(StringUtil.isBlank(url)) return lrcStr;
		url = StringUtil.normalizeUrl(url);
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri(url)
				.build();
		CloseableHttpResponse response = httpclient.execute(request);
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Header contentType = entity.getContentType();
				String encoding = "utf-8";
				if(contentType != null){
					String type = contentType.getValue();
					if(type != null && type.startsWith("text/")){//Content-Type:text/html;charset=utf-8;
						encoding = type.substring(type.lastIndexOf("charset=") + "charset=".length());
						int pos = encoding.lastIndexOf(";");
						if(pos > -1){
							encoding = encoding.substring(0, pos);
						}
					}
				}
				
				lrcStr = EntityUtils.toString(entity, encoding);
			}	
		} finally {
			if (response != null) {
				response.close();
			}
		}
		return lrcStr;
	}
	
	/**
	 * 百度的图片质量一般，没有图片的专辑也给个占位图
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public byte[] downloadPic(String url) throws IOException {
		byte[] content = null;
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri(url)
				.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36")
				.build();
		CloseableHttpResponse response = httpclient.execute(request);
		
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				content = EntityUtils.toByteArray(entity);
			}	
		} finally {
			response.close();
		}
		return content;
	}
	
	
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		System.out.println(new BaiduMusicApi(HttpClients.createDefault(), new ObjectMapper()).searchMusic("僕じゃない"));
		System.out.println(new BaiduMusicApi(HttpClients.createDefault(), new ObjectMapper()).getLyric("http://musicdata.baidu.com/data2/lrc/65445557/65445557.lrc"));
//		System.out.println(new BaiduMusicApi(HttpClients.createDefault(), new ObjectMapper()).downloadPic("http://musicdata.baidu.com/data2/pic/43742380/43742380.jpg"));
	}
}
