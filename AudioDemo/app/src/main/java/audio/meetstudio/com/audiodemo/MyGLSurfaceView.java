package audio.meetstudio.com.audiodemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ChrisDu on 2016/10/18.
 */

public class MyGLSurfaceView extends GLSurfaceView {

    private MyRenderer myRenderer;

    public MyGLSurfaceView(Context context) {
        super(context);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initMyRenderer() {
        myRenderer = new MyRenderer();
        setRenderer(myRenderer);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        queueEvent(new Runnable() {
            public void run() {
                myRenderer.setColor(event.getX() / getWidth(), event.getY() / getHeight(), 0.5f);
            }
        });
        return true;
    }

    public class MyRenderer implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {
            gl10.glViewport(0, 0, i, i1);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            gl10.glClearColor(mRed, mGreen, mBlue, 1.0f);
            gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        }

        public void setColor(float r, float g, float b) {
            mRed = r;
            mGreen = g;
            mBlue = b;
        }

        private float mRed;
        private float mGreen;
        private float mBlue;
    }
}
