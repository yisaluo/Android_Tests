package stavetest.meetstudio.com.stavetest;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ChrisDu on 2017/8/25.
 */

public class MyTimer {

    private Context mContext;

    private float tickTime;

    private float maxTime = -1.f;

    private Timer mTimer;

    private static float TIMER_PERIOD = 1.f / 60.f;

    private MyTimerListener m_listener = null;

    private Handler doActionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            tick();
        }
    };

    public MyTimer(Context context, MyTimerListener listener) {
        this.mContext = context;
        this.m_listener = listener;
    }

    public void reset() {
        tickTime = 0.f;
        maxTime = -1.f;


    }

    public void startTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                doActionHandler.sendMessage(message);
            }
        }, 0, (int)(TIMER_PERIOD * 1000)); // 表示1000毫秒之後，每隔1000毫秒執行一次
    }

    public void pauseTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        reset();
    }

    public void setMaxTime(float max) {
        maxTime = max;
    }

    private void tick() {
        tickTime +=  TIMER_PERIOD;

        if (m_listener != null) {
            m_listener.onTimerUpdated(tickTime, TIMER_PERIOD);
        }

        // Log.i("tick", "tickTime = " + tickTime);

        if (maxTime > 0) {
            if (tickTime >= maxTime) {
                stopTimer();
                if (m_listener != null) {
                    m_listener.onTimerMax();
                }
            }
        }
    }

    public float getCurrentTickTimer() {
        return tickTime;
    }

    public interface MyTimerListener {
        public void onTimerUpdated(float totalTime, float dt);

        public void onTimerMax();
    }
}
