package stavetest.meetstudio.com.stavetest;

import android.media.AudioFormat;
import android.media.AudioRecord;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

/**
 * Created by wl on 2016/5/25.
 */
public class AudioProcess {

    private static final int SAMPLE_RATE = 22050;

    private AudioDispatcher dispatcher;
    private OnFreqChangedListener onFreqChangedListener;

    // 音量检测
    private SilenceDetector silenceDetector;

    public AudioProcess() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        dispatcher = AudioDispatcherFactory
                .fromDefaultMicrophone(SAMPLE_RATE, bufferSize, 0);
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
        silenceDetector = new SilenceDetector();
        dispatcher.addAudioProcessor(silenceDetector);
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
