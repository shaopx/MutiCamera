# MutiCamera
前后摄像头同时打开的拍照录制成mp4视频文件的demo, 同时演示了美颜,贴纸,滤镜等功能; 演示了对opengl es, mediacodec等api的使用

![screen1](https://github.com/shaopx/MutiCamera/blob/master/screencap/demo1.jpg "Magic Gardens")
![screen2](https://github.com/shaopx/MutiCamera/blob/master/screencap/demo2.jpg "Magic Gardens")
![screen3](https://github.com/shaopx/MutiCamera/blob/master/screencap/demo3.jpg "Magic Gardens")
![screen4](https://github.com/shaopx/MutiCamera/blob/master/screencap/demo4.jpg "Magic Gardens")

## 演示的功能点  
-- 前后摄像头同时开启   
使用opengl es将前后两个摄像头得到的数据进行合并渲染, 使用了离屏渲染技术
目前这个功能只能在21年之后的主流中高端手机上可用, 不只底层硬件要支持(比如高通基线), 还要在系统软件层面支持(谷歌好像是从21年后面的版本才支持).   
在华为鸿蒙系统上目前不支持.   
在小米10, 小米11上自测可用.   

-- 滤镜切换    
实现了几个简单的滤镜, 可以左右滑动屏幕进行切换, 使用的仍然也是opengl离屏渲染技术.       

-- 美颜功能   
只对小窗口前置摄像头的画面进行了美颜, 演示了如何在opengl es中使用美颜的方法, 当然具体的参数需要进一步校准, 这里这是作为一个演示. 

-- 贴纸   
将一张png图片在录制视频时, 实时渲染到视口画面, 水印功能本质上与贴纸的实现思路相同, 就不再演示了. 主要是生成水印需要额外的字体渲染功能, 比较费时. 

-- 录制为mp4   
使用android的mediaCodec组件进行录制, 比使用ffmpeg更快, 兼容性更高. 对于只是android端的视频录制, 完全没必要使用ffmpeg, 不止是复杂的问题, 性能兼容性也没法比.

-- 视频播放   
录制完成之后, 会自动播放刚录制成功的视频, 使用的是exoplayer, 这里用的还是exoplayer2, 没来得及升级到jetpack库media3, 不影响功能. 

## 未来计划
-- 视频录制的自动分段    
-- h265格式的支持   
-- 人脸的自动识别, 包括五官的识别    

