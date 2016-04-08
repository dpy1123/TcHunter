package top.devgo.tchunter.api;

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
import org.apache.log4j.Logger;

import top.devgo.tchunter.util.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QQMusicApi {
	private static Logger logger = Logger.getLogger(QQMusicApi.class.getName());
	/**
	 * 搜索最多返回条数，默认100
	 */
	public static int MAX_SEARCH_RESULT = 100;
	
	private CloseableHttpClient httpclient;
	private ObjectMapper mapper; 
	
	public QQMusicApi(CloseableHttpClient httpclient, ObjectMapper mapper) {
		this.httpclient = httpclient;
		this.mapper = mapper;
	}
	
	/**
	 * 
	 * @param keywords
	 * @return LinkedList<br> 
     * Map={duration=264546, artist=angela, album=ZERO, id=26235098, title=僕じゃない}
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
		logger.info("[QQ音乐]keyword: "+keyword);
		
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://s.music.qq.com/fcgi-bin/music_search_new_platform")//platform=jqminiframe.json&needNewCode=0&p=1&catZhida=0&remoteplace=sizer.newclient.next_song&w=%E5%91%A8%E6%9D%B0%E4%BC%A6%20%E8%BF%94%E5%9B%9E
				.addParameter("w", keyword)
				.addParameter("t", "0")//page
				.addParameter("n", String.valueOf(MAX_SEARCH_RESULT))
				.addParameter("aggr", "1")
				.addParameter("cr", "1")
				.addParameter("loginUin", "0")
				.addParameter("format", "json")
				.addParameter("inCharset", "utf-8")
				.addParameter("outCharset", "utf-8")
				.addParameter("notice", "0")
				.addParameter("platform", "jqminiframe.json")
				.addParameter("needNewCode", "0")
				.addParameter("p", "1")
				.addParameter("catZhida", "0")
				.addParameter("remoteplace", "sizer.newclient.next_song")
				.setHeader("Host", "s.music.qq.com")
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
	
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseSearchResult(Map<String, Object> searchResult) {
    	List<Map<String, Object>> resultList = new LinkedList<Map<String, Object>>();

		Integer resultCode = (Integer) searchResult.get("code");
		if(resultCode == 0){
			Map<String, Object> result = (Map<String, Object>) (((Map<String, Object>)searchResult.get("data"))).get("song");
			List<Map<String, Object>> songs = (List<Map<String, Object>>) result.get("list");//对null值进行强制转换后的返回值为null
			if(songs == null){
				return null;
			}
			logger.info("[QQ音乐]search results count: "+songs.size()+" of "+result.get("totalnum"));
			for (Map<String, Object> song : songs) {//foreach也要对songs进行null判断，否则会报错
				String f = (String) song.get("f");
				if(StringUtil.isNotBlank(f)){
					String[] fileds = f.split("\\|");
					if(fileds.length == 25){
						Map<String, Object> mp3 = new HashMap<String, Object>();
						//4810477|僕じゃない|12411|Angela|427696|ZERO|2282174|264|6|1|2|10587117|4234132|320000|0|0|0|6291201|6652667|0
						//|004NXqYt3TEgWR|000oEvjV2FuRnP|001Vnlyn2eaGew|0|8013
						mp3.put("title", fileds[1]);
						mp3.put("artist", fileds[3]);
						mp3.put("album", fileds[5]);
						mp3.put("duration", 1000 * Integer.parseInt(fileds[7]));
						mp3.put("id", fileds[20]);
						mp3.put("img_id", fileds[22]);
						mp3.put("lrc_id", fileds[0]);
						resultList.add(mp3);
					}
				}
			}
		}
    	return resultList;
    }
	
	
	public byte[] downloadPic(String img_id) throws IOException {
		byte[] content = null;
		if(StringUtil.isNotBlank(img_id) && img_id.length()>2){
			String s1 = img_id.substring(img_id.length()-2, img_id.length()-1);
			String s2 = img_id.substring(img_id.length()-1, img_id.length());
			HttpUriRequest request = RequestBuilder
					.get()
					//http://i.gtimg.cn/music/photo/mid_album_500/e/w/001Vnlyn2eaGew.jpg
					.setUri("http://i.gtimg.cn/music/photo/mid_album_500/"+s1+"/"+s2+"/"+img_id+".jpg")
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
		}
		return content;
	}
	
	
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		System.out.println(new QQMusicApi(HttpClients.createDefault(), new ObjectMapper()).searchMusic("僕じゃない"));
		System.out.println(new QQMusicApi(HttpClients.createDefault(), new ObjectMapper()).downloadPic("001Vnlyn2eaGew"));
	}
}
