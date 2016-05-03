package test.home.net.voiceamplifier;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int RATE = 44100;
    private static final int IN_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int OUT_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private byte[] buffer;

    private Button startButton;
    private Button stopButton;
    private boolean isRecording;
    private AudioTrack audioTrack;
    private AudioRecord audioRecord;
    private int recordBufferSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "Got min buffer size: " + recordBufferSize);

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        stopButton.setEnabled(false);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    isRecording = true;
                    stopButton.setEnabled(isRecording);
                    startButton.setEnabled(!isRecording);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            recordBufferSize = AudioRecord.getMinBufferSize(RATE, IN_CHANNEL, ENCODING);
                            buffer = new byte[recordBufferSize];

                            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE, IN_CHANNEL, ENCODING, recordBufferSize);
                            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, OUT_CHANNEL, ENCODING, recordBufferSize, AudioTrack.MODE_STREAM);

                            routeSound();
                        }
                    }).start();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecording = false;
                stopButton.setEnabled(isRecording);
                startButton.setEnabled(!isRecording);
                audioRecord.release();
            }
        });

    }

    private void routeSound() {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            audioTrack.play();

            while (isRecording) {
                audioRecord.read(buffer, 0, recordBufferSize);
                audioTrack.write(buffer, 0, recordBufferSize);
            }
        } else {
            Log.i(TAG, "Could not initialize AudioRecord object");
            finish();
        }
    }
}
