package top.devgo.tchunter.file;

import java.util.Map;

/**
 * 对音乐文件的操作
 * @author dd
 *
 */
public interface MusicFileOper {
	
	 public Map<String, Object> getInfo(String filePath) throws Exception;
	 
	 public void updateInfo(String filePath, Map<String, Object> bestfitInfo) throws Exception;
	 
	 public void updateAlbumImg(String filePath, byte[] albumImageData, String mimeType) throws Exception;

}
