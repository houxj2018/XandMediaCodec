package com.houxj.xandmediacodec;

/**
 * Created by 侯晓戬 on 2018/7/11.
 * PCM数据播放回调
 */

public interface IXAudioPCMPLayCallBack {
    public void onProgressChanged(int time);
    public void onComplete();
}
