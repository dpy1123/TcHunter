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
* 执行结束会输出noPicList和noLrcList，分别给出了找不到图片或歌词的mp3列表。


# Further Improvements 
* 任务异常时的处理。  
    * 访问search.kuwo.cn有时候会Connection timed out, 默认连接超时时间是5s，如发生超时可以再次运行程序。
    * Api.searchMusic在parseJsonResult的时候有时候会因含有非法字符报错，暂时在只发现酷我的某些搜索结果格式不干净。
* 优化搜索图词的关键字抽取。  
    搜索结果的好坏依赖各家的API，因此这里应该尽可能用意义明确的搜索关键字。  
    目前的搜索关键字是title artist album。title选择顺序：文件名->id3v2中的title，其他的信来自id3v2tag。  