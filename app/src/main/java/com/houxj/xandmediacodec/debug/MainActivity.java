package com.houxj.xandmediacodec.debug;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.houxj.xandmediacodec.IXAudioDecodeCallBack;
import com.houxj.xandmediacodec.R;
import com.houxj.xandmediacodec.XAudioDecode;
import com.houxj.xandmediacodec.XAudioPCMPlay;
import com.houxj.xandmediacodec.utils.JLogEx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private XAudioPCMPlay mAudioPlay;
    private XAudioDecode mAudioDecode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JLogEx.setDebugEnable(true);
//        JLogEx.setInfoEnable(true);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view){
        int id = view.getId();
        if(R.id.but_decode == id){
            testDecode();
        }else if(R.id.but_check_decodec == id){
//            XAudioDecode.checkMediaDecoder();
            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath();
            file_path += File.separator + Environment.DIRECTORY_MUSIC + File.separator + "辛晓琪歌曲-m4a.pcm";
            JLogEx.d("T = %d", XAudioPCMPlay.getPcmTotalTime(file_path));
        }else if(R.id.but_play_pcm == id){
            playPCM();
        }else if(R.id.but_stop == id){
            stopDecode();
        }
    }

    private void stopDecode(){
        if(null != mAudioDecode){
            mAudioDecode.stop();
        }
    }
    private void playPCM(){
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath();
        file_path += File.separator + Environment.DIRECTORY_MUSIC + File.separator + "大壮-我们不一样_mp3.pcm";
        XAudioPCMPlay.newInstance()
                .setPCMPath(file_path)
                .play();
    }

    private void writeFile(byte[] data){
        long timg = System.currentTimeMillis();
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath();
        file_path += File.separator + Environment.DIRECTORY_MUSIC + File.separator + "大壮-我们不一样_mp3.pcm";
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file_path,true);
            outputStream.write(data);
            outputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != outputStream){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        JLogEx.d("T=%d", (System.currentTimeMillis() - timg));
    }


    private void testDecode(){
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath();
        //"几个你_薛之谦.aac";//大壮-我们不一样.mp3 //test2.wma(x) 辛晓琪歌曲.m4a test1.m4a  木叶旅途-轻音乐.wma(x)
        // 费玉清-相思比梦长.ape(x) 费玉清-热情的夏季.flac 费玉清-夏之旅.wav 电影.wmv
        file_path += File.separator + Environment.DIRECTORY_MUSIC + File.separator + "费玉清-夏之旅.wav";
        File file = new File(file_path);
        JLogEx.d("%s %s",file_path, file.exists());
        mAudioPlay = XAudioPCMPlay.newInstance().play();
        mAudioDecode = XAudioDecode.newInstance()
                .setAudioPath(file_path)
                .seekTo(30)//30秒开始解码
                .setDecodeLiListener(new IXAudioDecodeCallBack() {
                    @Override
                    public void onDecode(byte[] pcm) {
//                        writeFile(pcm);
                        mAudioPlay.write(pcm);
                    }

                    @Override
                    public void onComplete() {
                        JLogEx.d();
                        mAudioPlay.stop();
                    }

                    @Override
                    public void onFail(int error) {
                        JLogEx.d(error);
                    }
                }).startAsync();
    }

    private void savePcmFile(byte[] data){

    }
}
