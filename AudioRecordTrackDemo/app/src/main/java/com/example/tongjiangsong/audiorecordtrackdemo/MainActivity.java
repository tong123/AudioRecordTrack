package com.example.tongjiangsong.audiorecordtrackdemo;

import android.os.Bundle;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity  implements View.OnClickListener {
    PipedInputStream in;
    boolean isRecord;
    MAudioRecord m_audio_record ;
    MAudioPlayer m_audio_player;
    Button record_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isRecord = false;
        record_btn = (Button)findViewById(R.id.button);
        record_btn.setOnClickListener(this);
    }

    private void startRecord(){
        in = new PipedInputStream();
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    m_audio_record = new MAudioRecord(MainActivity.this, in);
                    m_audio_record.StartAudioData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                PipedOutputStream pout = new PipedOutputStream();
                m_audio_player = new MAudioPlayer();
                try {
                    m_audio_player.setOutputStream(pout);
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            m_audio_player.startPlayAudio();
                        }
                    }).start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                int size = 0 ;
                try {
                    while (true){
                        while (in.available()>0){
                            size = in.read(buffer);
                            pout.write(buffer, 0, size);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        if (isRecord){
            isRecord = false;
            m_audio_record.stopRecord();
            m_audio_player.stopPlay();
        }else{
            isRecord = true;
            startRecord();
        }
    }
}

