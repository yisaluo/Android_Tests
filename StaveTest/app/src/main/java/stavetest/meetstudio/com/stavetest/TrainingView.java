package stavetest.meetstudio.com.stavetest;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;

/**
 * Created by ChrisDu on 2017/8/23.
 */

public class TrainingView extends RelativeLayout {

    WebView webView;
    Button instrumentButton;
    Button voiceButton;
    Button recordButton;
    Button stopAndScoreButton;
    RelativeLayout staveLayout;
    RelativeLayout controlLayout;
    View cover;

    public TrainingView(Context context) {
        super(context);
    }

    public TrainingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrainingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TrainingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void initViews() {
        webView = (WebView) this.findViewById(R.id.webview);
        instrumentButton = (Button) this.findViewById(R.id.standard_instrument);
        voiceButton = (Button) this.findViewById(R.id.standard_voice);
        recordButton = (Button) this.findViewById(R.id.standard_stop_and_score);
        stopAndScoreButton = (Button) this.findViewById(R.id.standard_record);
        staveLayout = (RelativeLayout) this.findViewById(R.id.layout_stave);
        controlLayout = (RelativeLayout) this.findViewById(R.id.control_buttons);
        cover = this.findViewById(R.id.cover);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.getSettings().setAllowFileAccessFromFileURLs(true);
            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
    }

    public void active() {
        cover.setVisibility(View.GONE);
        controlLayout.setVisibility(View.VISIBLE);
    }

    public void deactive() {
        cover.setVisibility(View.VISIBLE);
        controlLayout.setVisibility(View.GONE);
    }
}
