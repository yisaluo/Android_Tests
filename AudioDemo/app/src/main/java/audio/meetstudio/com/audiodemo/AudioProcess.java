package audio.meetstudio.com.audiodemo;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.filters.LowPassSP;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
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
    private OnsetChangedListener onOnsetChangedListener;

    private double sensitivity = 5.0;
    private double threshold = 0.5;

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

        dispatcher.addAudioProcessor(new BandPass(2600, 2500, SAMPLE_RATE));


        // add a processor, handle percussion event.
//        dispatcher.addAudioProcessor(new PercussionOnsetDetector(SAMPLE_RATE,
//                BUFFER_SIZE, new OnsetHandler() {
//            @Override
//            public void handleOnset(double v, double v1) {
//                onOnsetChangedListener.onOnsetChanged(v, v1);
//            }
//        }, sensitivity, threshold));
    }

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
}
