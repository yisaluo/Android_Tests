package audio.meetstudio.com.audiodemo;

import android.animation.ObjectAnimator;
import android.content.res.ObbInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by ChrisDu on 2016/10/19.
 */

public class RhythmTestActivity extends AppCompatActivity {

    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 22050;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    Object mLock;

    private TextView volumeTextView = null;
    private TextView rhythmCountTextView = null;
    private TextView averageNoiseTextView = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rhythm);

        // 设置Toolbar
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 按钮
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        volumeTextView = (TextView) this.findViewById(R.id.volume);
        rhythmCountTextView = (TextView) this.findViewById(R.id.rhythm_count);
        averageNoiseTextView = (TextView) this.findViewById(R.id.average_noise);

        // 音量检测
        Toast.makeText(this, "正在检测环境噪音", Toast.LENGTH_SHORT).show();
        initVolumeDetect();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        isGetVoiceRun = false;
    }

    /**
     * 音量检测
     */
    private void initVolumeDetect() {
        mLock = new Object();

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        if (mAudioRecord == null) {
            Log.e("sound", "mAudioRecord初始化失败");
        }
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isGetVoiceRun) {
                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // 将 buffer 内容取出，进行平方和运算
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // 平方和除以数据总长度，得到音量大小。
                    double mean = v / (double) r;
                    final double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "分贝值:" + volume);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onVolumeChanged(volume);
                        }
                    });
                    // 大概一秒十次
                    synchronized (mLock) {
                        try {
                            mLock.wait(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }).start();
    }

    private int environmentNoiseCount = 0;
    private int averageNoise = 0;
    private boolean averageNoiseDone = false;
    private double preNoise = 0;
    private double prePreNoise = 0;
    private int totalRhythmCount = 0;
    private int averageNoiseCount = 100;

    private void onVolumeChanged(double volume) {
        volumeTextView.setText("分贝值:" + volume);

        // 先检测环境音
        if (!averageNoiseDone) {
            if (environmentNoiseCount < averageNoiseCount) {
                averageNoise += volume;

                environmentNoiseCount++;

                return;
            } else {
                averageNoise = averageNoise / averageNoiseCount;

                averageNoiseDone = true;
                averageNoiseTextView.setText("环境噪音平均值: " + averageNoise + "分贝");
                Toast.makeText(this, "环境音检测完毕" + averageNoise, Toast.LENGTH_SHORT).show();

                return;
            }
        }

        // 开始通过音量变化，记录节奏点
        if (preNoise - averageNoise > 10 && preNoise > prePreNoise && preNoise > volume) {
            // 记录一个节奏点
            totalRhythmCount++;
        }

        if (preNoise == 0) {
            // 第一次更新
            preNoise = volume;
        } else {
            prePreNoise = preNoise;
            preNoise = volume;
        }

        rhythmCountTextView.setText("节奏数: " + totalRhythmCount);
    }
}
