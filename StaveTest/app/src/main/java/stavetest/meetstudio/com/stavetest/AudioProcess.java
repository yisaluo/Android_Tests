package stavetest.meetstudio.com.stavetest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

/**
 * Created by wl on 2016/5/25.
 */
public class AudioProcess {

    public static final int SAMPLE_RATE = 22050;
    public static final int BUFFER_SIZE = 512; //AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    public static int bufferSize;
    private AudioRecord mAudioRecord;

    private MAudioDispatcher dispatcher;
    private OnFreqChangedListener onFreqChangedListener;

    // 音量检测
    private SilenceDetector silenceDetector;

    public AudioProcess(MAudioDispatcher.OnByteReadListener listener) {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

//        dispatcher = AudioDispatcherFactory
//                .fromDefaultMicrophone(SAMPLE_RATE, bufferSize, 0);
//        dispatcher.addAudioProcessor(new PitchProcessor(
//                PitchProcessor.PitchEstimationAlgorithm.YIN,
//                SAMPLE_RATE,
//                bufferSize,
//                new PitchDetectionHandler() {
//                    @Override
//                    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
//                        onFreqChangedListener.onFreqChanged(pitchDetectionResult.getPitch());
//                    }
//                }));

//        int var3 = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
//        int var4 = var3 / 2;
//        if (var4 <= BUFFER_SIZE) {
        this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat((float) SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, true, false);
        AndroidAudioInputStream inputSteam = new AndroidAudioInputStream(mAudioRecord, format);
        mAudioRecord.startRecording();
        dispatcher = new MAudioDispatcher(inputSteam, bufferSize, 0);
        dispatcher.setOnByteReadListener(listener);
//        } else {
//            throw new IllegalArgumentException("Buffer size too small should be at least " + var3 * 2);
//        }

        dispatcher.addAudioProcessor(new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                SAMPLE_RATE,
                bufferSize,
                new PitchDetectionHandler() {
                    @Override
                    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                        onFreqChangedListener.onFreqChanged(pitchDetectionResult.getPitch());
                    }
                }));

        dispatcher.addAudioProcessor(new BandPass(830, 770, SAMPLE_RATE));
//        silenceDetector = new SilenceDetector();
//        dispatcher.addAudioProcessor(silenceDetector);
    }

    public void start() {
        try{
            new Thread(dispatcher,"Audio Dispatcher").start();
        }catch (Error e){
            e.printStackTrace();
        }

    }

    public void stop() {
        dispatcher.stop();
    }

    public interface OnFreqChangedListener {
        void onFreqChanged(float freq);
    }

    public void setOnFreqChangedListener(OnFreqChangedListener listener) {
        this.onFreqChangedListener = listener;
    }

    /**
     * 获取当前音量值(单位分贝？)
     * @return
     */
    public double getCurrentSPL() {
        return silenceDetector.currentSPL();
    }
}
