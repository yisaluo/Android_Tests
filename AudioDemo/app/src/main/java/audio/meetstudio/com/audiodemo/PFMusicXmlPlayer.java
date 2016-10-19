package audio.meetstudio.com.audiodemo;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;


/**
 * 作者：chaoyongzhang on 15/9/11 14:30
 * 邮箱：zhangcy@meet-future.com
 */
public class PFMusicXmlPlayer implements Runnable{
    private int tag;
    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }

    public interface XmlPlayerListener{
        void onProgress(float progress);
    }
    private Thread thread;
    public boolean isPlaying;
    private Activity activity;
    public XmlPlayerListener listener;

    public native void destroy();
    public native void play(String json);
    public native void stop();
    public native float getProgress();
    public native void tick(float tick);
    public native boolean isPlaying();
    public native void resetSoundSourcePathUrl(String path);
    public native void resetNotes(String json);
    public native int minMidiNote();
    public native int maxMidiNote();


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PFMusicXmlPlayer.this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tick(0.016f);
                    if (listener != null){
                        listener.onProgress(getProgress());
                    }
                }
            });
        }
    };

    public void  start(final Activity activity){
        this.activity = activity;
        if (thread != null){
            isPlaying = false;
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isPlaying = true;
        thread = new Thread(this, "XmlPlayer");
        thread.start();
    }

    public void stopThread(){
        if (!isPlaying){
            return;
        }
        stop();
        resetSoundSourcePathUrl("");
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static {
        System.loadLibrary("player");
    }

    @Override
    public void run() {
        while (isPlaying){
            try {
                Thread.sleep(16);
                handler.sendEmptyMessage(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
