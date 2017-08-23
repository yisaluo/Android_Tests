package stavetest.meetstudio.com.stavetest;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by ChrisDu on 2017/8/23.
 */

public class PitchView extends View {
    public PitchView(Context context) {
        super(context);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void updatePitch(float pitch) {

    }

    public void addTargetPitch() {

    }
}
