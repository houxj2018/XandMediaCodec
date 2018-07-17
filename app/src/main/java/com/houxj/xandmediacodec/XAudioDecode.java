package com.houxj.xandmediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Build;

import com.houxj.xandmediacodec.utils.JLogEx;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by 侯晓戬 on 2018/7/10.
 * 音频解码器
 */

public class XAudioDecode {
    private static final int DECODE_STATE_INIT = 0; //初始化，未开始解码
    private static final int DECODE_STATE_DECODEING = 1; //解码中
    private static final int DECODE_STATE_COMPLETE = 2; //解码完成
    private static final int DECODE_STATE_STOP = 0xFF; //用户停止解码

    private static final int KEY_SAMPLE_RATE = 48000;//采样率
    private IXAudioDecodeCallBack mDecodeListener = null;
    private String mDecodeFile = null;
    private int mAudioTime = 0; //多媒体文件时长
    private MediaExtractor mMediaExtractor;
    private MediaCodec mMediaDecode;
    private int mDecodeTime = -1;  //解码开始时间

    private ByteBuffer[] mDecodeInputBuffers;
    private ByteBuffer[] mDecodeOutputBuffers;
    private MediaCodec.BufferInfo mDecodeBufferInfo;
    private int mDecodeRelust = DECODE_STATE_INIT;  // 0未开始解码；1 解码完成；小于0解码失败 2请求结束；

    //TODO 建立实例
    public static XAudioDecode newInstance(){
        return new XAudioDecode();
    }

    private XAudioDecode(){

    }

    //TODO 设置解码文件
    public XAudioDecode setAudioPath(String file){
        mDecodeFile = file;
        mAudioTime = getMediaFileTime(mDecodeFile);
        return this;
    }

    //TODO 设置解码回调监听
    public XAudioDecode setDecodeLiListener(IXAudioDecodeCallBack callBack){
        mDecodeListener = callBack;
        return this;
    }

    //TODO 设置开始解码的位置（时间 秒数）
    public XAudioDecode seekTo(int timeSec){
        mDecodeTime = timeSec * 1000 ; // 秒转毫秒（总长度得到的时候毫秒，所以转成毫秒）
        return this;
    }

    //TODO 开始异步解码处理
    public XAudioDecode startAsync(){
        new Thread(new DecodeRunnable()).start();
        return this;
    }

    //TODO 停止解码
    public void stop(){
        setDecodeState(DECODE_STATE_STOP);
    }

    //TODO 是否资源
    public void release(){
        if(null != mMediaDecode){
            mMediaDecode.stop();
            mMediaDecode.release();
            mMediaDecode = null;
        }
        if(null != mMediaExtractor){
            mMediaExtractor.release();
            mMediaExtractor = null;
        }
        mDecodeListener = null;
    }

    //TODO 时长
    public int getAudioTime(){
        return mAudioTime;
    }

    //TODO 解码线程
    private class DecodeRunnable implements Runnable{

        @Override
        public void run() {
            JLogEx.d("Time Len = %d", mAudioTime);
            if(mDecodeTime >= (mAudioTime)){//如果跳过的时间超过总时间，则从头开始
                mDecodeTime = -1;
            }
            if(initMediaDecodec()) {
                long time = System.currentTimeMillis();
                do {
                    decodeAudioToPCM();
                }while (getDecodeState() == DECODE_STATE_DECODEING);
                JLogEx.d("Decode Use Time %d Sec", (System.currentTimeMillis() - time)/1000);
                if(getDecodeState() == DECODE_STATE_COMPLETE
                        || getDecodeState() == DECODE_STATE_STOP){
                    callBackComplete();
                }else{
                    callBackError(getDecodeState());
                }
            }else{
                callBackError(IXAudioDecodeCallBack.ERROR_ID_FILE_ERROR);
            }
        }
    }

    //TODO 设置和获取状态
    private int getDecodeState(){
        int nRet = DECODE_STATE_INIT;
        synchronized (this){
            nRet = mDecodeRelust;
        }
        return nRet;
    }

    private void setDecodeState(int state){
        synchronized (this){
            mDecodeRelust = state;
        }
    }

    //TODO 初始化解码器
    private boolean initMediaDecodec(){
        boolean bRet = false;
        try {
            mMediaExtractor = null;
            mMediaDecode = null;
            if(null != mDecodeFile) {
                mMediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
                mMediaExtractor.setDataSource(mDecodeFile);//媒体文件的位置
                int trackCount = mMediaExtractor.getTrackCount();

                JLogEx.d("trackCount=%d mDecodeTime= %d", trackCount, mDecodeTime);
                for (int i = 0; i < trackCount; i++){//查找音频轨道
                    MediaFormat format = mMediaExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    JLogEx.d(mime);
                    if (mime.startsWith("audio")) {//获取音频轨道
                        mMediaExtractor.selectTrack(i);//选择此音频轨道
                        mMediaDecode = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, KEY_SAMPLE_RATE);
                        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,0);
                        JLogEx.d("%s", format.toString());
                        mMediaDecode.configure(format,null,null, 0);
                        break;
                    }
                }
                //开启解码器并获取解码输入，输出缓存
                if(null != mMediaDecode){
                    mMediaDecode.start();//开启解码器
                    mDecodeInputBuffers = mMediaDecode.getInputBuffers();
                    mDecodeOutputBuffers = mMediaDecode.getOutputBuffers();
                    mDecodeBufferInfo = new MediaCodec.BufferInfo();
                    if(mDecodeTime > 0){//这里的seekto是微秒，mDecodeTime 存的是毫秒，所有转一下
                        mMediaExtractor.seekTo(mDecodeTime*1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                        mMediaExtractor.advance();
                    }
                    setDecodeState(DECODE_STATE_INIT);
                    JLogEx.d("IN=%d,OUT=%d", mDecodeInputBuffers.length,
                            mDecodeOutputBuffers.length);
                    bRet = true;
                }
            }else{
                callBackError(IXAudioDecodeCallBack.ERROR_ID_FILE_ERROR);
            }
        }catch (IOException e){
            e.printStackTrace();
            JLogEx.w(e.getMessage());
        }
        return bRet;
    }

    //TODO 解码音频数据
    private void decodeAudioToPCM(){
        JLogEx.i("");
        if(null !=mMediaDecode && null != mMediaExtractor){
            setDecodeState(DECODE_STATE_DECODEING);//解码中
            for (int i= 0;i< mDecodeInputBuffers.length; i++){
                int InputBuffIndex = mMediaDecode.dequeueInputBuffer(-1);
                JLogEx.i("InputBuffIndex = %d", InputBuffIndex);
                if(InputBuffIndex >=0 ){
                    ByteBuffer inputBuffer = mDecodeInputBuffers[InputBuffIndex];//拿到inputBuffer
                    inputBuffer.clear();//清空之前传入inputBuffer内的数据
                    //MediaExtractor读取数据到inputBuffer中
                    int readSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                    JLogEx.i("readSize=%d", readSize);
                    if (readSize < 0) {//小于0 代表所有数据已读取完成
                        setDecodeState(DECODE_STATE_COMPLETE);
                        mMediaDecode.queueInputBuffer(InputBuffIndex, 0, 0,
                                500, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        break;
                    }else{
                        //通知MediaDecode解码刚刚传入的数据
                        long time = mMediaExtractor.getSampleTime();
                        mMediaDecode.queueInputBuffer(InputBuffIndex,0, readSize,time,0);
                        mMediaExtractor.advance();//MediaExtractor移动到下一取样处
                    }
                }else{
                    setDecodeState(DECODE_STATE_COMPLETE);
                    break;
                }
            }

            //获取解码得到的byte[]数据 参数BufferInfo上面已介绍
            // 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
            //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
            int outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
            JLogEx.i("outputIndex=%d,Szie=%d", outputIndex, mDecodeBufferInfo.size);
            if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){//输出缓存改变，重新查询
                mDecodeOutputBuffers = mMediaDecode.getOutputBuffers();
                outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
                JLogEx.i("outputIndex=%d,Szie=%d", outputIndex, mDecodeBufferInfo.size);
            }
            if(outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){//输出的格式变化，重新查询
                MediaFormat mediaFormat = mMediaDecode.getOutputFormat();
                JLogEx.i("%s", mediaFormat.toString());
                outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
                JLogEx.i("outputIndex=%d,Szie=%d", outputIndex, mDecodeBufferInfo.size);
            }
            if(outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER){//超时，多查询一次
                outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
            }

            if(outputIndex >= 0){
                ByteBuffer outputBuffer;
                byte[] pcmData;
                JLogEx.i("flags=%d offset=%d TimeUs=%d,size=%d", mDecodeBufferInfo.flags,
                        mDecodeBufferInfo.offset,
                        mDecodeBufferInfo.presentationTimeUs,
                        mDecodeBufferInfo.size);
                //每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
                while (outputIndex >= 0){
                    outputBuffer = mDecodeOutputBuffers[outputIndex];//拿到用于存放PCM数据的Buffer
                    pcmData = new byte[mDecodeBufferInfo.size];//BufferInfo内定义了此数据块的大小
                    outputBuffer.get(pcmData);//将Buffer内的数据取出到字节数组中
                    //数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，
                    // 不清空下次会得到同样的数据
                    outputBuffer.clear();
                    callBackDecodeData(pcmData);
                    //此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
                    mMediaDecode.releaseOutputBuffer(outputIndex, false);
                    //再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
                    outputIndex = mMediaDecode.dequeueOutputBuffer(mDecodeBufferInfo, 10000);
                    JLogEx.i("outputIndex=%d,Szie=%d", outputIndex, mDecodeBufferInfo.size);
                }
            }else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                mDecodeOutputBuffers = mMediaDecode.getOutputBuffers();
            }else if(outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat mediaFormat = mMediaDecode.getOutputFormat();
                JLogEx.i("%s", mediaFormat.toString());
            }
        }else{
            setDecodeState(IXAudioDecodeCallBack.ERROR_ID_INIT_FAIL);
        }
    }

    //TODO 回调解码数据
    private void callBackDecodeData(byte[] data){
        if(null != mDecodeListener){
            mDecodeListener.onDecode(data);
        }
    }

    //TODO 回调失败
    private void callBackError(int error){
        if(null != mDecodeListener){
            mDecodeListener.onFail(error);
        }
        release();
    }

    //TODO 回调解码完成
    private void callBackComplete(){
        if(null != mDecodeListener){
            mDecodeListener.onComplete();
        }
        release();
    }

    //TODO 检查解码器
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void checkMediaDecoder(){
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos=      mediaCodecList.getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
           JLogEx.d("codecInfo = %s" , codecInfo.getName());
        }
    }

    //TODO 获取多媒体文件时长
    public static int getMediaFileTime(String file_path){
        MediaPlayer mediaPlayer = new MediaPlayer();
        int totalTime = 0;
        try {
            mediaPlayer.setDataSource(file_path);
            mediaPlayer.prepare();
            totalTime = mediaPlayer.getDuration();
            mediaPlayer.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalTime;
    }
}
