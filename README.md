# XandMediaCodec
MediaCodec 解码音频输出PCM数据，和播放PCM数据文件的库

AAR 库生成方法：
	1、修改根目录的 config.gradle 的 isDebugType = false （true 是调试模式生成的是app）
	2、直接运行批处理文件 build-aar.bat (这个就是编译成AAR的)
	3、执行完成以后在 app/output 目录下生成 带版本号的aar库文件

注意第一次生成的时候回下载 gradle 和各种库文件



修改说明：

20187-17：
	增加了seekto 接口，可以跳转到指定的时间位置开始解码
	增加了停止接口，可以随时停止解码
	demo调试上直接将解码和播放对接上了