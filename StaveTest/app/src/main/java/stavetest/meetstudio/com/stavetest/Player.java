package stavetest.meetstudio.com.stavetest;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.TimerTask;

public class Player implements OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnErrorListener {
    /**
     * state*
     */
    public static final int STATE_STOP = 0;
    public static final int STATE_PREPARE = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_PAUSE = 3;
    protected static final int STATE_DOWNLOADED = 10;

    public MediaPlayer mediaPlayer;
    private OnPlayListener mListener = null;

    public boolean prepared = false;
    public boolean autoStart = true;

    public Player() {

        try {
            initMediaPlayer();
        } catch (Exception e) {
            Log.e("mediaPlayer", "error", e);
        }
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    public void setOnPlayListener(OnPlayListener listener) {
        this.mListener = listener;
    }

    /**
     * ****************************************************
     * 通过定时器和Handler来更新进度条
     * ****************************************************
     */
    TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            if (mediaPlayer == null)
                return;
            /*if (mediaPlayer.isPlaying() && controlBar.getSeekBar().isPressed() == false) {
                handleProgress.sendEmptyMessage(0);
            } */
        }
    };

    public interface OnPlayListener {
        void onPrepared(int duration);

        void onEnded();

        void onUpdate(int position);
    }

    //*****************************************************
    public void play() {
        mediaPlayer.start();

        if (mListener != null) {
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void playLocalFile(String filePath) {
        prepared = false;
        autoStart = true;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
        initMediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAssetsFile(String fileName) {
        autoStart = true;
        AssetFileDescriptor fileDescriptor;
        try {
            fileDescriptor = MainActivity.m_instance.getAssets().openFd(fileName);

            initMediaPlayer();

            mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());

            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }


    /**
     * 通过onPrepared播放
     */
    @Override
    public void onPrepared(MediaPlayer arg0) {
        prepared = true;

        if (mListener != null) {
            mListener.onPrepared(mediaPlayer.getDuration());
        }

        if (autoStart)
            play();
    }


    public void onBufferingUpdate(MediaPlayer arg0, int bufferingProgress) {
//        controlBar.getSeekBar().setSecondaryProgress(bufferingProgress);
//        int currentProgress=controlBar.getSeekBar().getMax()*mediaPlayer.getCurrentPosition()/mediaPlayer.getDuration();
//        Log.e(currentProgress+"% play", bufferingProgress + "% buffer");
    }


    public void onCompletion(MediaPlayer arg0) {

    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public void down_file(String url) throws IOException {
        //下载函数
        URL myURL = new URL(url);
        URLConnection conn = myURL.openConnection();
        conn.connect();
        InputStream is = conn.getInputStream();
        if (is == null) throw new RuntimeException("stream is null");
        //把文件存到path
        String path = "/sdcard/temp";
        OutputStream os = new FileOutputStream(path);
        byte buf[] = new byte[1024];
        do {
            int numread = is.read(buf);
            if (is.read(buf) == -1) {
                break;
            }
            os.write(buf, 0, numread);
        } while (true);
        is.close();
        os.close();
    }
}
