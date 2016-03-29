# TcHunter
图词hunter，这是一个自动获取mp3图词的工具。  

# Feature
1.图词信息来自网易云音乐。  
2.会更新mp3的id3v2 tag中的title、album、track、artist以及专辑封面，歌词会以同名的lrc文件保存。  
3.多线程。  

# Usage
1.备份一下你的mp3文件或文件夹，并删除已有的错误的lrc文件。  
2.直接导入项目run或者通过maven build出jar包来run。  
  java -jar TCHunter.jar 文件或目录   


# Further Improvements
1.sysout用log替换掉，否则影响性能。  
2.任务异常时的处理。  
3.优化搜索图词的关键字抽取。