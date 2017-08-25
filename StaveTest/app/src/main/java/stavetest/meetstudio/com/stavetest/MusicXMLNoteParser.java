package stavetest.meetstudio.com.stavetest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by ChrisDu on 2017/8/25.
 */

public class MusicXMLNoteParser {

    public class Note {

        public Note() {

        }

        public Note(int note, float pitch, int duration, boolean rest, int startTick, float startTime, float durationTime) {
            this.midiNote = note;
            this.pitch = pitch;
            this.durationTicks = duration;
            this.rest = rest;
            this.startTick = startTick;
            this.startTime = startTime;
            this.durationTime = durationTime;
        }

        public int midiNote;
        public float pitch;
        public int durationTicks;
        public boolean rest;
        public int startTick;
        public float startTime;
        public float durationTime;
    }

    public class Time {
        public int num_beats;
        public int beat_value;
    }

    private static MusicXMLNoteParser m_instance = null;

    private int totalDurationTicks = 0;

    private int tempo = 0;

    private Time mTime;

    private int beat_unit;
    private int divisions;

    public static MusicXMLNoteParser getInstance() {
        if (m_instance == null) {
            m_instance = new MusicXMLNoteParser();
        }

        return m_instance;
    }

    ArrayList<Note> parseJsonData(String jsonData) {
        totalDurationTicks = 0;
        ArrayList<Note> notesArray = new ArrayList<Note>();

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray measures = jsonObject.optJSONArray("measures");
            if (measures.length() > 0) {
                for (int i = 0; i < measures.length(); i++) {

                    if (i == 0) {
                        tempo = measures.optJSONObject(0).optInt("per_minute");

                        mTime = new Time();
                        mTime.num_beats = measures.optJSONObject(0).optJSONObject("time").optInt("num_beats");
                        mTime.beat_value = measures.optJSONObject(0).optJSONObject("time").optInt("beat_value");

                        beat_unit = mTime.beat_value = measures.optJSONObject(0).optInt("beat_unit");
                        divisions = mTime.beat_value = measures.optJSONObject(0).optInt("divisions");
                    }

                    JSONArray parts = measures.optJSONObject(i).optJSONArray("parts");
                    JSONObject firstVoice = parts.optJSONObject(0).optJSONArray("voices").optJSONObject(0);
                    JSONArray notes = firstVoice.optJSONArray("notes");
                    // 遍历notes
                    for (int j = 0; j < notes.length(); j++) {
                        Note note = new Note();
                        boolean rest = notes.optJSONObject(j).getBoolean("rest");
                        int durationTicks = notes.optJSONObject(j).getInt("intrinsicTicks");
                        note.startTick = totalDurationTicks;
                        if (rest) {
                            // 休止符
                            note.rest = true;
                            note.durationTicks = durationTicks;
                        } else {
                            JSONArray keys = notes.optJSONObject(j).optJSONArray("keys");
                            // 视唱支持一一个音的识别
                            String firstKey = keys.optString(0);
                            int firstNote = NoteConverter.toneFromKey(firstKey, "");
                            float firstPitch = NoteMap.getPitchOfNote(firstNote);

                            note.rest = false;
                            note.midiNote = firstNote;
                            note.pitch = firstPitch;
                        }

                        float timePerBeat = 60.f / (float)tempo;
                        float beats = (float)totalDurationTicks / (float)divisions;
                        float time = timePerBeat * beats;
                        note.startTime = time;

                        beats = (float)durationTicks / (float)divisions;
                        time = timePerBeat * beats;
                        note.durationTime = time;

                        totalDurationTicks += durationTicks;


                        notesArray.add(note);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return notesArray;
    }

    public int getTotalDurationTicks() {
        return this.totalDurationTicks;
    }

    public float getTotalDuration() {
        float timePerBeat = 60.f / (float)tempo;
        float beats = totalDurationTicks / divisions;
        float totalDuration = timePerBeat * beats;
        return totalDuration;
    }
}
