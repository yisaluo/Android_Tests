package audio.meetstudio.com.audiodemo;

/**
 * Created by ChrisDu on 2016/10/17.
 */

public interface OnByteReadListener {
    void onByteRead(int length, byte[] audioByteBuffer);
}
