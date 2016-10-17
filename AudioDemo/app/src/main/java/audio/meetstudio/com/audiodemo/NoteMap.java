package audio.meetstudio.com.audiodemo;

import java.util.HashMap;

/**
 * Created by ChrisDu on 2016/10/11.
 */

public class NoteMap {

    /*
    {*********
27.500
29.135
30.868
32.703
34.648
36.708
38.891
41.203
43.654
46.249
48.999
51.913
55.000
58.270
61.735
65.406
69.296
73.416
77.782
82.407
87.307
92.499
97.999
103.826
110.000
116.541
123.471
130.813
138.591
146.832
155.563
164.814
174.614
184.997
195.998
207.652
220.000
233.082
246.942
261.626
277.183
293.665
311.127
329.628
349.228
369.994
391.995
415.305
440.000
466.164
493.883
523.251
554.365
587.330
622.254
659.255
698.456
739.989
783.991
830.609
880.000
932.328
987.767
1046.502
1108.731
1174.659
1244.508
1318.510
1396.913
1479.978
1567.982
1661.219
1760.000
1864.655
1975.533
2093.005
2217.461
2349.318
2489.016
2637.020
2793.826
2959.955
3135.963
3322.438
3520.000
3729.310
3951.066
4186.009
**********}
     */

    public static final float C4_FREQ = 440;
    public static final float STEP_FREQ = (float) Math.pow(2, 1f / 12f);

    public static HashMap<String, String> NoteMap = new HashMap<>();

    /**
     * 根据识别到的频率值，计算出距其最近的音
     *
     * @param pitch 识别到的频率
     * @return 音名
     */
    public static String caculateNoteName(float pitch) {
        // 所在八度计算
        if (pitch < 27.5) {
            return "";
        }
        
        int octaveNum = 0;
        int noteIndex = 0;
        float freq = 0;
        for (; octaveNum < 9; octaveNum++) {
            float baseFreq = 27.5f * (float)(Math.pow(2, octaveNum));
            float minFreq = baseFreq * (1 + 1 / STEP_FREQ) / 2;
            if (pitch >= minFreq && pitch < 2 * minFreq) {
                // 计算在此八度内的index
                for (int j = 0; j < 12; j++) {
                    float min = (float)(minFreq * Math.pow(STEP_FREQ, j));
                    float max = (float)(minFreq * Math.pow(STEP_FREQ, j + 1));
                    if (pitch <  max&& pitch >= min) {
                        noteIndex = j;
                        freq = (float)(baseFreq * Math.pow(STEP_FREQ, j));
                        break;
                    }
                }

                break;
            }
        }

        String noteNameInOctave = getNoteNameInOctave(noteIndex);
        String noteName = String.format("%s/%d,%f", noteNameInOctave, noteIndex > 2 ? octaveNum + 1 : octaveNum, freq);

        return noteName;
    }

    private static String getNoteNameInOctave(int index) {
        switch (index) {
            case 0:
                return "A";
            case 1:
                return "#A";
            case 2:
                return "B";
            case 3:
                return "C";
            case 4:
                return "#C";
            case 5:
                return "D";
            case 6:
                return "#D";
            case 7:
                return "E";
            case 8:
                return "F";
            case 9:
                return "#F";
            case 10:
                return "G";
            case 11:
                return "#G";
            default:
                break;
        }

        return "";
    }
}
