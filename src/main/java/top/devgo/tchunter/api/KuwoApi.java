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

import top.devgo.tchunter.util.StringUtil;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class KuwoApi {
	/**
	 * 搜索最多返回条数，默认100
	 */
	public static int MAX_SEARCH_RESULT = 100;
	
	private CloseableHttpClient httpclient;
	private ObjectMapper mapper; 
	
	public KuwoApi(CloseableHttpClient httpclient, ObjectMapper mapper) {
		this.httpclient = httpclient;
		this.mapper = mapper;
		mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true) ;  
		mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
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
		System.out.println("keyword: "+keyword);
		
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://search.kuwo.cn/r.s")
				.addParameter("all", keyword)
				.addParameter("ft", "music")
				.addParameter("itemset", "web_2016")//api版本，返回的json格式不同，可选web_2013 web_2015 web_2016
				.addParameter("client", "kt")
				.addParameter("pn", "0")
				.addParameter("rn", String.valueOf(MAX_SEARCH_RESULT))
				.addParameter("rformat", "json")
				.addParameter("encoding", "utf8")
				.setHeader("Host", "search.kuwo.cn")
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
    	
		List<Map<String, Object>> songs = (List<Map<String, Object>>) searchResult.get("abslist");//对null值进行强制转换后的返回值为null
		if(songs == null){
			return resultList;
		}
		System.out.println("search results count: "+songs.size()+" of "+searchResult.get("TOTAL"));
		for (Map<String, Object> song : songs) {//foreach也要对songs进行null判断，否则会报错
			Map<String, Object> mp3 = new HashMap<String, Object>();
			mp3.put("title", song.get("NAME"));
			mp3.put("id", song.get("MUSICRID"));
			mp3.put("artist", song.get("ARTIST"));
			mp3.put("album", song.get("ALBUM"));
			mp3.put("duration", 1000 * Integer.parseInt((String) song.get("DURATION")));
			
//			System.out.println(mp3);
			resultList.add(mp3);
		}
    	return resultList;
    }
	
	/**
	 * 
	 * @param id
	 * @return {music_id=3341247, mv_rid=MV_0, name=僕じゃない, song_url=http://yinyue.kuwo.cnhttp://yinyue.kuwo.cn/yy/gequ-angela_music/3341247.htm, artist=angela, artid=1921, singer=angela, special=Zero, ridmd591=0518E9A91DA1EB3B5EF1C568B55CFE3B, mp3size=10.09 MB, artist_url=http://yinyue.kuwo.cnhttp://yinyue.kuwo.cn/yy/geshou-angela/angela.htm, auther_url=http://www.kuwo.cn/mingxing/angela/, playid=play?playMQnumMQname0g1ekuKTjpMqkpAartist0YW5nZWxhssig10MTIxNjc1MTQ2NQssig20MjM1NzQ5MjE2MAmusicrid0TVVTSUNfMzM0MTI0Nwmvrid0TVZfMAmp3size0MTAuMDkgTUImrid0TVAzXzMzNDEyNDcmsig10MzQxNjgyNDEzmsig20MzM5MjAzMjY0OAmkvnsig10MAmkvnsig20MAmkvrid0TVZfMAmvsig10MAmvsig20MAsize0NC4wNyBNQgalbum0WmVybwkalaok0MAhasecho0MAfiletype0c29uZwscore0Mgsource0aHR0cDovL2VucGluZy5uZXQuY24vYWRtaW4vdXBsb2FkL211c2ljLzY4NjMwOTAxNi53bWEmvprovider0, artist_pic=http://img3.kuwo.cn/star/starheads/120/3/6388794be2cf5f298978498ff3c64a2_0.jpg, artist_pic240=http://img1.kuwo.cn/star/starheads/240/89/95/982251370.jpg, path=m3/2/13/2899147340.wma, mp3path=n1/16/96/4249280555.mp3, aacpath=a1/58/56/745715527.aac, wmadl=wmadl.cdn.kuwo.cn, mp3dl=other.web.rc01.sycdn.kuwo.cn, aacdl=other.web.rc03.sycdn.kuwo.cn, lyric=DBYAHlReXEpRUEAeCgxVEgAORRgLG0MXCRgaCwoRAB5UAwEaBAkEBhwaXxcAHVReSAsMAVEkOj0wJjpfWltfS1FS, lyric_zz=DBYAHlReXEpRUEAeCgxVEgAORRgLG0MXCRgaCwoRAB5UAwEaBAkEBhwaXxcAHVReSAsMAVEkOj0wJjpfWltfS1FSSgUdDQFYVA}
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Map<String, Object> getMusicInfo(String id) throws ClientProtocolException, IOException{
		
		HttpUriRequest request = RequestBuilder
				.get()
				.setUri("http://player.kuwo.cn/webmusic/st/getNewMuiseByRid")
				.addParameter("rid", id)
				.setHeader("Host", "player.kuwo.cn")
				.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36")
				.build();
		
		CloseableHttpResponse response = httpclient.execute(request);
		String jsonResult = null;
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				jsonResult = EntityUtils.toString(entity, "utf-8");
			}	
		} finally {
			if (response != null) {
				response.close();
			}
		}
		//parse result
		jsonResult = jsonResult.replaceAll("=", "").replaceAll("&", "");
		ObjectMapper xmlMapper = new XmlMapper();
		Map<String, Object> searchResult = xmlMapper.readValue(jsonResult, new TypeReference<Map<String, Object>>() { } );
		return searchResult;
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
		System.out.println(new KuwoApi(HttpClients.createDefault(), new ObjectMapper()).searchMusic("僕じゃない"));
		System.out.println(new KuwoApi(HttpClients.createDefault(), new ObjectMapper()).getMusicInfo("MUSIC_3341247").get("artist_pic240"));
	}
}
