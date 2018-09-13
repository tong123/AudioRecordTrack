package com.example.tongjiangsong.audiorecordtrackdemo;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import static android.content.ContentValues.TAG;

/*
 * To getaudio or play audio
 * */
public class MAudioRecord {
    private AudioRecord audioRecord;
    private Context context;
    private boolean isRecording = false ;
    private int buffersize = 0;
    private PipedOutputStream outstream ;//利用管道传输数据
    public MAudioRecord(Context context , PipedInputStream instream) throws IOException {
        this.context  = context;
        //初始化管道流 用于向外传输数据
        outstream = new PipedOutputStream();
        outstream.connect(instream);
    }
    public void StartAudioData(){//得到录音数据
        int frequency = 11025;
        int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        buffersize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, buffersize);
        byte[] buffer  = new byte[1024];
        audioRecord.startRecording();//开始录音
        isRecording = true;
        int bufferReadSize = 1024;
        String tmpName = System.currentTimeMillis()+"_"+44100+"";
        final File tmpFile = createFile(tmpName+".pcm");
        final File tmpOutFile = createFile(tmpName+".wav");
        try {
            FileOutputStream outputStream = new FileOutputStream(tmpFile.getAbsoluteFile());

            while (isRecording){
                int n_size = audioRecord.read(buffer, 0, 1024);
                Log.w("audiotest", "StartAudioData: ------------------------"+n_size);

                try {
                    //outstream.write(buffer, 0, 1024);
                    outputStream.write(buffer);
                    //outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outputStream.close();
            pcmToWave(tmpFile.getAbsolutePath(), tmpOutFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File createFile(String name) {
        String dirPath = Environment.getExternalStorageDirectory().getPath()+"/AudioRecord/";
        File file = new File(dirPath);
        if(!file.exists()) {
            file.mkdirs();
        }
        String filePath = dirPath +name;
        File objFile = new File(filePath);
        if (!objFile.exists()) {
            try {
                objFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return objFile;
        }
        return null;
    }

    public void stopRecord(){//停止录音
        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
        try {
            outstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pcmToWave(String inFileName, String outFileName){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudiolen = 0;
        long longSamplRate = 11025;
        long totalDataLen = totalAudiolen+36;
        int channels = 2;
        long byteRate = 16*longSamplRate*channels/8;
        byte[] data = new byte[1024];
        try {
            in = new FileInputStream(inFileName);

            out = new FileOutputStream(outFileName);
            totalAudiolen = in.getChannel().size();
            totalDataLen = totalAudiolen+36;
            writeWaveFileHeader(out, totalAudiolen, totalDataLen, longSamplRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                    int channels, long byteRate) {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        try {
            out.write(header, 0, 44);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}