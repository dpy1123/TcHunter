package top.devgo.tchunter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 * @throws Exception 
	 */
	public void testApp() throws Exception {
		String path = "D:\\test\\01. black bullet.flac";
		AudioFile audioFile = AudioFileIO.read(new File(path));
		FlacTag tag = (FlacTag) audioFile.getTag();
		
		VorbisCommentTag vorbisTag = tag.getVorbisCommentTag();
		

		System.out.println(vorbisTag.getFirst(VorbisCommentFieldKey.ALBUM));
		System.out.println(vorbisTag.getFirst(VorbisCommentFieldKey.ARTIST));
		System.out.println(vorbisTag.getFirst(VorbisCommentFieldKey.TITLE));
		System.out.println(vorbisTag.getFirst(VorbisCommentFieldKey.LYRICS));
		System.out.println(audioFile.getAudioHeader().getTrackLength());
		System.out.println(audioFile.getAudioHeader().getBitRate());
		System.out.println(audioFile.getAudioHeader().getSampleRate());
		
		System.out.println(tag.getImages().size());
		
		Iterator<TagField> it = vorbisTag.getFields();
		while(it.hasNext()){
			System.out.println(it.next().getId());
		}
		
//		assertTrue(true);
		System.out.println(getFlacInfo(path));
		
	}
	
	 /**
     * 获取flac文件详情
     * @param filePath
     * @return Map={duration=264594, artist=angela, album=ZERO, title=僕じゃない, track=2/12, has_album_pic=false}
     * @throws UnsupportedTagException
     * @throws InvalidDataException
     * @throws IOException
     */
    public static Map<String, Object> getFlacInfo(String filePath) throws Exception{
		Map<String, Object> info = new HashMap<String, Object>();
		AudioFile audioFile = AudioFileIO.read(new File(filePath));
		FlacTag tag = (FlacTag) audioFile.getTag();
		VorbisCommentTag vorbisTag = tag.getVorbisCommentTag();
		
		info.put("title", vorbisTag.getFirst(VorbisCommentFieldKey.TITLE));
		info.put("album", vorbisTag.getFirst(VorbisCommentFieldKey.ALBUM));
		info.put("artist", vorbisTag.getFirst(VorbisCommentFieldKey.ARTIST));
		info.put("duration", audioFile.getAudioHeader().getTrackLength()*1000);
		info.put("track", vorbisTag.getFirst(VorbisCommentFieldKey.TRACKNUMBER)+"/"+vorbisTag.getFirst("TOTALTRACKS"));
		info.put("has_album_pic", tag.getImages().size()>0 ? true:false);

		return info;
    }
}
