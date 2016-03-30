package top.devgo.tchunter;

import java.io.IOException;
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

import top.devgo.tchunter.util.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 调用天天动听的接口
 * @author dd
 *
 */
public class DongtingApi {
	/**
	 * 搜索最多返回条数，默认100
	 */
	public static int MAX_SEARCH_RESULT = 100;
	private CloseableHttpClient httpclient;
	private ObjectMapper mapper; 
	
	public DongtingApi(CloseableHttpClient httpclient, ObjectMapper mapper) {
		this.httpclient = httpclient;
		this.mapper = mapper;
	}
	
	/**
	 * 查询歌曲信息，结果集很差而且没有排序的，eg:僕じゃない，就找不到歌手是angela的
	 * @param keywords
	 * @return LinkedList<br> 
     * Map={duration=264546, artist=angela, album=ZERO, id=26235098, title=僕じゃない, track=2}
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public List<Map<String, Object>> search(String... keywords) throws ClientProtocolException, IOException{
		if(keywords.length < 1)
			throw new IllegalArgumentException("至少提供一个keywords") ;
		
		String keyword = "";
		for (int i = 0; i < keywords.length; i++) {
			if(StringUtil.isNotBlank(keywords[i]))
				keyword += keywords[i];
			if(i<keywords.length-1) keyword += " ";
		}
		System.out.println("keyword: "+keyword);
		
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://search.dongting.com/song/search/old")//?q=周杰伦&page=1&size=20
				.addParameter("q", keyword)
				.addParameter("page", "1")
				.addParameter("size", String.valueOf(MAX_SEARCH_RESULT))
				.setHeader("Host", "search.dongting.com")
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
			}	
		} finally {
			if (response != null) {
				response.close();
			}
		}
		
		//parse result
		Map<String, Object> searchResult = mapper.readValue(jsonResult, new TypeReference<Map<String, Object>>() { } );
		return parseSearchResult(searchResult);
	}

	/**
     * @param searchResult
     * @return LinkedList<br> 
     * Map={duration=264546, artist=angela, album=ZERO, id=26235098, title=僕じゃない, track=2}
     */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseSearchResult(Map<String, Object> searchResult) {
	List<Map<String, Object>> resultList = new LinkedList<Map<String, Object>>();
    	
		Integer resultCode = (Integer) searchResult.get("code");
		if(resultCode == 1){
			System.out.println("search results count: "+searchResult.get("rows"));
			List<Map<String, Object>> songs = (List<Map<String, Object>>) searchResult.get("data");//对null值进行强制转换后的返回值为null
			if(songs == null){
				return resultList;
			}
			for (Map<String, Object> song : songs) {//foreach也要对songs进行null判断，否则会报错
				Map<String, Object> mp3 = new HashMap<String, Object>();
				mp3.put("title", song.get("song_name"));
				mp3.put("id", song.get("song_id"));
				mp3.put("artist", song.get("singer_name"));
				mp3.put("album", song.get("album_name"));
				mp3.put("track", song.get("pick_count"));
				
				long duration = 0;
				List<Map<String, Object>> audition_list = (List<Map<String, Object>>) (
						song.get("audition_list") == null ? song.get("url_list") : song.get("audition_list"));
				if (audition_list!=null && audition_list.size()>0) {
					duration = timecode2Milli((String) audition_list.get(0).get("duration"));//04:26
				}
				mp3.put("duration", duration);
				
//				System.out.println(mp3);
				resultList.add(mp3);
			}
		}
    	return resultList;
	}
	
	/**
	 * 04:26 -> 266000
	 * @param duration
	 * @return
	 */
	private long timecode2Milli(String duration) {
		if (StringUtil.isBlank(duration)) {
			return 0;
		}
		String m = duration.split(":")[0];
		String s = duration.split(":")[1];
		return 1000*(Integer.parseInt(m)*60+Integer.parseInt(s));
	}
	
	/**
	 * 注意，这个接口不是严格的按照歌曲id去获取歌词的，而是根据名称和艺术家去查询歌词的。
	 * @param title 必须有
	 * @param artist 可以视为同歌名下的筛选条件
	 * @param id 同上视为筛选条件
	 * @return
	 * @throws IOException
	 */
	public String getLyric(String title, String artist, String id) throws IOException {
		String result = null;
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://lp.music.ttpod.com/lrc/down")
				.addParameter("title", title)
				.addParameter("artist", artist)
				.addParameter("song_id", id)
				.setHeader("Host", "lp.music.ttpod.com")
				.build();
		CloseableHttpResponse response = httpclient.execute(request);
		String lrcStr = null;
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
		Map<String, Object> lrcInfo = mapper.readValue(lrcStr, new TypeReference<Map<String, Object>>() { } );
		Integer result_code = (Integer) lrcInfo.get("code");
		if(result_code == 1){
			Map<String, Object> lrc = (Map<String, Object>) lrcInfo.get("data");
			if(lrc != null)
				result = (String) lrc.get("lrc");
		}
		return result;
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		System.out.println(new DongtingApi(HttpClients.createDefault(), new ObjectMapper()).search("僕じゃない"));
//		System.out.println(new DongtingApi(HttpClients.createDefault(), new ObjectMapper()).getLyric("僕じゃない", null, null));
		System.out.println(new DongtingApi(HttpClients.createDefault(), new ObjectMapper()).getLyric("僕じゃない", "angela", ""));
	}
}
