package top.devgo.tchunter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * 继承Mp3File，增加无参数save方法，直接保存入源文件
 * @author dd
 *
 */
class TcMp3File extends Mp3File {
	public TcMp3File(String mp3file) throws UnsupportedTagException,
			InvalidDataException, IOException {
		super(mp3file);
	}

	public void save() throws IOException, NotSupportedException {
		RandomAccessFile saveFile = new RandomAccessFile(this.file, "rw");
		byte[] originFile = new byte[(int) saveFile.length()];
		saveFile.readFully(originFile);
		saveFile.seek(0);
		try {
			if (hasId3v2Tag()) {
				saveFile.write(getId3v2Tag().toBytes());
			}
			saveMpegFrames(saveFile, originFile);
			if (hasCustomTag()) {
				saveFile.write(getCustomTag());
			}
			if (hasId3v1Tag()) {
				saveFile.write(getId3v1Tag().toBytes());
			}
		} finally {
			saveFile.close();
		}
	}

	private void saveMpegFrames(RandomAccessFile saveFile, byte[] originFile)
			throws IOException {
		int filePos = getXingOffset();
		if (filePos < 0)
			filePos = getStartOffset();
		if (filePos < 0)
			return;
		if (getEndOffset() < filePos)
			return;
		ByteArrayInputStream inputStream = new ByteArrayInputStream(originFile);
		byte[] bytes = new byte[bufferLength];
		try {
			inputStream.skip(filePos);
			while (true) {
				int bytesRead = inputStream.read(bytes, 0, bufferLength);
				if (filePos + bytesRead <= getEndOffset()) {
					saveFile.write(bytes, 0, bytesRead);
					filePos += bytesRead;
				} else {
					saveFile.write(bytes, 0, getEndOffset() - filePos + 1);
					break;
				}
			}
		} finally {
			inputStream.close();
		}
	}
}