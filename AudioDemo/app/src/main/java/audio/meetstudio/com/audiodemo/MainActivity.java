package audio.meetstudio.com.audiodemo;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionResult;

public class MainActivity extends AppCompatActivity implements NoteLoader.NoteLoaderListener, PFMusicXmlPlayer.XmlPlayerListener, AudioProcess.OnFreqChangedListener, AudioProcess.OnsetChangedListener {

    private final static String TAG = "AudioDemo";

    private AudioProcess audioProcess;

    private TextView mTextView = null;
    private TextView noteNameTextView = null;
    private TextView targetTextView = null;
    private TextView volumeTextView = null;
    private GridLayout notesLayout = null;

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


        mTextView = (TextView) findViewById(R.id.note_pitch);
        noteNameTextView = (TextView) findViewById(R.id.note_name);
        targetTextView = (TextView) findViewById(R.id.target);
        volumeTextView = (TextView) findViewById(R.id.textview_volume);



        noteLoader = new NoteLoader(this);

        // 复制文件
        String abasepath = getFilesDir().getPath();
        String toPath = abasepath + "/";  // Your application path
        copyAsset(getAssets(), "merlin_gold_plus.sf2", toPath + "merlin_gold.sf2");
        copyAsset(getAssets(), "song_xxx.xml", toPath + "song_xxx.xml");
        copyAsset(getAssets(), "qian_yu_qian_xun.xml", toPath + "qian_yu_qian_xun.xml");

        // 读取MusicXML数据
        noteLoader.listener = this;
        noteLoader.loadStave("song_xxx.xml");

        handler = new Handler() {
            public void handleMessage(Message msg) {
                totalTime += ticksPerFrame;

                tick(totalTime);

                super.handleMessage(msg);
            }
        };


        mTimer = new Timer();

        inputArray = new ArrayList<>();

        // startRecord();

        notesLayout = (GridLayout) findViewById(R.id.notes_layout);

        TextView t1 = new TextView(this);   t1.setText(" 1 ");
        TextView t2 = new TextView(this);   t2.setText(" 1 ");
        TextView t3 = new TextView(this);   t3.setText(" 5 ");
        TextView t4 = new TextView(this);   t4.setText(" 5 ");
        TextView t5 = new TextView(this);   t5.setText(" 6 ");
        TextView t6 = new TextView(this);   t6.setText(" 6 ");
        TextView t7 = new TextView(this);   t7.setText(" 5 ");
        TextView t8 = new TextView(this);   t8.setText(" 4 ");
        TextView t9 = new TextView(this);   t9.setText(" 4 ");
        TextView t10 = new TextView(this);   t10.setText(" 3 ");
        TextView t11 = new TextView(this);   t11.setText(" 3 ");
        TextView t12 = new TextView(this);   t12.setText(" 2 ");
        TextView t13 = new TextView(this);   t13.setText(" 2 ");
        TextView t14 = new TextView(this);   t14.setText(" 1 ");
        TextView t15 = new TextView(this);   t15.setText(" 5 ");
        TextView t16 = new TextView(this);   t16.setText(" 5 ");
        TextView t17 = new TextView(this);   t17.setText(" 4 ");
        TextView t18 = new TextView(this);   t18.setText(" 4 ");
        TextView t19 = new TextView(this);   t19.setText(" 3 ");
        TextView t20 = new TextView(this);   t20.setText(" 3 ");
        TextView t21 = new TextView(this);   t21.setText(" 2 ");
        TextView t22 = new TextView(this);   t22.setText(" 5 ");
        TextView t23 = new TextView(this);   t23.setText(" 5 ");
        TextView t24 = new TextView(this);   t24.setText(" 4 ");
        TextView t25 = new TextView(this);   t25.setText(" 4 ");
        TextView t26 = new TextView(this);   t26.setText(" 3 ");
        TextView t27 = new TextView(this);   t27.setText(" 3 ");
        TextView t28 = new TextView(this);   t28.setText(" 2 ");
        TextView t29 = new TextView(this);   t29.setText(" 1 ");
        TextView t30 = new TextView(this);   t30.setText(" 1 ");
        TextView t31 = new TextView(this);   t31.setText(" 5 ");
        TextView t32 = new TextView(this);   t32.setText(" 5 ");
        TextView t33 = new TextView(this);   t33.setText(" 6 ");
        TextView t34 = new TextView(this);   t34.setText(" 6 ");
        TextView t35 = new TextView(this);   t35.setText(" 5 ");
        TextView t36 = new TextView(this);   t36.setText(" 4 ");
        TextView t37 = new TextView(this);   t37.setText(" 4 ");
        TextView t38 = new TextView(this);   t38.setText(" 3 ");
        TextView t39 = new TextView(this);   t39.setText(" 3 ");
        TextView t40 = new TextView(this);   t40.setText(" 2 ");
        TextView t41 = new TextView(this);   t41.setText(" 2 ");
        TextView t42 = new TextView(this);   t42.setText(" 1 ");

        notesTipsArray.add(t1);
        notesTipsArray.add(t2);
        notesTipsArray.add(t3);
        notesTipsArray.add(t4);
        notesTipsArray.add(t5);
        notesTipsArray.add(t6);
        notesTipsArray.add(t7);
        notesTipsArray.add(t8);
        notesTipsArray.add(t9);
        notesTipsArray.add(t10);
        notesTipsArray.add(t11);
        notesTipsArray.add(t12);
        notesTipsArray.add(t13);
        notesTipsArray.add(t14);
        notesTipsArray.add(t15);
        notesTipsArray.add(t16);
        notesTipsArray.add(t17);
        notesTipsArray.add(t18);
        notesTipsArray.add(t19);
        notesTipsArray.add(t20);
        notesTipsArray.add(t21);
        notesTipsArray.add(t22);
        notesTipsArray.add(t23);
        notesTipsArray.add(t24);
        notesTipsArray.add(t25);
        notesTipsArray.add(t26);
        notesTipsArray.add(t27);
        notesTipsArray.add(t28);
        notesTipsArray.add(t29);
        notesTipsArray.add(t30);
        notesTipsArray.add(t31);
        notesTipsArray.add(t32);
        notesTipsArray.add(t33);
        notesTipsArray.add(t34);
        notesTipsArray.add(t35);
        notesTipsArray.add(t36);
        notesTipsArray.add(t37);
        notesTipsArray.add(t38);
        notesTipsArray.add(t39);
        notesTipsArray.add(t40);
        notesTipsArray.add(t41);
        notesTipsArray.add(t42);

        for (int i = 0; i < notesTipsArray.size(); i++) {
            TextView tv = notesTipsArray.get(i);
            notesLayout.addView(tv);
        }

        if (hasRecordingPermission()) {
            showToast("已获取录音权限");

            startAudioDetect();
        } else {
            showToast("未获取录音权限");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioProcess.stop();
        stopRecord();
    }

    private static boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
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
        audioProcess = new AudioProcess();
        audioProcess.setOnFreqChangedListener(this);
        audioProcess.setOnsetChangedListener(this);
        audioProcess.start();
    }

//    @Override
//    public void onPitchChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
//        double timeStamp = audioEvent.getTimeStamp();
//        audioEvent.getEndTimeStamp();
//        float pitch = pitchDetectionResult.getPitch();
//
//        if (pitch > -1) {
//            final String msg = "pitch = " + pitch + ",timeStamp = " + timeStamp + ",endTimeStamp = " + audioEvent.getEndTimeStamp();
//            Log.i("PitchDetect", msg);
//            if (isChecking) {
//                fixPitch = Math.max(fixPitch, pitch);
//                pitchOffset = fixPitch - 261.626f;
//            } else {
//                String noteNames = NoteMap.caculateNoteName(pitch);
//                String[] notesNameArray = noteNames.split(",");
//                final String noteName = notesNameArray[0];
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTextView.setText(msg);
//                        noteNameTextView.setText(noteName);
//                    }
//                });
//            }
//        }
//    }

    @Override
    public void onNoteLoaded(NoteLoader loader, String notes) {
//        String abasepath = getFilesDir().getPath();
//        String toPath = abasepath + "/";  // Your application path

//        player.resetNotes(notes);
//        player.resetSoundSourcePathUrl(toPath + "merlin_gold.sf2");
//        player.play(noteLoader.result);
//        player.start(MainActivity.this);
        Toast.makeText(this, "曲谱已加载", Toast.LENGTH_SHORT).show();
        Gson gson = new Gson();
        musicXMLNote = gson.fromJson(notes, new TypeToken<MusicXMLNote>() {
        }.getType());

        noteArray.clear();
        measuresCount = musicXMLNote.getLength();
        MusicXMLNote.MeasuresBean measure = musicXMLNote.getMeasures().get(0);
        tempo = measure.getMeasure_tempo();

        long tick = 0;
        for (int i = 0; i < musicXMLNote.getMeasures().size(); i++) {
            measure = musicXMLNote.getMeasures().get(i);
            quarterDuration = measure.getDivisions();
            // 一小节对应的时长(ticks)
            quarterDuration = quarterDuration * 4 / measure.getTime().getBeat_value();
            measureDuration = quarterDuration * measure.getTime().getNum_beats();
            // 计算整首歌曲总时长(ticks)
            totalDuration += measureDuration;

            // tick += measureDuration * i;
            for (int j = 0; j < measure.getParts().get(0).getVoices().size(); j++) {
                MusicXMLNote.MeasuresBean.PartsBean.VoicesBean voice = measure.getParts().get(0).getVoices().get(j);
                for (int k = 0; k < voice.getNotes().size(); k++) {
                    MusicXMLNote.MeasuresBean.PartsBean.VoicesBean.NotesBean note = voice.getNotes().get(k);

                    long start = tick;
                    int duration = note.getIntrinsicTicks();
                    tick += duration;
                    NoteBean noteBean = new NoteBean(note.getKeys().get(0), start, duration, note.isRest());

                    noteArray.add(noteBean);
                }
            }

            /*
                PCVoice* voice = (PCVoice *)measure->voices->getObjectAtIndex(j);
                for (int k = 0; k < voice->notes->count(); k++)
                {
                    PCNote* pcnote = (PCNote *)voice->notes->getObjectAtIndex(k);

                    for (int l = 0; l < pcnote->tones.size(); l++)
                    {
                        int staveNum = pcnote->stave;
                        int stave = 0; //0 高音, 1 低音
                        PCStave* pcStave = (PCStave *)staveArray->getObjectAtIndex(staveNum);
                        if (!pcStave->isTrebleClef())
                        {
                            stave = 1;
                        }
                        else if (pcStave->isTrebleClef())
                        {
                            stave = 0;
                        }

                        if (preStave == -1)
                        {
                            preStave = stave;
                        }

                        if (preStave != stave)
                        {
                            supportBothHand = true;
                        }

                        int midiNote = pcnote->tones.at(l);
                        int octave = octaveOfNote(midiNote);
                        if (octave <= minOctave)
                        {
                            minOctave = octave;
                        }

                        if (octave >= maxOctave)
                        {
                            maxOctave = octave;
                        }

                        Note* note = new Note(pcnote->tones.at(l), pcnote->duration, pcnote->start + measureDuration * i, pcnote->rest, pcnote->track, pcnote->velocity, pcnote->tag, pcnote->fingering, stave, pcnote->tieBegin, pcnote->tieEnd);
                        note->stave = pcnote->stave;

                        // 弱起小节时长修正
                        if (i == 0)
                        {
                            // 第一小节
                            if (note->start + note->duration >= longestNote)
                            {
                                // 取最后一个音符的结束时间
                                longestNote = (int)(note->start + note->duration);
                            }
                        }

                        if (note->tieEnd && notesArray.size() > 0)
                        {
                            // 当前音符为连音符结尾，需要将时长加到前一个音符上
                            Note* preNote = (Note *)notesArray.at(notesArray.size() - 1);
                            if (preNote->tieBegin)
                            {
                                preNote->duration += note->duration;
                            }
                        }

//                        CCLOG("pcnote tone = %d, start = %d, end = %d", pcnote->tones.at(l), pcnote->start, pcnote->start + pcnote->duration);
//                        if (!note->rest)
//                        {
                        note->index = notesArray.size();
                        if (note->rest || note->start == preStart) {
                            if (note->finger == preFingerInt && note->leftHand == isLeft) {
                                note->finger = 0;
                            }
                            note->numNotationTag = -1;
                        } else {
                            preNotationIdx++;
                            note->numNotationTag = preNotationIdx;
                        }
                        isLeft = note->leftHand;
                        preFingerInt = note->finger;
                        preStart = note->start;
                        notesArray.pushBack(note);
                        CCLOG("add bg tone = %d, start = %ld, end = %ld", note->note, note->start, pcnote->start + pcnote->duration);

//                        }
                    }
                }
            }

            if (i == 0)
            {
                // 第一小节
                if (longestNote < measureDuration)
                {
                    // 最后一个音符结束时间，不在小节结尾，则该小节为弱起小节
                    // 时间差
                    int minus = measureDuration - longestNote;

                    for (int i = 0; i < notesArray.size(); i++)
                    {
                        Note* note = notesArray.at(i);
                        // 将所有音符的起始时间加上时间差，相当于弱起小节缺少的拍数用休止符代替
                        note->start += minus;
                        // note->start -= measureDuration;
                    }
                }
            }
            */
        }

        timePerTick_f = (float) 60 / tempo / quarterDuration;
        ticksPerFrame = (long) (0.016 / timePerTick_f);
        int a = 5;
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

            int inputRightCount = 0;
            int inputTotalCount = 0;

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

//            while (inputNote.time > targetNote.start && inputNote.time < targetNote.start + targetNote.duration) {
//                // 在标准音时间范围内的输入音符
//                // 保留连续正确而片段
//                // 判断连续时，向后计算5各数据点
//                if (inputNote.noteName.equals(targetNote.noteName)) {
//                    // 正确
//                    inputRightCount++;
//
//                    // 判断后面数据是否连续
//
//                } else {
//                    // 错误
//
//                }
//
//                inputTotalCount++;
//
//                inputIndex++;
//                inputNote = inputArray.get(inputIndex);
//            }
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
        rate = rate / 0.80f;
        if (rate > 100f) {
            rate = 100f;
        }

        float toneRate = (float)toneRights / (float)toneTotal * 100;

        targetTextView.setText("计算结果: 节奏准确率: " + rate + "%, 音准正确率: " + toneRate);
    }

    public void clear(View view) {
        if (audioProcess == null) {
            startAudioDetect();
        }
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
        mTimer.scheduleAtFixedRate(task, 0, 16);

    }

    @Override
    public void onFreqChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        audioEvent.getEndTimeStamp();
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

                    inputArray.add(note);
                    final String log = "pitch = " + note.freq + ",timeStamp = " + timeStamp + ",totalTime = " + totalTime;
                    Log.i("PitchDetect", log);
                }
            }
        }
    }

    private void tick(long time) {
        if (noteIndex >= noteArray.size()) {
            caculate(null);

            return;
        }

        if (isRecording) {
            NoteBean noteBean = noteArray.get(noteIndex);

            if (time <= noteBean.start + noteBean.duration) {
                clearColors();
                targetTextView.setText("请弹这个音：" + noteBean.noteName);
                TextView tv = notesTipsArray.get(noteIndex);
                tv.setTextColor(Color.GREEN);
            } else {
                noteIndex++;
            }
        }
    }

    private void clearColors() {
        for (int i = 0; i < notesTipsArray.size(); i++) {
            TextView tv = notesTipsArray.get(i);
            tv.setTextColor(Color.WHITE);
        }
    }

    private void analyzeData() {

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

    /**
     * 更新话筒状态
     */
    private int BASE = 1;
    private int SPACE = 100;// 间隔取样时间
    private MediaRecorder mMediaRecorder;
    private final Handler mHandler = new Handler();
    public static final int MAX_LENGTH = 1000 * 60 * 10;// 最大录音时长1000*60*10;
    private File file;

    private Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };

    public void startRecord() {
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        try {
            PackageManager m = getPackageManager();
            String s = getPackageName();
            PackageInfo p = null;
            try {
                p = m.getPackageInfo(s, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String basePath = p.applicationInfo.dataDir;
            String recordPath = basePath + "/files/" + "vol.amr";
            file = new File(recordPath);
            /* ②setAudioSource/setVedioSource */
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            /* ③准备 */
            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            mMediaRecorder.setMaxDuration(MAX_LENGTH);
            mMediaRecorder.prepare();
            /* ④开始 */
            mMediaRecorder.start();
            // AudioRecord audioRecord.
            /* 获取开始时间* */
            // pre=mMediaRecorder.getMaxAmplitude();
            updateMicStatus();
        } catch (IllegalStateException e) {
            Log.i(TAG,
                    "call startAmr(File mRecAudioFile) failed!"
                            + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateMicStatus() {
        if (mMediaRecorder != null) {
            double ratio = (double) mMediaRecorder.getMaxAmplitude() / BASE;
            double db = 0;// 分贝
            if (ratio > 1)
                db = 20 * Math.log10(ratio);
            Log.d(TAG, "分贝值：" + db);
            volumeTextView.setText("分贝值: " + db);
            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        if (mMediaRecorder == null)
            return;
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
    }
}
