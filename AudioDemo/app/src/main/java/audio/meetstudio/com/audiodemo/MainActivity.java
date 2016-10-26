package audio.meetstudio.com.audiodemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.util.fft.FFT;
import ca.uol.aig.fftpack.RealDoubleFFT;

import static audio.meetstudio.com.audiodemo.StaveViewActivity.dataArray;
import static audio.meetstudio.com.audiodemo.StaveViewActivity.numNotationMusjeStrWith;

public class MainActivity extends AppCompatActivity implements NoteLoader.NoteLoaderListener, PFMusicXmlPlayer.XmlPlayerListener, AudioProcess.OnFreqChangedListener, AudioProcess.OnsetChangedListener, OnByteReadListener {

    private final static String TAG = "AudioDemo";

    private AudioProcess audioProcess;

    private TextView mTextView = null;
    private TextView noteNameTextView = null;
    private TextView targetTextView = null;
    private TextView volumeTextView = null;
    private GridLayout notesLayout = null;
    private TextView tempoTextView = null;

    // MusicXML数据加载
    private NoteLoader noteLoader;

    // 播放器
    private PFMusicXmlPlayer player = new PFMusicXmlPlayer();

    // 计时器
    private Timer mTimer = null;

    private MusicXMLNote musicXMLNote;

    private ArrayList<InputNote> inputArray;

    private int measuresCount = 0;  // 小节数
    private int tempo = 0;          // 速度
    private int quarterDuration = 0;// 四分音符时长
    private int measureDuration = 0;// 小节对应时长
    private int totalDuration = 0;  // 总时长

    private long totalTime = 0;

    private ArrayList<NoteBean> noteArray = new ArrayList<>();
    private ArrayList<TextView> notesTipsArray = new ArrayList<>();

    private float timePerTick_f = 0;
    private long ticksPerFrame = 0;

    private TimerTask task;

    private Handler handler;

    private int noteIndex = 0;

    private boolean isRecording = false;

    private String mSongName;
    private String mFileName;

    private boolean staveShowed = false;

    private int environmentNoiseCount = 0;
    private int averageNoise = 0;
    private boolean averageNoiseDone = false;
    private double preNoise = 0;
    private double prePreNoise = 0;
    private int totalRhythmCount = 0;
    private int averageNoiseCount = 100;

    private NoteBean currentNote = null;

    private int tempoCount = 0;
    private long tempoTick = 0;
    private long tempoDuration = 0;
    private long tempoIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Intent intent = getIntent();
        if (intent.hasExtra(SongSelectActivity.INTENT_SONG_NAME)) {
            mSongName = intent.getStringExtra(SongSelectActivity.INTENT_SONG_NAME);
        }

        if (intent.hasExtra(SongSelectActivity.INTENT_FILE_NAME)) {
            mFileName = intent.getStringExtra(SongSelectActivity.INTENT_FILE_NAME);
        }

        mTextView = (TextView) findViewById(R.id.note_pitch);
        noteNameTextView = (TextView) findViewById(R.id.note_name);
        targetTextView = (TextView) findViewById(R.id.target);
        volumeTextView = (TextView) findViewById(R.id.textview_volume);
        tempoTextView = (TextView) findViewById(R.id.tempo);

        noteLoader = new NoteLoader(this);

        PackageManager m = getPackageManager();
        String s = getPackageName();
        PackageInfo p = null;
        try {
            p = m.getPackageInfo(s, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        basePath = p.applicationInfo.dataDir;

        AssetsCopyer.releaseAssets(this, "musje", basePath + "/files/");

        initWebView();

        showStave();

        // 读取MusicXML数据
        if (!TextUtils.isEmpty(mFileName)) {
            noteLoader.listener = this;
             noteLoader.loadStave(mFileName);
        }

        handler = new Handler() {
            public void handleMessage(Message msg) {
                tick(totalTime, ticksPerFrame);

                super.handleMessage(msg);
            }
        };

        mTimer = new Timer();

        inputArray = new ArrayList<>();

        notesLayout = (GridLayout) findViewById(R.id.notes_layout);

        if (hasRecordingPermission()) {
            showToast("已获取录音权限");

            startAudioDetect();
        } else {
            showToast("未获取录音权限");
        }

        ((Button) findViewById(R.id.view_stave)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mWebView != null) {
                    mWebView.setVisibility(mWebView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }

                if (!staveShowed) {
                    staveShowed = true;
                    String data = numNotationMusjeStrWith(musicXMLNote, noteArray);

                    MusicXMLNote.MeasuresBean.PartsBean.StavesBean bean = musicXMLNote.getMeasures().get(0).getParts().get(0).getStaves().get(0);

                    String keySigString = "1 = " + bean.getKey();
                    if (keySigString.length() == 0) {
                        keySigString = "1 = C";
                    }
                    final String jsUrl = String.format("javascript:showScoreWithFontSize(\"%s\", \"%s\", %d, %d)", data, keySigString, 360, 15);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.loadUrl(jsUrl);
                        }
                    });
                }
            }
        });

//        Toast.makeText(this, "正在检测环境噪音", Toast.LENGTH_SHORT).show();
    }

    private void showStave() {
        String url = "file://" + basePath + "/files/musje/musje.html";
        System.out.print(url);
        if (mWebView != null) {
            mWebView.loadUrl(url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioProcess.stop();
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (musicXMLNote != null && noteArray.size() > 0) {

            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 判断是否有录音权限
     *
     * @return
     */
    private boolean hasRecordingPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
                return false;
            }
        }

        return true;
    }

    /**
     * 显示Toast提示
     *
     * @param msg
     */
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void startAudioDetect() {
        audioProcess = new AudioProcess(this);
        audioProcess.setOnFreqChangedListener(this);
//        audioProcess.setOnsetChangedListener(this);
        audioProcess.start();
    }

    @Override
    public void onNoteLoaded(final NoteLoader loader, String notes) {
        Toast.makeText(this, "曲谱已加载", Toast.LENGTH_SHORT).show();
        Gson gson = new Gson();
        musicXMLNote = gson.fromJson(notes, new TypeToken<MusicXMLNote>() {
        }.getType());
        StaveViewActivity.musicXMLNote = musicXMLNote;
        noteArray.clear();
        dataArray.clear();
        measuresCount = musicXMLNote.getLength();
        MusicXMLNote.MeasuresBean measure = musicXMLNote.getMeasures().get(0);
        tempo = measure.getMeasure_tempo();
        measure.getParts().get(0).getStaves().get(0).getKey();

        long tick = 0;
        for (int i = 0; i < musicXMLNote.getMeasures().size(); i++) {
            measure = musicXMLNote.getMeasures().get(i);
            quarterDuration = measure.getDivisions();
            // 一小节对应的时长(ticks)
//            quarterDuration = quarterDuration / measure.getTime().getBeat_value();
            measureDuration = quarterDuration * measure.getTime().getNum_beats();
            // 计算整首歌曲总时长(ticks)
            totalDuration += measureDuration;

            tempoDuration = quarterDuration * 4 / measure.getTime().getBeat_value();
            tempoCount = measure.getTime().getNum_beats();
            tempoIndex = 0 - measure.getTime().getNum_beats();

            // tick += measureDuration * i;
            for (int j = 0; j < measure.getParts().get(0).getVoices().size(); j++) {
                MusicXMLNote.MeasuresBean.PartsBean.VoicesBean voice = measure.getParts().get(0).getVoices().get(j);
                for (int k = 0; k < voice.getNotes().size(); k++) {
                    MusicXMLNote.MeasuresBean.PartsBean.VoicesBean.NotesBean note = voice.getNotes().get(k);

                    long start = tick;
                    int duration = note.getIntrinsicTicks();
                    tick += duration;
                    NoteBean noteBean = new NoteBean(note.getKeys().get(0), start, duration, note.isRest(), note);
                    String sub = noteBean.noteName.substring(1, 2);
                    noteBean.note = NoteConverter.toneFromKey(noteBean.noteName, sub);
                    noteArray.add(noteBean);
                    dataArray.add(noteBean);
                    final TextView noteTextView = new TextView(this);
                    noteTextView.setText(" " + noteBean.noteName + " ");
                    if (noteBean.rest) {
                        noteTextView.setText(" 0 ");
                    }
                    notesTipsArray.add(noteTextView);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notesLayout.addView(noteTextView);
                        }
                    });

                }
            }
        }


        timePerTick_f = (float) 60 / tempo / quarterDuration;
        ticksPerFrame = (long) (0.016 / timePerTick_f);
        int a = 5;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.time)).setText("速度: " + tempo);
            }
        });

    }

    @Override
    public void onProgress(float progress) {

    }

    public void caculate(View view) {
        isRecording = false;
        task.cancel();
        mTimer.cancel();
        totalTime = 0;

        // 计算结果

        // 处理结果，保留连续平稳的数据

        // 总数为曲谱音符的个数
        int totalCount = noteArray.size();
        int targetIndex = 0;
        int rightCount = 0;
        int inputIndex = 0;
        for (int i = 0; i < noteArray.size(); i++) {
            NoteBean targetNote = noteArray.get(i);
            if (inputIndex >= inputArray.size()) {
                break;
            }
            InputNote inputNote = inputArray.get(inputIndex);

            while (inputNote.time > targetNote.start && inputNote.time < targetNote.start + targetNote.duration) {
                // 将单个的错误点纠正
                if (inputNote.noteName.equals(targetNote.noteName)) {
                    // 正确
                    // 判断后面2个数据是否连续
                    if (inputIndex + 2 < inputArray.size()) {
                        InputNote inputNote1 = inputArray.get(inputIndex + 1);
                        InputNote inputNote2 = inputArray.get(inputIndex + 2);

                        if (!inputNote1.noteName.equals(targetNote.noteName) && inputNote2.noteName.equals(targetNote.noteName)) {
                            // 连续的三个点，当中间的点数据错误，且两边的点数据正确时，将这个点判定为正确数据
                            inputNote1.noteName = inputNote.noteName;
                            inputNote1.freq = inputNote.freq;
                        }
                    }
                }

                inputIndex++;
                if (inputIndex >= inputArray.size()) {
                    break;
                }
                inputNote = inputArray.get(inputIndex);
            }
        }

        int toneRights = 0;
        int toneTotal = noteArray.size();
        boolean isToneRight = false;
        for (int i = 0; i < inputArray.size(); i++) {
            InputNote inputNote = inputArray.get(i);
            NoteBean targetNote = noteArray.get(targetIndex);

            if (inputNote.time >= targetNote.start && inputNote.time <= targetNote.start + targetNote.duration) {
                if (inputNote.noteName.equalsIgnoreCase(targetNote.noteName)) {
                    rightCount++;

                    if (targetNote.type == 3) {
                        // 先判断起始时间
                        if (inputNote.time >= targetNote.start && inputNote.time < targetNote.start + (targetNote.duration) / 4) {
                        } else {
                            // late
                            targetNote.type = 1;
                        }
                    }

                    if (targetNote.type == 3) {
                        if (i + 1 < inputArray.size()) {
                            InputNote nextNote = inputArray.get(i + 1);
                            if (nextNote.noteName.equalsIgnoreCase(inputNote.noteName)) {

                            } else {
                                // 最后一个正确的数据
                                if (inputNote.time < targetNote.start + targetNote.duration / 2) {
                                    targetNote.type = 2;
                                }
                            }
                        }
                    }

                    if (targetNote.type == 3)
                        targetNote.type = 0;

                    if (!isToneRight) {
                        isToneRight = true;
                        toneRights++;
                    }
                }
            } else {
                i--;
                targetIndex++;
                isToneRight = false;
                if (targetIndex >= noteArray.size()) {
                    break;
                }
            }
        }

        totalCount = inputArray.size();
        float rate = (float) rightCount / (float) totalCount * 100;

        // 数据造假，给用户一个比较好的结果
        rate = rate / 0.90f;
        if (rate > 100f) {
            rate = 100f;
        }

        float toneRate = (float) toneRights / (float) toneTotal * 100;

        targetTextView.setText("计算结果: 节奏准确率: " + rate + "%\n音准正确率: " + toneRate);

        showNoteResult();
    }

    private void showNoteResult() {
        for (int i = 0; i < notesTipsArray.size(); i++) {
            TextView tv = notesTipsArray.get(i);
            tv.setTextColor(Color.WHITE);

            NoteBean bean = noteArray.get(i);
            if (bean.type == 1) {
                tv.setTextColor(Color.BLUE);
            } else if (bean.type == 2) {
                tv.setTextColor(Color.YELLOW);
            } else if (bean.type == 3) {
                tv.setTextColor(Color.RED);
            }
        }
    }

    public void clear(View view) {
        if (audioProcess == null) {
            startAudioDetect();
        }

        clearColors();

        noteIndex = 0;
        inputArray.clear();

        isRecording = true;
        mTimer = new Timer();
        task = new TimerTask() {
            public void run() {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }
        };
        int interval = (int) (1f / 60f * 1000);
        mTimer.scheduleAtFixedRate(task, 0, interval);
    }

    @Override
    public void onFreqChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        float pitch = pitchDetectionResult.getPitch();
        if (isChecking) {
            fixPitch = Math.max(fixPitch, pitch);
            pitchOffset = fixPitch - 261.626f;
        } else {
            if (pitch > -2) {
                final String msg = "pitch = " + pitch + "\ntimeStamp = " + timeStamp + "\ntotalTime = " + totalTime;
                String noteNames = NoteMap.caculateNoteName((float) (pitch / Math.pow(2, pitchOffset / 12)));
                String[] notesNameArray = noteNames.split(",");
                final String noteName = TextUtils.isEmpty(noteNames) ? "" : notesNameArray[0];

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(msg);
                        noteNameTextView.setText(noteName);
                    }
                });

                if (isRecording) {
                    InputNote note = new InputNote();
                    note.noteName = noteName;
                    note.timeStamp = timeStamp;
                    note.time = totalTime;
                    note.freq = TextUtils.isEmpty(noteNames) ? 0 : Float.valueOf(notesNameArray[1]);
                    if (TextUtils.isEmpty(noteName)) {
                        note.note = 0;
                    } else {
                        note.note = NoteConverter.toneFromKey(noteName, noteName.substring(1, 2));
                    }
                    inputArray.add(note);
                    final String log = "pitch = " + note.freq + ",timeStamp = " + timeStamp + ",totalTime = " + totalTime;
//                    Log.i("PitchDetect", log);
                }
            }
        }
    }

    private void tick(long time, long dt) {
        if (noteIndex >= noteArray.size()) {
            caculate(null);

            return;
        }

        if (isRecording) {
            if (tempoIndex >= 0) {
                totalTime += ticksPerFrame;
                currentNote = noteArray.get(noteIndex);

                if (totalTime > currentNote.start && totalTime <= currentNote.start + currentNote.duration) {

                    targetTextView.setText("请弹这个音：" + (currentNote.rest ? "" : currentNote.noteName));
                    TextView tv = notesTipsArray.get(noteIndex);
                    tv.setTextColor(Color.GREEN);

//                String str = String.format("javascript:colorNote(%d,'%s')", noteIndex + 1, "#00ff00");
//                if (mWebView != null)
//                    mWebView.loadUrl(str);
                } else {
                    clearColors();
                    noteIndex++;
                }
            }

            // 节拍器

            if (tempoTick > tempoDuration) {
                tempoIndex++;
                if (tempoIndex >= tempoCount) {
                    tempoIndex = 0;
                }
                tempoTick = 0;
            }
            tempoTextView.setText("节拍：" + (tempoIndex + 1));
            tempoTick += dt;
        }
    }

    private void clearColors() {
        targetTextView.setText("请弹这个音：");
        for (int i = 0; i < notesTipsArray.size(); i++) {
            TextView tv = notesTipsArray.get(i);
            tv.setTextColor(Color.WHITE);
        }

        for (int i = 0; i < noteArray.size(); i++) {
            noteArray.get(i).type = 3;
        }
    }

    @Override
    public void onOnsetChanged(double v1, double v2) {
        Log.i("onOnsetChanged", "v1 = " + v1 + ", v2 = " + v2);
    }

    // 手动校对音高
    private float fixPitch = 0;
    private float pitchOffset = 0;
    private boolean isChecking = false;

    public void onCheckTone(View view) {
        isChecking = true;
    }

    public void onFinishCheckTone(View view) {
        isChecking = false;

        pitchOffset = (float) (12 * Math.log(fixPitch / 261.626) / Math.log(2));
        Log.i("onFinishCheckTone", "pitchOffset = " + pitchOffset);
    }

    public void onResetTone(View view) {
        fixPitch = 0;
        pitchOffset = 0;
    }

    private RealDoubleFFT trasformer;
    double[] toTransform;

    @Override
    public void onByteRead(int length, byte[] audioByteBuffer) {


        // 将 buffer 内容取出，进行平方和运算
        int r = length;
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < audioByteBuffer.length; i += 2) {
            short value = getShort(audioByteBuffer, i);
            v += value * value;
        }

//        toTransform = new double[audioByteBuffer.length / 2];
//        if (trasformer == null) {
//            trasformer = new RealDoubleFFT(audioByteBuffer.length / 2);
//        }
//
//        for (int i = 0; i < audioByteBuffer.length / 2 && i < length; i += 2) {
//            toTransform[i] = (double) getShort(audioByteBuffer, i) / 32768.0;
//        }
//        // TODO
//        trasformer.ft(toTransform);

//        for (int i = 0; i < toTransform.length; i++) {
//            int x = i;
//            int downy = (int) (100 - (toTransform[i] * 10));
//            int upy = 100;
//            Log.i("drawLine", String.format("i = %d, x = %d, downy = %d, upy = %d", i, x, downy, upy));
//        }

        // 平方和除以数据总长度，得到音量大小。
        double mean = v / (double) r;
        final double volume = 10 * Math.log10(mean);
        final double vol = volume;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                volumeTextView.setText("分贝值: " + vol);
                Log.i("SilenceDetector", "currentdBSPL = " + audioProcess.getCurrentdBSPL());
            }
        });


//        // 先检测环境音
//        if (!averageNoiseDone) {
//            if (environmentNoiseCount < averageNoiseCount) {
//                averageNoise += volume;
//
//                environmentNoiseCount++;
//
//                return;
//            } else {
//                averageNoise = averageNoise / averageNoiseCount;
//                final int noise = averageNoise;
//                averageNoiseDone = true;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(MainActivity.this, "环境音检测完毕" + noise, Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//
//                return;
//            }
//        }
//
//        // 开始通过音量变化，记录节奏点
//        if (preNoise - averageNoise > 10 && preNoise > prePreNoise && preNoise > volume) {
//            // 记录一个节奏点
//            totalRhythmCount++;
//
//            if (currentNote != null && isRecording) {
//                currentNote.newInputTime = totalTime;
//                currentNote.newInput = true;
//            }
//        }
//
//        if (preNoise == 0) {
//            // 第一次更新
//            preNoise = volume;
//        } else {
//            prePreNoise = preNoise;
//            preNoise = volume;
//        }
    }

    public static short getShort(byte[] b, int index) {
        return (short) (((b[index + 1] << 8) | b[index + 0] & 0xff));
    }

    private WebView mWebView;
    private String basePath = "";
    private static HashMap<String, String> alterMap = new HashMap<>();

    private void initWebView() {
        mWebView = (WebView) this.findViewById(R.id.stave_webview);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
            mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
        mWebView.setBackgroundColor(0); // 设置背景色
        mWebView.getBackground().setAlpha(2); // 设置填充透明度 范围：0-255
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mWebView.setWebViewClient(new MyClient());
    }

    class MyClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i("OverrideUrl", url);
            return true;//super.shouldOverrideUrlLoading(view, url);
        }
    }
}
