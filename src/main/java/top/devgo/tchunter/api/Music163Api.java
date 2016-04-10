package top.devgo.tchunter.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

/**
 * 调用网易云音乐的接口
 * @author dd
 *
 */
public class Music163Api {
	private static Logger logger = Logger.getLogger(Music163Api.class.getName());
	/**
	 * 搜索最多返回条数，默认100
	 */
	public static int MAX_SEARCH_RESULT = 100;
	
	private CloseableHttpClient httpclient;
	private ObjectMapper mapper; 
	
	public Music163Api(CloseableHttpClient httpclient, ObjectMapper mapper) {
		this.httpclient = httpclient;
		this.mapper = mapper;
	}
	
	/**
	 * 
	 * @param keywords
	 * @return LinkedList<br> 
     * Map={duration=264546, artist=angela, album=ZERO, album_pic=http://p4.music.126.net/-ru0sOeqEv4DtO3-_k0x-w==/2392537302068353.jpg,
     *  id=26235098, title=僕じゃない, track=2/12}
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
		logger.info("[网易云音乐]keyword: "+keyword);
		
		HttpUriRequest request = RequestBuilder
				.post()
				.setUri("http://music.163.com/api/search/pc")
				.addParameter("s", keyword)
				.addParameter("offset", "0")
				.addParameter("limit", String.valueOf(MAX_SEARCH_RESULT))
				.addParameter("type", "1")
				.setHeader("Referer", "http://music.163.com/")
				.setHeader("Cookie", "appver=1.5.0.75771;")
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
		return parseSearchResult(searchResult);
	}
	
	/**
     * @param searchResult
     * @return LinkedList<br> 
     * Map={duration=264546, artist=angela, album=ZERO, album_pic=http://p4.music.126.net/-ru0sOeqEv4DtO3-_k0x-w==/2392537302068353.jpg,
     *  id=26235098, title=僕じゃない, track=2/12}
     */
    @SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseSearchResult(Map<String, Object> searchResult) {
    	List<Map<String, Object>> resultList = new LinkedList<Map<String, Object>>();
    	
		Integer resultCode = (Integer) searchResult.get("code");
		if(resultCode == 200){
			Map<String, Object> result = (Map<String, Object>) searchResult.get("result");
			List<Map<String, Object>> songs = (List<Map<String, Object>>) result.get("songs");//对null值进行强制转换后的返回值为null
			if(songs == null){
				return null;
			}
			logger.info("[网易云音乐]search results count: "+songs.size()+" of "+result.get("songCount"));
			for (Map<String, Object> song : songs) {//foreach也要对songs进行null判断，否则会报错
				//艺术家
				List<Map<String, Object>> artists = (List<Map<String, Object>>) song.get("artists");
				String artistNames = "";
				for (Map<String, Object> artist : artists) {
					artistNames += "/"+artist.get("name");
				}
				artistNames = artistNames.substring(1);
				//专辑
				Map<String, Object> album = (Map<String, Object>) song.get("album");
				String album_name = (String) album.get("name");
				Integer album_size = (Integer) album.get("size");
				String album_pic = (String) album.get("picUrl");
				
				Map<String, Object> mp3 = new HashMap<String, Object>();
				mp3.put("title", song.get("name"));
				mp3.put("id", song.get("id"));
				mp3.put("track", song.get("position")+"/"+album_size);
				mp3.put("artist", artistNames);
				mp3.put("album", album_name);
				mp3.put("duration", song.get("duration"));
				mp3.put("album_pic", album_pic);
				
				resultList.add(mp3);
			}
		}
    	return resultList;
    }
	
	@SuppressWarnings("unchecked")
	public String getLyric(String id) throws IOException {
		String result = null;
		if(StringUtil.isBlank(id)) return result;
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://music.163.com/api/song/lyric")//?os=pc&id=93920&lv=-1&kv=-1&tv=-1
				.addParameter("id", id)
				.addParameter("lv", "-1")
				.addParameter("kv", "-1")
				.addParameter("tv", "-1")
				.setHeader("Referer", "http://music.163.com/")
				.setHeader("Cookie", "appver=1.5.0.75771;")
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
		if(result_code == 200){
			Map<String, Object> lrc = (Map<String, Object>) lrcInfo.get("lrc");
			if(lrc != null)
				result = (String) lrc.get("lyric");
		}
		return result;
	}
	
	public void downloadPic(String url, String filename) throws IOException {
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri(url)
				.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36")
				.build();
		CloseableHttpResponse response = httpclient.execute(request);
		OutputStream fos = new FileOutputStream(new File(filename));
		
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				fos.write(EntityUtils.toByteArray(entity));
			}	
		} finally {
			response.close();
			fos.close();
		}
	}
	
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
		System.out.println(new Music163Api(HttpClients.createDefault(), new ObjectMapper()).searchMusic("僕じゃない"));
	}
}
