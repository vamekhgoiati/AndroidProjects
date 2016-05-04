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
    private static final int BITS_PER_SAMPLE = 16;
    private static final int BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8;
    private static final int EMPTY_SPACE = 64 - BITS_PER_SAMPLE;


    private float gain = 1.0f;

    private Button startByteBufferButton;
    private Button startShortBufferButton;
    private Button startShortBufferGain1Button;
    private Button startShortBufferGain2Button;
    private Button stopButton;
    private boolean isRecording;
    private AudioTrack audioTrack;
    private AudioRecord audioRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startByteBufferButton = (Button) findViewById(R.id.start_byte_buffer_button);
        startShortBufferButton = (Button) findViewById(R.id.start_short_buffer_button);
        startShortBufferGain1Button = (Button) findViewById(R.id.start_short_buffer_gain1_button);
        startShortBufferGain2Button = (Button) findViewById(R.id.start_short_buffer_gain2_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        stopButton.setEnabled(false);

        setListeners();

    }

    private void setListeners() {

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    isRecording = true;
                    setButtonsEnabled(isRecording);
                    createBufferThreadAndStart(v.getId());
                }
            }
        };

        startByteBufferButton.setOnClickListener(listener);
        startShortBufferButton.setOnClickListener(listener);
        startShortBufferGain1Button.setOnClickListener(listener);
        startShortBufferGain2Button.setOnClickListener(listener);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecording = false;
                setButtonsEnabled(isRecording);
                audioRecord.release();
                audioTrack.release();
            }
        });

    }

    private Thread createByteBufferThread() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int bufferSize = initAudioComponents();
                startByteBuffer(bufferSize);
            }
        });

        return t;
    }

    private void startByteBuffer(int bufferSize) {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            audioTrack.play();

            byte[] buffer = new byte[bufferSize];

            while (isRecording) {
                audioRecord.read(buffer, 0, bufferSize);
                audioTrack.write(buffer, 0, bufferSize);
            }

        } else {
            Log.i(TAG, "Could not initialize AudioRecord object. Application will shut down");
            finish();
        }
    }

    private void createBufferThreadAndStart(int id) {
        switch (id) {
            case R.id.start_byte_buffer_button:
                createByteBufferThread().start();
                break;
            case R.id.start_short_buffer_button:
                createShortBufferThread().start();
                break;
            case R.id.start_short_buffer_gain1_button:
                gain = 2.0f;
                createShortBufferThread().start();
                break;
            case R.id.start_short_buffer_gain2_button:
                gain = 4.0f;
                createShortBufferThread().start();
                break;
        }
    }

    private Thread createShortBufferThread() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int bufferSize = initAudioComponents();
                startShortBuffer(bufferSize);
            }
        });

        return t;
    }

    private void startShortBuffer(int bufferSize) {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            audioTrack.play();

            short[] buffer = new short[bufferSize];
            while (isRecording) {
                int readSize = audioRecord.read(buffer, 0,
                        buffer.length);

                int byteIndex = 0;
                int byteIndex2 = 0;

                for (int frameIndex = 0; frameIndex < readSize; frameIndex++) {

                    for (int c = 0; c < 1; c++) {

                        if (gain != 1) {

                            long accumulator = 0;
                            for (int b = 0; b < BYTES_PER_SAMPLE; b++) {
                                accumulator += ((long) (buffer[byteIndex++] & 0xff)) << (b * 8 + EMPTY_SPACE);
                            }

                            double sample = ((double) accumulator / (double) Long.MAX_VALUE);
                            sample *= gain;
                            int intValue = (int) (sample * (double) Integer.MAX_VALUE);

                            for (int i = 0; i < BYTES_PER_SAMPLE; i++) {
                                buffer[i + byteIndex2] = (byte) (intValue >>> ((i + 2) * 8) & 0xff);
                            }
                            byteIndex2 += BYTES_PER_SAMPLE;

                        }
                    }

                    if (buffer[frameIndex] > 32765) {
                        buffer[frameIndex] = 32767;

                    } else if (buffer[frameIndex] < -32767) {
                        buffer[frameIndex] = -32767;
                    }


                }

                audioTrack.write(buffer, 0, buffer.length);
            }

        } else {
            Log.i(TAG, "Could not initialize AudioRecord object. Application will shut down");
            finish();
        }
    }

    private void setButtonsEnabled(boolean isRecording) {
        stopButton.setEnabled(isRecording);
        startByteBufferButton.setEnabled(!isRecording);
        startShortBufferButton.setEnabled(!isRecording);
        startShortBufferGain1Button.setEnabled(!isRecording);
        startShortBufferGain2Button.setEnabled(!isRecording);
    }

    private short byte2Short(byte b1, byte b2) {
        return (short) ((b1 & 0xff) | (b2 << 8));
    }

    private byte[] short2Byte(short s) {
        byte[] res = new byte[2];
        res[0] = (byte) (s & 0xff);
        res[1] = (byte) ((s >> 8) & 0xff);

        return res;
    }

    private int initAudioComponents() {

        int recordBufferSize = AudioRecord.getMinBufferSize(RATE, IN_CHANNEL, ENCODING);
        Log.i(TAG, "Got min buffer size: " + recordBufferSize);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE, IN_CHANNEL, ENCODING, recordBufferSize);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, OUT_CHANNEL, ENCODING, recordBufferSize, AudioTrack.MODE_STREAM);

        return recordBufferSize;
    }
}
