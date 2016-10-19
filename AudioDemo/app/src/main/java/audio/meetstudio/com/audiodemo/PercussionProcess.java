package audio.meetstudio.com.audiodemo;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

/**
 * Created by ChrisDu on 2016/10/12.
 */

public class PercussionProcess {

    private static final int SAMPLE_RATE = 22050;
    private static final int BUFFER_SIZE = 1024;

    private AudioDispatcher dispatcher;


    private double sensitivity = 10.0;
    private double threshold = 1.0;

    public PercussionProcess() {
        dispatcher = AudioDispatcherFactory
                .fromDefaultMicrophone(SAMPLE_RATE, BUFFER_SIZE, 0);


    }

    public void start() {
        try{
            new Thread(dispatcher,"PercussionProcess").start();
        }catch (Error e){
            e.printStackTrace();
        }
    }

    public void stop() {
        dispatcher.stop();
    }


}
