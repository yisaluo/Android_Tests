package audio.meetstudio.com.audiodemo;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static audio.meetstudio.com.audiodemo.MyGLSurfaceView.*;


/**
 * Created by ChrisDu on 2016/10/17.
 */

public class SongSelectActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ArrayList<SongBean> songList = new ArrayList<>();
    private ListView mSongListView = null;

    public final static String INTENT_SONG_NAME = "INTENT_SONG_NAME";
    public final static String INTENT_FILE_NAME = "INTENT_FILE_NAME";

    String url;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        url = getIntent().getStringExtra("fasdfasdfad");

        setContentView(R.layout.songselect_main);
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initMyGLSurfaceView();

        // 复制文件
        String abasepath = getFilesDir().getPath();
        String toPath = abasepath + "/";  // Your application path
        copyAsset(getAssets(), "merlin_gold_plus.sf2", toPath + "merlin_gold.sf2");
        copyAsset(getAssets(), "song_xxx.xml", toPath + "song_xxx.xml");
        copyAsset(getAssets(), "qian_yu_qian_xun.xml", toPath + "qian_yu_qian_xun.xml");
        copyAsset(getAssets(), "cong_cong_na_nian.xml", toPath + "cong_cong_na_nian.xml");
        copyAsset(getAssets(), "let_it_go.xml", toPath + "let_it_go.xml");

        // 歌曲列表
        songList.add(new SongBean("小星星", "song_xxx.xml"));
        songList.add(new SongBean("匆匆那年", "cong_cong_na_nian.xml"));
        songList.add(new SongBean("Let It Go", "let_it_go.xml"));
//        songList.add(new SongBean("千与千寻", "qian_yu_qian_xun.xml"));
        songList.add(new SongBean("节奏测试", ""));
        mSongListView = (ListView)findViewById(R.id.song_list);
        mSongListView.setOnItemClickListener(this);
        SongListAdapter mAdapter = new SongListAdapter();
        mSongListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
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

    /**
     * 显示歌曲列表
     */
    public void showSongList() {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        SongBean bean = songList.get(i);
        Intent intent = new Intent(SongSelectActivity.this, MainActivity.class);
        intent.putExtra(INTENT_SONG_NAME, bean.mSongName);
        intent.putExtra(INTENT_FILE_NAME, bean.mFileName);

        if (i == songList.size() - 1) {
            intent = new Intent(SongSelectActivity.this, RhythmTestActivity.class);
        }

        startActivity(intent);
    }

    public class SongListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return songList.size();
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
        public View getView(int i, View view, ViewGroup viewGroup) {
            SongHolder holder;
            if (view == null || view.getTag() == null) {
                view = new LinearLayout(SongSelectActivity.this.getBaseContext());

                TextView name = new TextView(SongSelectActivity.this);
                ((LinearLayout)view).addView(name);
                holder = new SongHolder();
                holder.songName = name;

                view.setTag(holder);
            } else {
                holder = (SongHolder)view.getTag();
            }

            SongBean bean = songList.get(i);
            holder.songName.setText(bean.mSongName);
            holder.songName.setTextColor(Color.WHITE);
            holder.songName.setTextSize(30);
            return view;
        }
    }

    public class SongHolder {
        public TextView songName;
    }

    private class SongBean {

        private String mSongName;
        private String mFileName;

        public SongBean(String songName, String fileName) {
            this.mSongName = songName;
            this.mFileName = fileName;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        myGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myGLSurfaceView.onPause();
    }

    //------------------
    // GLSurfaceView
    private MyGLSurfaceView myGLSurfaceView;
    private void initMyGLSurfaceView() {
        myGLSurfaceView = (MyGLSurfaceView)findViewById(R.id.my_surfaceview);
        myGLSurfaceView.initMyRenderer();
    }
}
