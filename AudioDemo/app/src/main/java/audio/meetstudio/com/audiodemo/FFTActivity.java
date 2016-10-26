package audio.meetstudio.com.audiodemo;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import ca.uol.aig.fftpack.RealDoubleFFT;

/**
 * Created by ChrisDu on 2016/10/25.
 */

public class FFTActivity extends Activity {

    private static final int SAMPLE_RATE = 8000;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    public static int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    public static int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord;
    private RealDoubleFFT transformer;
    int blockSize;// = 256;
    boolean started = false;
    boolean CANCELLED_FLAG = false;
    private AudioTask recordTask;
    int width = 1440;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        blockSize = 256;

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfiguration, audioEncoding);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, channelConfiguration,
                audioEncoding, bufferSize);
        transformer = new RealDoubleFFT(blockSize);
        initRecord();
    }

    public void initRecord() {
        started = true;
        recordTask = new AudioTask();
        recordTask.execute();
    }

    private class AudioTask extends AsyncTask<Void, double[], Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {

            int bufferReadResult;
            short[] buffer = new short[blockSize];
            double[] toTransform = new double[blockSize];
            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                Log.e("Recording failed", e.toString());

            }
            while (started) {

                if (isCancelled() || (CANCELLED_FLAG == true)) {

                    started = false;
                    // publishProgress(cancelledResult);
                    Log.d("doInBackground", "Cancelling the RecordTask");
                    break;
                } else {
                    bufferReadResult = audioRecord.read(buffer, 0, blockSize);
                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed
                        // 16
                        // bit
                    }

                    transformer.ft(toTransform);

                    publishProgress(toTransform);
                }

            }
            return true;
        }

        @Override
        protected void onProgressUpdate(double[]... progress) {
            Log.e("RecordingProgress", "Displaying in progress");

            Log.d("Test:", Integer.toString(progress[0].length));


            if (width > 512) {
                for (int i = 0; i < progress[0].length; i++) {
                    int x = 2 * i;
                    int downy = (int) (150 - (progress[0][i] * 10));
                    int upy = 150;
                }
            }
        }
    }
}
