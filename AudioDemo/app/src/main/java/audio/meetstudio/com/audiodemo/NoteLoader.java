package audio.meetstudio.com.audiodemo;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * 作者：chaoyongzhang on 15/9/9 16:09
 * 邮箱：zhangcy@meet-future.com
 */
public class NoteLoader {
    private static final String APP_CACAHE_DIRNAME = "/webcache";
    public String result = null;
    private Context mContext;

    public interface NoteLoaderListener{
        void onNoteLoaded(NoteLoader loader, String notes);
    }

    public NoteLoaderListener listener = null;
    private static WebView webView;
    public NoteLoader(Context context){
        mContext = context;
    }

    public void initWebView() {
        webView = new WebView(mContext);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        String cacheDirPath = mContext.getFilesDir().getAbsolutePath()+APP_CACAHE_DIRNAME;
//      String cacheDirPath = getCacheDir().getAbsolutePath()+Constant.APP_DB_DIRNAME;
        //设置数据库缓存路径
        webView.getSettings().setDatabasePath(cacheDirPath);
        //设置  Application Caches 缓存目录
        webView.getSettings().setAppCachePath(cacheDirPath);
        //开启 Application Caches 功能
        webView.getSettings().setAppCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            webView.getSettings().setAllowFileAccessFromFileURLs(true);
            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
        webView.addJavascriptInterface(this, "control");
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void loadStave(String filename){
        initWebView();
        String url = "file:///android_asset/octave_json.html" + "?filename=" + filename;
        webView.loadUrl(url);
    }

    public void loadWebStave(String url){
        String urlStr = "file:///android_asset/octave_web_json.html" + "?filename=" + url;
        webView.loadUrl(urlStr);
    }

    @Override
    protected void finalize() throws Throwable {
        // webView.destroy();
        super.finalize();
    }

    @JavascriptInterface
    public void onNoteResult(String result) {
        Log.i("onNoteResult", "onSumResult result=" + result);
        this.result = result;
        if (listener != null){
            listener.onNoteLoaded(this, result);
        }
    }
}
