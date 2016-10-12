package audio.meetstudio.com.audiodemo;

/**
 * Created by ChrisDu on 2016/10/12.
 */

public class NoteBean {

    public NoteBean(String name, long start, int duration) {
        this.noteName = name;
        this.start = start;
        this.duration = duration;
    }

    public String noteName;
    public long start;
    public int duration;

}
