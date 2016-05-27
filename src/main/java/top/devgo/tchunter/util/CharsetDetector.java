package top.devgo.tchunter.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.ByteOrderMarkDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.ParsingDetector;

/**
 * 封装自cpDetector
 * @author dd
 *
 */
public class CharsetDetector {

	  // Create the proxy:
	private static CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance(); // A singleton.
	 
	static{
		// Add the implementations of info.monitorenter.cpdetector.io.ICodepageDetector: 
	    // This one is quick if we deal with unicode codepages: 
	    detector.add(new ByteOrderMarkDetector()); 
	    // The first instance delegated to tries to detect the meta charset attribut in html pages.
	    detector.add(new ParsingDetector(true)); // be verbose about parsing.
	    // This one does the tricks of exclusion and frequency detection, if first implementation is 
	    // unsuccessful:
	    detector.add(JChardetFacade.getInstance()); // Another singleton.
	    detector.add(ASCIIDetector.getInstance()); // Fallback, see javadoc.
	    
//	    detector是探测器，它把探测任务交给具体的探测实现类的实例完成。
//      cpDetector内置了一些常用的探测实现类，这些探测实现类的实例可以通过add方法 加进来，如ParsingDetector、JChardetFacade、ASCIIDetector、UnicodeDetector。
//      detector按照“谁最先返回非空的探测结果，就以该结果为准”的原则返回探测到的字符集编码。
//	    	ParsingDetector可用于检查HTML、XML等文件或字符流的编码,构造方法中的参数用于指示是否显示探测过程的详细信息，为false不显示。
//      	JChardetFacade封装了由Mozilla组织提供的JChardet，它可以完成大多数文件的编码测定。
	}
	
	
	/**
	 * 
	 * @param string 待测的文本数组
	 * @param length 待测的字节数
	 * @return
	 */
	public static String detect(byte[] string, int length) {
		Charset charset = null;
		ByteArrayInputStream inputStream = new ByteArrayInputStream(string);
		try {
            charset = detector.detectCodepage(inputStream, length);//detectCodepage(待测的文本输入流,测量该流所需的读入字节数); 字节数越多，判定越准确，当然时间也花得越长。
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally{
        	IOUtil.close(inputStream);
        }
        if (charset != null)
            return charset.name();
        else
            return null;
	}
	
	public static String detect(byte[] string) {
		return detect(string, string.length);
	}
	
	public static void main(String[] args) {
		System.out.println(detect("AAAAAAAAAAAAAAAAAAAAAA好き".getBytes()));
	}
}
