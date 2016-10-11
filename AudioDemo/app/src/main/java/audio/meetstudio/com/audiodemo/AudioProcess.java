package audio.meetstudio.com.audiodemo;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

/**
 * Created by ChrisDu on 2016/10/8.
 */

public class AudioProcess {

    private static final int SAMPLE_RATE = 22050;
    private static final int BUFFER_SIZE = 1024;

    private AudioDispatcher dispatcher;
    private OnFreqChangedListener onFreqChangedListener;

    public AudioProcess() {
        dispatcher = AudioDispatcherFactory
                .fromDefaultMicrophone(SAMPLE_RATE, BUFFER_SIZE, 0);
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
        void onFreqChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent);
    }

    public void setOnFreqChangedListener(OnFreqChangedListener listener) {
        this.onFreqChangedListener = listener;
    }
}
