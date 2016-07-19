package top.devgo.tchunter.file;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.valuepair.ImageFormats;
import org.jaudiotagger.tag.reference.PictureTypes;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import top.devgo.tchunter.util.StringUtil;

/**
 * Flac操作类
 * @author dd
 *
 */
public class FlacOper implements MusicFileOper {

	/**
     * 获取flac文件详情
     * @param filePath
     * @return Map={duration=264594, artist=angela, album=ZERO, title=僕じゃない, track=2/12, has_album_pic=false}
	 * @throws InvalidAudioFrameException 
	 * @throws ReadOnlyFileException 
	 * @throws TagException 
	 * @throws IOException 
	 * @throws CannotReadException 
     * @throws Exception
     */
	@Override
	public Map<String, Object> getInfo(String filePath) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		Map<String, Object> info = new HashMap<String, Object>();
		
		String name = filePath.substring(filePath.lastIndexOf('\\')+1, filePath.lastIndexOf('.'));
		info.put("title", name);

		AudioFile audioFile = AudioFileIO.read(new File(filePath));
		FlacTag tag = (FlacTag) audioFile.getTag();
		VorbisCommentTag vorbisTag = tag.getVorbisCommentTag();
		
		String title = vorbisTag.getFirst(VorbisCommentFieldKey.TITLE);
		if (StringUtil.isNotBlank(title) && !StringUtil.isMessyCode(title) &&
				title.replaceAll("((?=[\\x21-\\x7e]+)[^A-Za-z0-9])", "").length() > name.replaceAll("((?=[\\x21-\\x7e]+)[^A-Za-z0-9])", "").length())
				//title要比name更有价值，即去除特殊符号，字符串更长的认为更有价值
			info.put("title", title);
		
		info.put("album", vorbisTag.getFirst(VorbisCommentFieldKey.ALBUM));
		info.put("artist", vorbisTag.getFirst(VorbisCommentFieldKey.ARTIST));
		info.put("duration", audioFile.getAudioHeader().getTrackLength()*1000L);
		info.put("track", vorbisTag.getFirst(VorbisCommentFieldKey.TRACKNUMBER)+"/"+vorbisTag.getFirst("TOTALTRACKS"));
		info.put("has_album_pic", !tag.getImages().isEmpty());

		return info;
    }
    
	@Override
    public void updateInfo(String filePath, Map<String, Object> bestfitMp3Info) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException  {
    	String title, track, artist, album;
    	AudioFile audioFile = AudioFileIO.read(new File(filePath));
    	FlacTag tag = (FlacTag) audioFile.getTag();
    	
		title = tag.getFirst(FieldKey.TITLE);
		if(StringUtil.isBlank(title) || StringUtil.isMessyCode(title)){
			tag.setField(FieldKey.TITLE, (String) bestfitMp3Info.get("title"));
		}
		track = tag.getFirst(FieldKey.TRACK);
		if(StringUtil.isBlank(track) ){
			Object trk = bestfitMp3Info.get("track");
			if(trk instanceof Integer){
				tag.setField(FieldKey.TRACK, trk+"");
			}else{
				String trkStr = (String) trk;
				String[] tracks = trkStr.split("/");
				if (tracks.length==2) {
					tag.setField(FieldKey.TRACK, tracks[0]);
					tag.setField(FieldKey.TRACK_TOTAL, tracks[1]);
				}
			}
		}
		artist = tag.getFirst(FieldKey.ARTIST);
		if(StringUtil.isBlank(artist) || StringUtil.isMessyCode(artist)){
			tag.setField(FieldKey.ARTIST, (String) bestfitMp3Info.get("artist"));
		}
		album = tag.getFirst(FieldKey.ALBUM);
		if(StringUtil.isBlank(album) || StringUtil.isMessyCode(album)){
			tag.setField(FieldKey.ALBUM, (String) bestfitMp3Info.get("album"));
		}
    }
    
    
    
    /**
	 * 更新专辑图<br>
	 * 原来没有图或新图比原图高清，则更新
	 * @param file
	 * @param albumImageData
	 * @param mimeType
     * @throws InvalidAudioFrameException 
     * @throws ReadOnlyFileException 
     * @throws TagException 
     * @throws IOException 
     * @throws CannotReadException 
	 * @throws Exception 
	 */
	@Override
	public void updateAlbumImg(String filePath, byte[] albumImageData, String mimeType) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		AudioFile audioFile = AudioFileIO.read(new File(filePath));
    	FlacTag tag = (FlacTag) audioFile.getTag();
    	
    	BufferedImage img = ImageIO.read(new ByteArrayInputStream(albumImageData));
    	
    	if (tag.getImages().isEmpty()) {
    		tag.setField(tag.createArtworkField(albumImageData, PictureTypes.DEFAULT_ID, 
    				ImageFormats.getMimeTypeForBinarySignature(albumImageData), null,
    				img.getWidth(), img.getHeight(), 24, 0));
		}else{
			byte[] oldImgData = tag.getImages().get(0).getImageData();
			if(oldImgData == null || oldImgData.length < albumImageData.length){
				tag.setField(tag.createArtworkField(albumImageData, PictureTypes.DEFAULT_ID, 
	    				ImageFormats.getMimeTypeForBinarySignature(albumImageData), null,
	    				img.getWidth(), img.getHeight(), 24, 0));
			}
		}
    	
	}

}
