package com.houxj.xandmediacodec;

/**
 * Created by 侯晓戬 on 2018/7/10.
 * 音频解码回调监听接口
 */

public interface IXAudioDecodeCallBack {
    public final static int ERROR_ID_FILE_ERROR = -1;   //错误的音频文件
    public final static int ERROR_ID_INIT_FAIL = -2;   //初始化失败

    public void onDecode(byte[] pcm);//解码中，传送解码数据给外部业务
    public void onComplete();   //解码完成
    public void onFail(int error);  //解码失败，参数是失败ID
}
