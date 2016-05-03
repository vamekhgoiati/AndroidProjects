package test.home.net.voiceamplifier;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final int BUFFER_SIZE_IN_BYTES = 2048;
    private static final int BUFFER_SIZE_IN_SHORT = 1024;

    private static final int RATE = 44100;
    private static final int IN_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int OUT_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private short[] buffer;

    private Button startButton;
    private Button stopButton;
    private boolean isRecording;
    private AudioTrack audioTrack;
    private AudioRecord audioRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buffer = new short[BUFFER_SIZE_IN_SHORT];

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE, IN_CHANNEL, ENCODING, BUFFER_SIZE_IN_BYTES);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, OUT_CHANNEL, ENCODING, BUFFER_SIZE_IN_BYTES, AudioTrack.MODE_STREAM);

        audioRecord.startRecording();
        audioTrack.play();

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    isRecording = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (isRecording) {
                                audioRecord.read(buffer, 0, BUFFER_SIZE_IN_SHORT);
                                audioTrack.write(buffer, 0, BUFFER_SIZE_IN_SHORT);
                            }
                        }
                    }).start();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecording = false;
            }
        });

    }
}
