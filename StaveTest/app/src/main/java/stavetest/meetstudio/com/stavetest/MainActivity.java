package stavetest.meetstudio.com.stavetest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity implements NoteLoader.NoteLoaderListener, View.OnClickListener, AudioProcess.OnFreqChangedListener, MAudioDispatcher.OnByteReadListener, MyTimer.MyTimerListener {

    private String testData = "";

    private ScrollView mScrollView = null;
    private LinearLayout mLinearLayout = null;

    private int sliceIndex = 0;
    private HashMap<Integer, TrainingView> trainingViewHashMap = new HashMap<>();

    private AudioProcess audioProcess;

    private int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1;

    private ArrayList<MusicXMLNoteParser.Note> notesArray = new ArrayList();

    private MyTimer myTimer;

    @Override
    public void onTimerUpdated(float totalTime, float dt) {

    }

    @Override
    public void onTimerMax() {
        this.onRecordButton(null);
    }

    private class PitchData {
        public float pitch;
        public float time;
        public int note;
    }

    private ArrayList<PitchData> pitchDatas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        copyFile("opensheetdisplay.html");
        copyFile("vexflow-min.js");
        copyFile("vexflow_view.js");

        mScrollView = (ScrollView)findViewById(R.id.scrollview);
        mLinearLayout = (LinearLayout)findViewById(R.id.staves_layout);

        for (int i = 0; i < 8; i++)
        {
            TrainingView view = (TrainingView)LayoutInflater.from(this).inflate(R.layout.item_training, null);
            view.initViews();

            trainingViewHashMap.put(i, view);

            String url = String.format("file:///android_asset/octave.html?filename=training_%d.xml", i);
            view.webView.loadUrl(url);

            mLinearLayout.addView(view);

            Integer integer = i;
            view.cover.setTag(integer);
            view.cover.setOnClickListener(this);

            if (sliceIndex == i)
            {
                view.cover.setVisibility(View.GONE);
                view.controlLayout.setVisibility(View.VISIBLE);
            }

            // Button click listener
            Button instrumentButton = (Button)view.findViewById(R.id.standard_instrument);
            Button voiceButton = (Button)view.findViewById(R.id.standard_voice);
            Button scoreAndReplayButton = (Button)view.findViewById(R.id.standard_stop_and_score);
            Button recordButton = (Button) view.findViewById(R.id.record);

            instrumentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onInstrumentButton(view);
                }
            });

            voiceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onVoiceButton(view);
                }
            });

            scoreAndReplayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onScoreAndReplayButton(view);
                }
            });

            recordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onRecordButton(view);
                }
            });
        }


        NoteLoader noteLoader = new NoteLoader(this);
        noteLoader.listener = this;
        noteLoader.loadStave("training_7.xml");

        // 检查权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            //申请 RECORD_AUDIO 权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE);
        }

        myTimer = new MyTimer(this, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {

            startShiyin();
            Toast.makeText(getApplicationContext(), "授权成功", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), "授权拒绝", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();

            file = new File(fileName);
        }

        try {
            int bytesum = 0;
            int byteread = 0;
            InputStream inStream = this.getAssets().open(fileName); //读入原文件
            FileOutputStream fs = this.openFileOutput(fileName, Context.MODE_PRIVATE);
            byte[] buffer = new byte[1024];
            int length;
            while ((byteread = inStream.read(buffer)) != -1) {
                bytesum += byteread; //字节数 文件大小
                fs.write(buffer, 0, byteread);
            }
            inStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (audioProcess == null) {
                startShiyin();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopShiyin();
    }

    /**
     * 开始音频识别
     */
    private void startShiyin() {
        audioProcess = new AudioProcess(this);
        audioProcess.setOnFreqChangedListener(this);
        audioProcess.start();
    }

    /**
     * 停止音频识别
     */
    private void stopShiyin() {
        if (audioProcess != null) {
            audioProcess.stop();
            audioProcess = null;
        }
    }

    @Override
    public void onNoteLoaded(NoteLoader loader, String notesStr) {
        Log.i("onNoteLoaded", "onNoteLoaded result=" + notesStr);
        // 解析曲谱数据
        notesArray.clear();
        pitchDatas.clear();
        notesArray = MusicXMLNoteParser.getInstance().parseJsonData(notesStr);
        Log.i("onNoteLoaded", "notesArray.size() = " + notesArray.size());

        float duration = MusicXMLNoteParser.getInstance().getTotalDuration();
        Log.i("onNoteLoaded", "duration = " + duration);

        myTimer.setMaxTime(duration);
    }

    @Override
    public void onClick(View view) {
        int index = (Integer)view.getTag();
        Log.i("onClick", "onClick -> " + index);

        trainingViewHashMap.get(sliceIndex).deactive();
        sliceIndex = index;
        trainingViewHashMap.get(sliceIndex).active();
    }

    @Override
    public void onFreqChanged(float freq) {
        // Log.i("onFreqChanged", "freq = " + freq);
        if (RecordFileManager.getInstance(this).isRecording) {
            String noteNames = NoteMap.caculateNoteName(freq);
            String[] notesNameArray = noteNames.split(",");
            String noteName = TextUtils.isEmpty(noteNames) ? "" : notesNameArray[0];
            Log.i("onFreqChanged", "noteName = " + noteName);
            int note = 0;
            if (!TextUtils.isEmpty(noteName)) {
                note = NoteConverter.toneFromKey(noteName, noteName.substring(1, 2));
            }

            // 录音状态时，进行应该检测对比
            PitchData data = new PitchData();
            data.time = myTimer.getCurrentTickTimer();
            data.pitch = freq;
            data.note = note;

            pitchDatas.add(data);
        }
    }

    @Override
    public void onByteRead(int length, byte[] audioByteBuffer) {
        // 读取到字节数据，保存到录音文件中
        RecordFileManager.getInstance(this).writeRecordFileData(audioByteBuffer);
    }

    public class TrainingHolder {
        String fileName;
        WebView webView;
        Button instrumentButton;
        Button voiceButton;
        Button recordButton;
        Button stopAndScoreButton;
        RelativeLayout controlLayout;
        boolean loaded = false;
    }

    private HashMap<Integer, WebView> webViews = new HashMap<>();

    public class TrainingAdapter extends BaseAdapter {

        private Context mContext;
        private LayoutInflater mInflater;

        public TrainingAdapter(Context context) {
            this.mContext = context;
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return 8;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            TrainingHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_training, null);

                holder = new TrainingHolder();
                holder.webView = (WebView) convertView.findViewById(R.id.webview);
                holder.instrumentButton = (Button) convertView.findViewById(R.id.standard_instrument);
                holder.voiceButton = (Button) convertView.findViewById(R.id.standard_voice);
                holder.recordButton = (Button) convertView.findViewById(R.id.standard_stop_and_score);
                holder.stopAndScoreButton = (Button) convertView.findViewById(R.id.record);
                holder.controlLayout = (RelativeLayout) convertView.findViewById(R.id.control_buttons);

                holder.webView.getSettings().setJavaScriptEnabled(true);
                holder.webView.getSettings().setAllowFileAccess(true);
                holder.webView.getSettings().setAllowContentAccess(true);
                holder.webView.getSettings().setUseWideViewPort(true);
                holder.webView.getSettings().setLoadWithOverviewMode(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.webView.getSettings().setAllowFileAccessFromFileURLs(true);
                    holder.webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
                }

                webViews.put(position, holder.webView);

                convertView.setTag(holder);
            } else {
                holder = (TrainingHolder)convertView.getTag();
            }

            String url = String.format("file:///android_asset/octave.html?filename=training_%d.xml", position);
            holder.webView.loadUrl(url);

            return convertView;
        }
    }

    // 按钮事件

    /**
     * 乐器示范
     * @param view
     */
    public void onInstrumentButton(View view) {

    }

    /**
     * 人声示范
     * @param view
     */
    public void onVoiceButton(View view) {

    }

    /**
     * 分数、回放
     * @param view
     */
    public void onScoreAndReplayButton(View view) {
        if (RecordFileManager.getInstance(this).isPlaying) {
            RecordFileManager.getInstance(this).stopPlayRecordFile();
        } else {
            RecordFileManager.getInstance(this).playRecordFile();
        }
    }

    /**
     * 录音按钮
     * @param view
     */
    public void onRecordButton(View view) {
        Log.i("onRecordButton", "onRecordButton");
        if (RecordFileManager.getInstance(this).isRecording) {
            RecordFileManager.getInstance(this).finishWriteRecordFileData();

            myTimer.stopTimer();

            // 评分
            caculateScore();
        } else {
            String fileName = String.format("slice_%d", sliceIndex);
            RecordFileManager.getInstance(this).prepareRecordFile(fileName);

            myTimer.startTimer();
        }
    }

    /**
     * 计算分数
     */
    public void caculateScore() {
        // 遍历音高数据数组
        int targetIndex = 0;
        for (int i = 0; i < pitchDatas.size(); i++) {
            PitchData data = pitchDatas.get(i);
            MusicXMLNoteParser.Note targetNote = notesArray.get(targetIndex);
            if (!targetNote.rest && data.time >= targetNote.startTime && data.time <= targetNote.startTime + targetNote.durationTime) {
                if (data.note == targetNote.midiNote) {
                    Log.i("caculateScore", "right");
                    Toast.makeText(this, "对了", Toast.LENGTH_LONG).show();
                } else {
                    if (data.note < targetNote.midiNote) {
                        Toast.makeText(this, "低了" + (targetNote.midiNote - data.note) + "个半音", Toast.LENGTH_LONG).show();
                    } else if (data.note > targetNote.midiNote) {
                        Toast.makeText(this, "高了了" + (data.note - targetNote.midiNote) + "个半音", Toast.LENGTH_LONG).show();
                    }
                }
            } else {

            }
        }
    }
}
