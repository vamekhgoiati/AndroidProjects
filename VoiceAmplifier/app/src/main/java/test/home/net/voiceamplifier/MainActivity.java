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
    private Button startByteBufferGainButton;
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
        startByteBufferGainButton = (Button) findViewById(R.id.start_byte_buffer_gain_button);
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
        startByteBufferGainButton.setOnClickListener(listener);
        startShortBufferButton.setOnClickListener(listener);
        startShortBufferGain1Button.setOnClickListener(listener);
        startShortBufferGain2Button.setOnClickListener(listener);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecording = false;
                setButtonsEnabled(isRecording);
                releaseAudioResources();
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

    private void createBufferThreadAndStart(int id) {
        switch (id) {
            case R.id.start_byte_buffer_button:
                gain = 1.0f;
                createByteBufferThread().start();
                break;
            case R.id.start_byte_buffer_gain_button:
                gain = 2.0f;
                createByteBufferThread().start();
                break;
            case R.id.start_short_buffer_button:
                gain = 1.0f;
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

            byte[] byteBuffer = new byte[bufferSize];
            short[] buffer;
            while (isRecording) {

                int readSize = audioRecord.read(byteBuffer, 0,
                        byteBuffer.length);

                Log.i(TAG, "Size read: " + readSize);
                buffer = byteArray2ShortArray(byteBuffer);

                for (int frameIndex = 0; frameIndex < buffer.length; frameIndex++) {
                    if (gain != 1) {

                        double sample = ((double) buffer[frameIndex] / Double.MAX_VALUE);
                        sample *= gain;
                        int intValue = (int) (sample * Double.MAX_VALUE);

                        if (intValue > 32765) {
                            intValue = 32765;
                        }

                        if (intValue < -32767) {
                            intValue = -32767;
                        }

                        buffer[frameIndex] = (short) intValue;
                    }
                }

                audioTrack.write(shortArray2ByteArray(buffer), 0, buffer.length * 2);
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

    private byte[] shortArray2ByteArray(short[] shortArray) {
        byte[] byteArray = new byte[shortArray.length * 2];
        byte[] tempBuffer;
        for (int i = 0; i < shortArray.length; i++) {
            tempBuffer = short2Byte(shortArray[i]);
            byteArray[i * 2] = tempBuffer[0];
            byteArray[(i * 2) + 1] = tempBuffer[1];
        }

        return byteArray;
    }

    private short[] byteArray2ShortArray(byte[] byteArray) {
        short[] shortArray = new short[byteArray.length / 2];
        for (int i = 0; i < byteArray.length / 2; i++) {
            shortArray[i] = byte2Short(byteArray[i * 2], byteArray[(i * 2) + 1]);
        }

        return shortArray;
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

    @Override
    protected void onStop() {
        super.onStop();
        releaseAudioResources();
    }

    private void releaseAudioResources() {

        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
        }

        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAudioResources();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseAudioResources();
    }
}
