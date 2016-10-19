package audio.meetstudio.com.audiodemo;

import android.util.Log;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;


/**
 * Created by ChrisDu on 2016/10/8.
 */

public class UtterProcess implements PitchDetectionHandler{

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 7168;

    private AudioDispatcher dispatcher;
    private UtterProcessListener mListener = null;

    public UtterProcess() {
        PitchProcessor.PitchEstimationAlgorithm algo = PitchProcessor.PitchEstimationAlgorithm.valueOf("YIN");

        dispatcher = AudioDispatcherFactory
                .fromDefaultMicrophone(SAMPLE_RATE, BUFFER_SIZE, 0);

        dispatcher.addAudioProcessor(new PitchProcessor(algo, SAMPLE_RATE, BUFFER_SIZE, this));
    }

    public void start() {
        new Thread(dispatcher,"Audio dispatching").start();
    }

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        if (mListener != null) {
            mListener.onPitchChanged(pitchDetectionResult, audioEvent);
        }
    }

    public interface UtterProcessListener {
        public void onPitchChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent);
    }

    public void setUtterProcessListener(UtterProcessListener listener) {
        this.mListener = listener;
    }
}
