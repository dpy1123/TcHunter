package top.devgo.tchunter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import top.devgo.tchunter.util.StringUtil;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * Mp3Info工具类
 * @author dd
 *
 */
public class Mp3Helper {
	
	/**
	 * 去除Mp3Info
	 * @param mp3file
	 * @throws NotSupportedException
	 * @throws IOException
	 * @throws InvalidDataException 
	 * @throws UnsupportedTagException 
	 */
	public static void removeMp3Info(String filePath) throws NotSupportedException, IOException, UnsupportedTagException, InvalidDataException{
		TcMp3File mp3file = new TcMp3File(filePath);
		if (mp3file.hasId3v1Tag()) {
			mp3file.removeId3v1Tag();//有问题，id3v1标签无法完全移除
		}
		if (mp3file.hasId3v2Tag()) {
			mp3file.removeId3v2Tag();
		}
		if (mp3file.hasCustomTag()) {
			mp3file.removeCustomTag();
		}
		mp3file.save();
	}
	
	/**
	 * 更新专辑图<br>
	 * 如果有id3v2Tag，并且原来没有图或新图比原图高清，则更新
	 * @param file
	 * @param albumImageData
	 * @param mimeType
	 * @throws NotSupportedException
	 * @throws IOException
	 */
	public static void updateAlbumImg(TcMp3File file, byte[] albumImageData, String mimeType) throws NotSupportedException, IOException {
		if (file.hasId3v2Tag()) {
			ID3v2 id3v2Tag = file.getId3v2Tag();
			byte[] old_img_data = id3v2Tag.getAlbumImage();
			if(old_img_data == null || old_img_data.length < albumImageData.length){
				id3v2Tag.setAlbumImage(albumImageData, mimeType);
				file.save();
			}
		}
	}
	
	/**
	 * 更新mp3file的信息
	 * @param mp3file
	 * @param bestfit_mp3Info
	 * @throws IOException 
	 * @throws NotSupportedException 
	 */
	public static void updateMp3Info(TcMp3File mp3file, Map<String, Object> bestfit_mp3Info) throws NotSupportedException, IOException {
    	String title, track, artist, album;
//		ID3v1 id3v1Tag = null;
//		if (mp3file.hasId3v1Tag()) {
//			id3v1Tag = mp3file.getId3v1Tag();
//		} else {
//			// mp3 does not have an ID3v1 tag, let's create one..
//			id3v1Tag = new ID3v1Tag();
//			mp3file.setId3v1Tag(id3v1Tag);
//		}
//		title = id3v1Tag.getTitle();
//		if(StringUtil.isBlank(title) || StringUtil.isMessyCode(title)){
//			id3v1Tag.setTitle((String) bestfit_mp3Info.get("title"));
//		}
//		track = id3v1Tag.getTrack();
//		if(StringUtil.isBlank(track) ){
//			String no = (String) bestfit_mp3Info.get("track");
//			if(StringUtil.isNotBlank(no)){
//				no = no.substring(0, no.lastIndexOf("/"));
//				id3v1Tag.setTrack(no);
//			}
//		}
//		artist = id3v1Tag.getArtist();
//		if(StringUtil.isBlank(artist) || StringUtil.isMessyCode(artist)){
//			id3v1Tag.setArtist((String) bestfit_mp3Info.get("artist"));
//		}
//		album = id3v1Tag.getAlbum();
//		if(StringUtil.isBlank(album) || StringUtil.isMessyCode(album)){
//			id3v1Tag.setAlbum((String) bestfit_mp3Info.get("album"));
//		}
    	ID3v2 id3v2Tag = null;
		if (mp3file.hasId3v2Tag()) {
			id3v2Tag = mp3file.getId3v2Tag();
		} else {
			// mp3 does not have an ID3v2 tag, let's create one..
			id3v2Tag = new ID3v24Tag();
			mp3file.setId3v2Tag(id3v2Tag);
		}
		title = id3v2Tag.getTitle();
		if(StringUtil.isBlank(title) || StringUtil.isMessyCode(title)){
			id3v2Tag.setTitle((String) bestfit_mp3Info.get("title"));
		}
		track = id3v2Tag.getTrack();
		if(StringUtil.isBlank(track) ){
			Object trk = bestfit_mp3Info.get("track");
			if(trk instanceof Integer){
				id3v2Tag.setTrack(trk+"");
			}else{
				id3v2Tag.setTrack((String)trk);
			}
		}
		artist = id3v2Tag.getArtist();
		if(StringUtil.isBlank(artist) || StringUtil.isMessyCode(artist)){
			id3v2Tag.setArtist((String) bestfit_mp3Info.get("artist"));
		}
		album = id3v2Tag.getAlbum();
		if(StringUtil.isBlank(album) || StringUtil.isMessyCode(album)){
			id3v2Tag.setAlbum((String) bestfit_mp3Info.get("album"));
		}
		mp3file.save();
	}
	 
    /**
     * 获取mp3文件详情
     * @param filePath
     * @return Map={duration=264594, artist=angela, album=ZERO, title=僕じゃない, track=2/12, has_album_pic=false}
     * @throws UnsupportedTagException
     * @throws InvalidDataException
     * @throws IOException
     */
    public static Map<String, Object> getMp3Info(String filePath) throws UnsupportedTagException, InvalidDataException, IOException{
		Map<String, Object> info = new HashMap<String, Object>();

		String name = filePath.substring(filePath.lastIndexOf('\\')+1, filePath.lastIndexOf('.'));
		info.put("title", name);
		Mp3File mp3file = new Mp3File(filePath);
		info.put("duration", mp3file.getLengthInMilliseconds());
//		if (mp3file.hasId3v1Tag()) {
//			ID3v1 id3v1Tag = mp3file.getId3v1Tag();
//			String artist = id3v1Tag.getArtist();
//			if (StringUtil.isNotBlank(artist) && !StringUtil.isMessyCode(artist))
//				info.put("artist", artist);
//			String title = id3v1Tag.getTitle();
//			if (StringUtil.isNotBlank(title) && !StringUtil.isMessyCode(title))
//				info.put("title", title);
//			String album = id3v1Tag.getAlbum();
//			if (StringUtil.isNotBlank(album) && !StringUtil.isMessyCode(album))
//				info.put("album", album);
//		}
		if (mp3file.hasId3v2Tag()) {
			ID3v2 id3v2Tag = mp3file.getId3v2Tag();
			String track = id3v2Tag.getTrack();
			if (StringUtil.isNotBlank(track) && !StringUtil.isMessyCode(track))
				info.put("track", track);
			String artist = id3v2Tag.getArtist();
			if (StringUtil.isNotBlank(artist) && !StringUtil.isMessyCode(artist) &&
					StringUtil.isMeanful(artist))
				info.put("artist", artist);
			String title = id3v2Tag.getTitle();
			if (StringUtil.isNotBlank(title) && !StringUtil.isMessyCode(title) &&
					title.replaceAll("((?=[\\x21-\\x7e]+)[^A-Za-z0-9])", "").length() > name.replaceAll("((?=[\\x21-\\x7e]+)[^A-Za-z0-9])", "").length())
					//title要比name更有价值，即去除特殊符号，字符串更长的认为更有价值
				info.put("title", title);
			String album = id3v2Tag.getAlbum();
			if (StringUtil.isNotBlank(album) && !StringUtil.isMessyCode(album))
				info.put("album", album);
			byte[] albumImageData = id3v2Tag.getAlbumImage();
			info.put("has_album_pic", albumImageData != null);
		}
 	   
    	return info;
    }
    
    /**
     * 控制台打印Mp3Info
     * @param path
     * @throws UnsupportedTagException
     * @throws InvalidDataException
     * @throws IOException
     */
    public static void displayMp3Info(String path) throws UnsupportedTagException, InvalidDataException, IOException{
		Mp3File mp3file = new Mp3File(path);
		if (mp3file.hasId3v1Tag()) {
			ID3v1 id3v1Tag = mp3file.getId3v1Tag();
			System.out.println("Track: " + id3v1Tag.getTrack());
			System.out.println("Artist: " + id3v1Tag.getArtist());
			System.out.println("Title: " + id3v1Tag.getTitle());
			System.out.println("Album: " + id3v1Tag.getAlbum());
			System.out.println("Year: " + id3v1Tag.getYear());
			System.out.println("Genre: " + id3v1Tag.getGenre() + " ("+ id3v1Tag.getGenreDescription() + ")");
			System.out.println("Comment: " + id3v1Tag.getComment());
		}
        if (mp3file.hasId3v2Tag()) {
			ID3v2 id3v2Tag = mp3file.getId3v2Tag();
			System.out.println("Track: " + id3v2Tag.getTrack());
			System.out.println("Artist: " + id3v2Tag.getArtist());
			System.out.println("Title: " + id3v2Tag.getTitle());
			System.out.println("Album: " + id3v2Tag.getAlbum());
			System.out.println("Year: " + id3v2Tag.getYear());
			System.out.println("Genre: " + id3v2Tag.getGenre() + " (" + id3v2Tag.getGenreDescription() + ")");
			System.out.println("Comment: " + id3v2Tag.getComment());
			System.out.println("Composer: " + id3v2Tag.getComposer());
			System.out.println("Publisher: " + id3v2Tag.getPublisher());
			System.out.println("Original artist: " + id3v2Tag.getOriginalArtist());
			System.out.println("Album artist: " + id3v2Tag.getAlbumArtist());
			System.out.println("Copyright: " + id3v2Tag.getCopyright());
			System.out.println("URL: " + id3v2Tag.getUrl());
			System.out.println("Encoder: " + id3v2Tag.getEncoder());
			byte[] albumImageData = id3v2Tag.getAlbumImage();
			if (albumImageData != null) {
				System.out.println("Have album image data, length: " + albumImageData.length + " bytes");
				System.out.println("Album image mime type: " + id3v2Tag.getAlbumImageMimeType());
			}
		}
	}
    
}
