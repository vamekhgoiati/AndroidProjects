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
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int RATE = 44100;
    private static final int IN_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int OUT_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /*
        values from [0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0]
        position      0    1    2    3    4    5    6
     */
    private static final int SEEKBAR_MAX_VALUE = 6;
    private static final int SEEKBAR_START_POS = 2;
    private static final float SEEKBAR_STEP = .5f;

    private float gain = 1.0f;

    private Button startByteBufferButton;
    private Button stopButton;
    private SeekBar gainSeekBar;

    private boolean isRecording;
    private boolean continueRec;
    private AudioTrack audioTrack;
    private AudioRecord audioRecord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startByteBufferButton = (Button) findViewById(R.id.start_byte_buffer_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        gainSeekBar = (SeekBar) findViewById(R.id.gain_seek_bar);

        gainSeekBar.setMax(SEEKBAR_MAX_VALUE);
        gainSeekBar.setProgress(SEEKBAR_START_POS);

        stopButton.setEnabled(false);

        setListeners();

        if (savedInstanceState != null) {
            Log.i(TAG, "onCreate() called, activity re-created");
        } else {
            Log.i(TAG, "onCreate() called, new activity");
        }

    }

    private void setListeners() {

        startByteBufferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    isRecording = true;
                    setButtonsEnabled(isRecording);
                    createByteBufferThread().start();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecording = false;
                setButtonsEnabled(isRecording);
                releaseAudioResources(true);
            }
        });

        gainSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gain = getSeekBarValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private float getSeekBarValue(int intValue) {
        float res;
        res = 1.0f + (intValue - SEEKBAR_START_POS) * SEEKBAR_STEP;
        return res;
    }

    private Thread createByteBufferThread() {

        return new Thread(new Runnable() {
            @Override
            public void run() {
                int bufferSize = initAudioComponents();
                startByteBuffer(bufferSize);
            }
        });
    }

    private void startByteBuffer(int bufferSize) {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            audioTrack.play();

            byte[] buffer = new byte[bufferSize];

            while (isRecording) {
                audioRecord.read(buffer, 0, bufferSize);

                if (gain != 1) {

                    for (int offset = 0; offset < bufferSize; offset += 2) {
                        double sample = ((buffer[offset] & 0xff) | (buffer[offset + 1] << 8));

                        sample = sample / 32768.0;
                        sample = sample * gain;

                        int nsample = (int) Math.round(sample * 32767.0);
                        buffer[offset] = (byte) (nsample & 0xff);
                        buffer[offset + 1] = (byte) ((nsample >> 8) & 0xff);
                    }
                }

                audioTrack.write(buffer, 0, bufferSize);
            }

        } else {
            Log.i(TAG, "Could not initialize AudioRecord object. Application will shut down");
            finish();
        }
    }

    private void setButtonsEnabled(boolean isRecording) {
        stopButton.setEnabled(isRecording);
        startByteBufferButton.setEnabled(!isRecording);
    }

    private int initAudioComponents() {

        int recordBufferSize = AudioRecord.getMinBufferSize(RATE, IN_CHANNEL, ENCODING);
        int trackBufferSize = AudioTrack.getMinBufferSize(RATE, OUT_CHANNEL, ENCODING);
        Log.i(TAG, "Got record min buffer size: " + recordBufferSize);
        Log.i(TAG, "Got track min buffer size: " + trackBufferSize);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE, IN_CHANNEL, ENCODING, recordBufferSize);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, OUT_CHANNEL, ENCODING, trackBufferSize, AudioTrack.MODE_STREAM);

        return recordBufferSize;

    }

    private void releaseAudioResources(boolean stopRecording) {

        if (stopRecording) {
            isRecording = false;
        }

        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
            audioRecord = null;
        }

        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.release();
            audioTrack = null;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() called");
        if (continueRec) {
            isRecording = true;
            createByteBufferThread().start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause() called");
        continueRec = isRecording;
        releaseAudioResources(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop() called");
        releaseAudioResources(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() called");
        releaseAudioResources(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState(...) called");
    }
}
