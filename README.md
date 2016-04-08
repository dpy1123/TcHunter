# TcHunter
图词hunter，这是一个自动获取mp3图词的工具。  

# Feature
* 歌曲信息来自网易云音乐、百度音乐、qq音乐、酷我、天天动听，选最优的；  
  歌词选择：网易163->百度云音乐->天天动听；  
  图片信息： 网易163->qq音乐->酷我->百度云音乐。  
* 会更新mp3的id3v2 tag中的title、album、track、artist以及专辑封面，歌词会以同名的lrc文件保存。  
* 多线程。  

# Usage
* 备份一下你的mp3文件或文件夹，并删除已有的错误的lrc文件。  
* builds文件夹下已有build好的jar包，使用方式：    
  `java -jar TCHunter.jar 文件或目录`


# Further Improvements 
* 任务异常时的处理。  
* 优化搜索图词的关键字抽取。