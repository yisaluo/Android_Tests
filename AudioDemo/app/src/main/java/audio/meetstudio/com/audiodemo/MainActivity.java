package audio.meetstudio.com.audiodemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.icu.util.Measure;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends AppCompatActivity implements UtterProcess.UtterProcessListener, NoteLoader.NoteLoaderListener, PFMusicXmlPlayer.XmlPlayerListener, AudioProcess.OnFreqChangedListener, AudioProcess.OnsetChangedListener {

    private final static String TAG = "AudioDemo";

    private AudioProcess audioProcess;

    private TextView mTextView = null;
    private TextView noteNameTextView = null;
    private TextView targetTextView = null;

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

        if (hasRecordingPermission()) {
            showToast("已获取录音权限");

//            startAudioDetect();
        } else {
            showToast("未获取录音权限");
        }

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

    @Override
    public void onPitchChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        audioEvent.getEndTimeStamp();
        float pitch = pitchDetectionResult.getPitch();

        if (pitch > -1) {
            final String msg = "pitch = " + pitch + "\ntimeStamp = " + timeStamp + "\nendTimeStamp = " + audioEvent.getEndTimeStamp();
            final String noteName = NoteMap.caculateNoteName(pitch);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(msg);
                    noteNameTextView.setText(noteName);
                }
            });
        }
    }

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
                    NoteBean noteBean = new NoteBean(note.getKeys().get(0), start, duration);

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
        int a = 5;

        isRecording = false;
        task.cancel();
        mTimer.cancel();
        totalTime = 0;

        // 计算结果
        int totalCount = inputArray.size();
        int targetIndex = 0;
        int rightCount = 0;
        for (int i = 0; i < inputArray.size(); i++) {
            InputNote inputNote = inputArray.get(i);
            NoteBean targetNote = noteArray.get(targetIndex);

            if (inputNote.time >= targetNote.start && inputNote.time <= targetNote.start + targetNote.duration) {
                if (inputNote.noteName.equalsIgnoreCase(targetNote.noteName)) {
                    rightCount++;
                }
            } else {
                i--;
                targetIndex++;
                if (targetIndex >= noteArray.size()) {
                    break;
                }
            }
        }

        float rate = (float)rightCount / (float)totalCount * 100;
        targetTextView.setText("计算结果: " + rate + "%");
    }

    public void clear(View view) {
        noteIndex = 0;
        inputArray.clear();
        startAudioDetect();
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

        if (pitch > -2) {
            final String msg = "pitch = " + pitch + "\ntimeStamp = " + timeStamp + "\nendTimeStamp = " + audioEvent.getEndTimeStamp();
            final String noteName = NoteMap.caculateNoteName(pitch);
            Log.i("Pitch", msg);
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

                inputArray.add(note);
            }
        }
    }

    private void tick(long time) {
        if (noteIndex >= noteArray.size()) {
            caculate(null);

            return;
        }

        NoteBean noteBean = noteArray.get(noteIndex);

        if (time <= noteBean.start + noteBean.duration) {
            targetTextView.setText("请弹这个音：" + noteBean.noteName);
        } else {
            noteIndex++;
        }
    }

    private void analyzeData() {

    }

    @Override
    public void onOnsetChanged(double v1, double v2) {
        Log.i("onOnsetChanged", "v1 = " + v1 + ", v2 = " + v2);
    }
}
