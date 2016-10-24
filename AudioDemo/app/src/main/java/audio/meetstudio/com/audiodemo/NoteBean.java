package audio.meetstudio.com.audiodemo;

/**
 * Created by ChrisDu on 2016/10/12.
 */

public class NoteBean {

    public NoteBean(String name, long start, int duration, boolean rest, MusicXMLNote.MeasuresBean.PartsBean.VoicesBean.NotesBean bean) {
        this.noteName = name;
        this.start = start;
        this.duration = duration;
        this.rest = rest;
        this.notesBean = bean;
        this.type = 3;  // 0:right, 1:弹晚了, 2:抬早了, 3:wrong
        this.newInput = false;
        this.newInputTime = 0;
    }

    public String noteName;
    public long start;
    public int duration;
    public boolean rest;
    public int note;
    public MusicXMLNote.MeasuresBean.PartsBean.VoicesBean.NotesBean notesBean;
    public int type;
    public boolean newInput;
    public long newInputTime;
}
