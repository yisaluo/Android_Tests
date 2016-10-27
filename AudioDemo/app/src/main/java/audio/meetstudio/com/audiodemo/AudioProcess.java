package audio.meetstudio.com.audiodemo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaDataSource;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.filters.LowPassSP;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.util.fft.FFT;

/**
 * Created by ChrisDu on 2016/10/8.
 */

public class AudioProcess {

    private static final int SAMPLE_RATE = 22050;
    public static final int BUFFER_SIZE = 1024; //AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    public MAudioDispatcher dispatcher;
    private OnFreqChangedListener onFreqChangedListener;
    private OnsetChangedListener onOnsetChangedListener;
    private AudioRecord mAudioRecord;

    private double sensitivity = 5.0;
    private double threshold = 0.5;

    SilenceDetector silenceDetector;

    public AudioProcess(OnByteReadListener listener) {

//        dispatcher = AudioDispatcherFactory
//                .fromDefaultMicrophone(SAMPLE_RATE, BUFFER_SIZE, 0);
        int var3 = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int var4 = var3 / 2;
        if (var4 <= BUFFER_SIZE) {
            this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
            TarsosDSPAudioFormat format = new TarsosDSPAudioFormat((float) SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, true, false);
            AndroidAudioInputStream inputSteam = new AndroidAudioInputStream(mAudioRecord, format);
            mAudioRecord.startRecording();
            dispatcher = new MAudioDispatcher(inputSteam, BUFFER_SIZE, 0);
            dispatcher.setOnByteReadListener(listener);
        } else {
            throw new IllegalArgumentException("Buffer size too small should be at least " + var3 * 2);
        }

        dispatcher.addAudioProcessor(new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                SAMPLE_RATE,
                BUFFER_SIZE,
                new PitchDetectionHandler() {
                    @Override
                    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                        onFreqChangedListener.onFreqChanged(pitchDetectionResult, audioEvent);
                    }
                }));

        // 添加带通滤波
         dispatcher.addAudioProcessor(new BandPass(2600, 2400, SAMPLE_RATE));

        // 添加FFT变换
         dispatcher.addAudioProcessor(fftProcessor);

        // 声音检测
        silenceDetector = new SilenceDetector(-80, false);
        dispatcher.addAudioProcessor(silenceDetector);

        // add a processor, handle percussion event.
//        dispatcher.addAudioProcessor(new PercussionOnsetDetector(SAMPLE_RATE,
//                BUFFER_SIZE, new OnsetHandler() {
//            @Override
//            public void handleOnset(double v, double v1) {
//                onOnsetChangedListener.onOnsetChanged(v, v1);
//            }
//        }, sensitivity, threshold));
    }

    AudioProcessor fftProcessor = new AudioProcessor(){

        FFT fft = new FFT(BUFFER_SIZE);
        float[] amplitudes = new float[BUFFER_SIZE / 2];

        @Override
        public void processingFinished() {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean process(AudioEvent audioEvent) {
//            Log.i("process", "Begin" + System.currentTimeMillis());
            float[] audioFloatBuffer = audioEvent.getFloatBuffer();
            float[] transformbuffer = new float[BUFFER_SIZE * 2];
            System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);
            fft.forwardTransform(transformbuffer);
            fft.modulus(transformbuffer, amplitudes);
//            panel.drawFFT(pitch, amplitudes,fft);
//            panel.repaint();

//            Log.i("process", "End" + System.currentTimeMillis());

            return true;
        }

    };

    public void start() {
        try {
            new Thread(dispatcher, "Audio Dispatcher").start();
        } catch (Error e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        dispatcher.stop();
    }

    public interface OnFreqChangedListener {
        void onFreqChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent);
    }

    public void setOnFreqChangedListener(OnFreqChangedListener listener) {
        this.onFreqChangedListener = listener;
    }

    public interface OnsetChangedListener {
        void onOnsetChanged(double v1, double v2);
    }

    public void setOnsetChangedListener(OnsetChangedListener listener) {
        this.onOnsetChangedListener = listener;
    }

    public AudioRecord getAudioRecord() {
        return mAudioRecord;
    }

    public double getCurrentdBSPL() {
        if (silenceDetector != null) {
            return silenceDetector.currentSPL();
        }

        return 0;
    }
}
