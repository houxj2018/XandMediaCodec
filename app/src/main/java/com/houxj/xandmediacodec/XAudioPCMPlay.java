package com.houxj.xandmediacodec;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.houxj.xandmediacodec.utils.JLogEx;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by 侯晓戬 on 2018/7/11.
 * 音频原始数据（PCM）播放
 */

public class XAudioPCMPlay implements AudioTrack.OnPlaybackPositionUpdateListener {
    private final static int RATE_IN_HZ = 48000;
    private AudioTrack mAudioTrack = null;
    private String mPCMFile =  null;
    private IXAudioPCMPLayCallBack mPlayCallBack = null;

    //TODO 建立实例
    public static XAudioPCMPlay newInstance(){
        return new XAudioPCMPlay();
    }

    private XAudioPCMPlay(){
    }

    private void initAudioTrack(){
        int bufferSize = AudioTrack.getMinBufferSize(RATE_IN_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        JLogEx.d("bufferSize= %d", bufferSize);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                RATE_IN_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);
        mAudioTrack.setPositionNotificationPeriod(RATE_IN_HZ);
        mAudioTrack.setPlaybackPositionUpdateListener(this);
    }

    //TODO 设置PCM文件地址
    public XAudioPCMPlay setPCMPath(String path){
        mPCMFile = path;
        return this;
    }

    //TODO 开始播放PCM数据
    public XAudioPCMPlay play(){
        initAudioTrack();
        if(null != mAudioTrack){
            mAudioTrack.play();
            if(null != mPCMFile){//启动读文件线程
                new Thread(new readFileRunnable()).start();
            }
        }
        return this;
    }

    //TODO 停止PCM数据播放
    public void stop(){
        if(null != mAudioTrack){
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    //TODO 写入PCM数据
    public void write(byte[] pcm){
        write(pcm, pcm.length);
    }
    public void write(byte[] pcm, int length){
        if(null != mAudioTrack){
            mAudioTrack.write(pcm, 0, length);
        }
    }

    // TODO 设置数据总长度,用来计算播放完成位置
    public XAudioPCMPlay setPcmTotalLen(int len){
        if(null !=mAudioTrack){
            mAudioTrack.setNotificationMarkerPosition(len/2);
        }
        return this;
    }

    //TODO 设置播放回调
    public XAudioPCMPlay setPlayListener(IXAudioPCMPLayCallBack callBack){
        mPlayCallBack = callBack;
        return this;
    }


    //获取PCM文件总时长
    public static int getPcmTotalTime(String pcmFile){
        FileInputStream inputStream = null;
        int time_sec = 0;
        try {
            inputStream = new FileInputStream(pcmFile);
            int file_len = inputStream.available();
            JLogEx.d("file_len=%d", file_len);
            time_sec = file_len / RATE_IN_HZ / 2;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(null != inputStream){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return time_sec;
    }

    @Override
    public void onMarkerReached(AudioTrack track) {
        JLogEx.d();
        if(null != mPlayCallBack){
            mPlayCallBack.onComplete();
        }
    }

    @Override
    public void onPeriodicNotification(AudioTrack track) {
        int time = track.getPlaybackHeadPosition()/track.getPositionNotificationPeriod();
        JLogEx.d("T=%d",time);
        if(null != mPlayCallBack){
            mPlayCallBack.onProgressChanged(time);
        }
    }


    //TODO 读取PCM文件线程
    private class readFileRunnable implements Runnable{
        @Override
        public void run() {
            FileInputStream inputStream = null;
            byte[] buff = new byte[1024];
            try {
                inputStream = new FileInputStream(mPCMFile);
                setPcmTotalLen(inputStream.available());
                int readSize = inputStream.read(buff);
                while (readSize > 0){
                    write(buff, readSize);
                    readSize = inputStream.read(buff);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(null != inputStream){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
