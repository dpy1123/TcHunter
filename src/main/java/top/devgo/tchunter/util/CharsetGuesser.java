package top.devgo.tchunter.util;

import java.io.ByteArrayInputStream;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;

class CharsetGuesser {
	private String result = null;
	private boolean found = false;

	/**
	 * 
	 * @param string
	 * @param languageHint
	 *            语言提示区域代码 eg：1 : Japanese; 2 : Chinese; 3 : Simplified Chinese;
	 *            4 : Traditional Chinese; 5 : Korean; 6 : Dont know (default)
	 * @return
	 */
	public String guess(byte[] string, int languageHint) {
		// Initalize the nsDetector() ;
		nsDetector det = new nsDetector(languageHint);

		// Set an observer...
		// The Notify() will be called when a matching charset is found.
		det.Init(new nsICharsetDetectionObserver() {
			public void Notify(String charset) {
				found = true;
				result = charset;
			}
		});

		ByteArrayInputStream imp = new ByteArrayInputStream(string);

		byte[] buf = new byte[1024];
		int len;
		boolean done = false;
		boolean isAscii = true;

		while ((len = imp.read(buf, 0, buf.length)) != -1) {

			// Check if the stream is only ascii.
			if (isAscii)
				isAscii = det.isAscii(buf, len);

			// DoIt if non-ascii and not done yet.
			if (!isAscii && !done)
				done = det.DoIt(buf, len, false);
		}
		det.DataEnd();
		if (isAscii) {
			result = "ASCII";
			found = true;
		}

		if (!found) {
			String prob[] = det.getProbableCharsets();
			if (prob.length > 0) { // 在没有发现情况下，则取第一个可能的编码
				result = prob[0];
			}
		}
		return result;
	}
	
}