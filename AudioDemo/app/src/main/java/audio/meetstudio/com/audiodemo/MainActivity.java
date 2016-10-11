package audio.meetstudio.com.audiodemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionResult;

public class MainActivity extends AppCompatActivity implements UtterProcess.UtterProcessListener, NoteLoader.NoteLoaderListener, PFMusicXmlPlayer.XmlPlayerListener, AudioProcess.OnFreqChangedListener {

    private final static String TAG = "AudioDemo";

    private AudioProcess audioProcess;

    private TextView mTextView = null;
    private TextView noteNameTextView = null;

    // MusicXML数据加载
    private NoteLoader noteLoader;

    // 播放器
    private PFMusicXmlPlayer player = new PFMusicXmlPlayer();

    // 计时器
    private Timer mTimer = null;

    private MusicXMLNote musicXMLNote;

    private String preNoteName = "";
    private ArrayList<InputNote> inputArray;

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

        if (hasRecordingPermission()) {
            showToast("已获取录音权限");

            startAudioDetect();
        } else {
            showToast("未获取录音权限");
        }

        noteLoader = new NoteLoader(this);

        // 复制文件
        String abasepath = getFilesDir().getPath();
        String toPath = abasepath + "/";  // Your application path
        copyAsset(getAssets(), "merlin_gold_plus.sf2", toPath + "merlin_gold.sf2");
        copyAsset(getAssets(), "song_xxx.xml", toPath + "song_xxx.xml");

        // 读取MusicXML数据
        noteLoader.listener = this;
        noteLoader.loadStave("song_xxx.xml");

        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {

                super.handleMessage(msg);
            }
        };

        TimerTask task = new TimerTask() {
            public void run() {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
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


//        audioProcess.setOnFreqChangedListener(new AudioProcess.OnFreqChangedListener() {
//            @Override
//            public void onFreqChanged(float freq) {
//                if (freq >= 0) {
//                    Log.i(TAG, "freq = " + freq);
//                }
//            }
//        });
//        audioProcess.start();

    }

    @Override
    public void onPitchChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        audioEvent.getEndTimeStamp();
        float pitch = pitchDetectionResult.getPitch();

        if (pitch > 60) {
            final String msg = "pitch = " + pitch + "\ntimeStamp = " + timeStamp + "\nendTimeStamp = " + audioEvent.getEndTimeStamp();
            final String noteName = NoteMap.caculateNoteName(pitch);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(msg);
                    noteNameTextView.setText(noteName);
                }
            });

            if (!preNoteName.equalsIgnoreCase(noteName)) {
                InputNote note = new InputNote();
                note.noteName = noteName;
                note.timeStamp = timeStamp;

                inputArray.add(note);

                preNoteName = noteName;
            }
        } else {
            preNoteName = "";
        }

        Log.i(TAG, "preNoteName = " + preNoteName);
    }

    @Override
    public void onNoteLoaded(NoteLoader loader, String notes) {
//        String abasepath = getFilesDir().getPath();
//        String toPath = abasepath + "/";  // Your application path

//        player.resetNotes(notes);
//        player.resetSoundSourcePathUrl(toPath + "merlin_gold.sf2");
//        player.play(noteLoader.result);
//        player.start(MainActivity.this);

        Gson gson = new Gson();
        musicXMLNote = gson.fromJson(notes, new TypeToken<MusicXMLNote>() {
        }.getType());

        int a = 5;
        int b = 4;
    }

    @Override
    public void onProgress(float progress) {

    }

    public void caculate(View view) {
        int a = 5;
    }

    public void clear(View view) {
        inputArray.clear();
        audioProcess.start();
    }

    @Override
    public void onFreqChanged(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        audioEvent.getEndTimeStamp();
        float pitch = pitchDetectionResult.getPitch();

        if (pitch > 60) {
            final String msg = "pitch = " + pitch + "\ntimeStamp = " + timeStamp + "\nendTimeStamp = " + audioEvent.getEndTimeStamp();
            final String noteName = NoteMap.caculateNoteName(pitch);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(msg);
                    noteNameTextView.setText(noteName);
                }
            });

            if (!preNoteName.equalsIgnoreCase(noteName)) {
                InputNote note = new InputNote();
                note.noteName = noteName;
                note.timeStamp = timeStamp;

                inputArray.add(note);

                preNoteName = noteName;
            }
        } else {
            preNoteName = "";
        }

        Log.i(TAG, "preNoteName = " + preNoteName);
    }
}
