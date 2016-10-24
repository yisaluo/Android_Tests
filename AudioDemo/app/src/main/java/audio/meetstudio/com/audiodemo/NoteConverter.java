package audio.meetstudio.com.audiodemo;

import android.text.TextUtils;

import org.w3c.dom.Text;

import java.util.HashMap;

/**
 * Created by ChrisDu on 2016/10/20.
 */

public class NoteConverter {
    public static HashMap<String, Integer> accMap = new HashMap<>();

    //do 1 对应的note值
    public static int noteIntDo;
    //是否是主升 （全用升号表示）
    public static boolean isMainSharp;
    //小节的序号
    public static int measureIdx;
    //前一个调的 分子
    public static int preTimeSigNumerator;
    //前一个调的分母
    public static int preTimeSigDenominator;

    //转调的Map
    public static HashMap<String, String> alterMap = new HashMap<>();

    public static int toneFromKey(String key, String acc) {
        int accInt = 0;
        if (acc.length() > 0) {
            if (!accMap.containsKey(key)) {
                accMap.put(key, 0);
            }

            Integer integer = accMap.get(key);
            if (acc.equalsIgnoreCase("#")) {
                integer += 1;
            } else if (acc.equalsIgnoreCase("b")) {
                integer -= 1;
            } else if (acc.equalsIgnoreCase("n")) {
                integer = 0;
            } else if (acc.equalsIgnoreCase("##")) {
                integer += 2;
            } else if (acc.equalsIgnoreCase("bb")) {
                integer -= 2;
            } else if (acc.equalsIgnoreCase("+")) {
                integer += 1;
            } else if (acc.equalsIgnoreCase("++")) {
                integer += 2;
            } else if (acc.equalsIgnoreCase("bbs")) {
                integer += 3;
            }
        }

        if (accMap.containsKey(key)) {
            accInt = accMap.get(key);
        }

        int index = key.indexOf("/");
        String octaveString = key.substring(index + 1, index + 2);
        String keyString = key.substring(0, 1);
        char keyC = keyString.charAt(0);
        int octave = Integer.valueOf(octaveString);

        int tone = 0;
        switch (keyC) {
            case 'C':
                tone = octave * 12 + 8 + 4;
                break;
            case 'D':
                tone = octave * 12 + 8 + 6;
                break;
            case 'E':
                tone = octave * 12 + 8 + 8;
                break;
            case 'F':
                tone = octave * 12 + 8 + 9;
                break;
            case 'G':
                tone = octave * 12 + 8 + 11;
                break;
            case 'A':
                tone = octave * 12 + 20 + 1;
                break;
            case 'B':
                tone = octave * 12 + 20 + 3;
                break;

            default:
                break;
        }

        String keyAlter = key.substring(1, 2);
        if (keyAlter.length() > 0) {
            if (keyAlter.equalsIgnoreCase("b") || keyAlter.equalsIgnoreCase("B")) {
                tone -= 1;
            } else if (keyAlter.equalsIgnoreCase("bb")) {
                tone -= 2;
            } else if (keyAlter.equalsIgnoreCase("#")) {
                tone += 1;
            } else if (keyAlter.equalsIgnoreCase("##")) {
                tone += 2;
            }
        }

        return tone;
    }

    //音符音高转道简谱音符
    public static String noteMusjeStrWith(MusicXMLNote.MeasuresBean.PartsBean.VoicesBean.NotesBean note, int leftDur) {
        String noteStr = "";

        //step 1 获取是否连线
        if (note.getTie() != null && note.getTie().equalsIgnoreCase("begin")) {
            noteStr = noteStr + "(";
        }

        //step 2 获取音高
        if (note.isRest()) {
            noteStr = noteStr + "0";
        } else {
            int noteInt = toneFromKey(note.getKeys().get(0), note.getKeys().get(0).substring(1, 2));
            //与中音do之间的差值
            int noteOffset = noteInt - noteIntDo;
            //点的个数
            int dotNum = 0;
            boolean isUp = noteOffset >= 0;
            if (isUp) {
                dotNum = (int) (noteOffset / 12);
                String noteValStr = noteValStrWith(noteOffset % 12, dotNum);
                noteStr = noteStr + noteValStr;
                //高音点
                while (dotNum > 0) {
                    noteStr = noteStr + "'";
                    dotNum--;
                }
            } else {
                while (noteOffset < 0) {
                    noteOffset += 12;
                    dotNum++;
                }
                String noteValStr = noteValStrWith(noteOffset, -dotNum);
                noteStr = noteStr + noteValStr;
                //低音点
                while (dotNum > 0) {
                    noteStr = noteStr + ",";
                    dotNum--;
                }
            }
        }

        //step 3 时值检测
        int noteDur = Math.min(leftDur, note.getIntrinsicTicks());
        if (noteDur <= 40) {
            //32分音符
            noteStr = noteStr + "=_";
        } else if (noteDur <= 56) {
            //32分附点音符
            noteStr = noteStr + "=_.";
        } else if (noteDur <= 80) {
            //16分音符
            noteStr = noteStr + "=";
        } else if (noteDur <= 112) {
            //16分附点音符
            noteStr = noteStr + "=.";
        } else if (noteDur <= 160) {
            //八分音符
            noteStr = noteStr + "_";
        } else if (noteDur <= 224) {
            //八分附点音符
            noteStr = noteStr + "_.";
        } else if (noteDur <= 320) {
            //四分音符
        } else if (noteDur <= 448) {
            //四分附点音符
            noteStr = noteStr + ".";
        } else if (noteDur <= 640) {
            //二分音符
            noteStr = noteStr + "-";
        } else if (noteDur <= 896) {
            //二分附点音符
            noteStr = noteStr + "-.";
        } else if (noteDur <= 1152) {
            //全音符
            noteStr = noteStr + "---";
        } else if (noteDur <= 1408) {
            //5 个四分音符
            noteStr = noteStr + "----";
        } else if (noteDur <= 1664) {
            //6 个四分音符
            noteStr = noteStr + "---.";
        }

        //step 4 检测连线end
        if (note.getTie() != null && note.getTie().equalsIgnoreCase("end")) {
            noteStr = noteStr + ")";
        }

        return noteStr;
    }

    //0 到11 之间音符值得转换
    public static String noteValStrWith(int noteInt, int dotNum) {
        if (noteInt < 0 || noteInt > 12) {
            return "音符值问题";
        }

        String noteValStr;
        String preKeyStr = String.valueOf(dotNum) + "_";
        switch (noteInt) {
            case 0: {
                String alterVal = alterMap.get(preKeyStr + "1");
                if (!TextUtils.isEmpty(alterVal)) {
                    //已变过调
                    noteValStr = "n1";
                    alterMap.put(preKeyStr + "1", "");
                } else {
                    noteValStr = "1";
                }
            }
            break;
            case 1: {
                String alterVal = alterMap.get(preKeyStr + (isMainSharp ? "1" : "2"));
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0
                        &&
                        ((isMainSharp && alterVal.equalsIgnoreCase("1")))
                        ||
                        ((!isMainSharp && alterVal.equalsIgnoreCase("-1")))) {
                    //已变过调
                    noteValStr = isMainSharp ? "1" : "2";
                } else {
                    noteValStr = isMainSharp ? "#1" : "b2";
                    alterMap.put(preKeyStr + (isMainSharp ? "1" : "2"), (isMainSharp ? "1" : "-1"));
                }
            }
            break;
            case 2: {
                String alterVal = alterMap.get(preKeyStr + "2");
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0) {
                    //已变过调
                    noteValStr = "n2";
                    alterMap.put(preKeyStr + "2", "");
                } else {
                    noteValStr = "2";
                }
            }
            break;
            case 3: {
                String alterVal = alterMap.get(preKeyStr + (isMainSharp ? "2" : "3"));
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0
                        && ((isMainSharp && alterVal.equalsIgnoreCase("1")) || ((!isMainSharp && alterVal.equalsIgnoreCase("-1"))))) {
                    //已变过调
                    noteValStr = isMainSharp ? "2" : "3";
                } else {
                    noteValStr = isMainSharp ? "#2" : "b3";
                    alterMap.put(preKeyStr + (isMainSharp ? "2" : "3"), isMainSharp ? "1" : "-1");
                }
            }
            break;
            case 4: {
                String alterVal = alterMap.get(preKeyStr + "3");
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0) {
                    //已变过调
                    noteValStr = "n3";
                    alterMap.put(preKeyStr + "3", "");
                } else {
                    noteValStr = "3";
                }
            }
            break;
            case 5: {
                String alterVal = alterMap.get(preKeyStr + "4");
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0) {
                    //已变过调
                    noteValStr = "n4";
                    alterMap.put(preKeyStr + "4", "");
                } else {
                    noteValStr = "4";
                }
            }
            break;
            case 6: {
                String alterVal = alterMap.get(preKeyStr + (isMainSharp ? "4" : "5"));
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0 && ((isMainSharp && alterVal.equalsIgnoreCase("1")) || ((!isMainSharp && alterVal.equalsIgnoreCase("-1"))))) {
                    //已变过调
                    noteValStr = isMainSharp ? "4" : "5";
                } else {
                    noteValStr = isMainSharp ? "#4" : "b5";
                    alterMap.put(preKeyStr + (isMainSharp ? "4" : "5"), isMainSharp ? "1" : "-1");
                }
            }
            break;
            case 7: {
                String alterVal = alterMap.get(preKeyStr + "5");
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0) {
                    //已变过调
                    noteValStr = "n5";
                    alterMap.put(preKeyStr + "5", "");
                } else {
                    noteValStr = "5";
                }
            }
            break;
            case 8: {
                String alterVal = alterMap.get(preKeyStr + (isMainSharp ? "5" : "6"));
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0 && ((isMainSharp && alterVal.equalsIgnoreCase("1")) || ((!isMainSharp && alterVal.equalsIgnoreCase("-1"))))) {
                    //已变过调
                    noteValStr = isMainSharp ? "5" : "6";
                } else {
                    noteValStr = isMainSharp ? "#5" : "b6";
                    alterMap.put(preKeyStr + (isMainSharp ? "5" : "6"), isMainSharp ? "1" : "-1");
                }
            }
            break;
            case 9: {
                String alterVal = alterMap.get(preKeyStr + "6");
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0) {
                    //已变过调
                    noteValStr = "n6";
                    alterMap.put(preKeyStr + "6","");
                } else {
                    noteValStr = "6";
                }
            }
            break;
            case 10: {
                String alterVal = alterMap.get(isMainSharp ? "6" : "7");
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0 && ((isMainSharp && alterVal.equalsIgnoreCase("1")) || ((!isMainSharp && alterVal.equalsIgnoreCase("-1"))))) {
                    //已变过调
                    noteValStr = isMainSharp ? "6" : "7";
                } else {
                    noteValStr = isMainSharp ? "#6" : "b7";
                    alterMap.put(preKeyStr + (isMainSharp ? "6" : "7"), isMainSharp ? "1" : "-1");
                }
            }
            break;
            case 11: {
                String alterVal = alterMap.get(preKeyStr + "7");
                if (!TextUtils.isEmpty(alterVal) && alterVal.length() > 0) {
                    //已变过调
                    noteValStr = "n7";
                    alterMap.put(preKeyStr + "7", "");
                } else {
                    noteValStr = "7";
                }
            }
            break;
            default:
                noteValStr = "";
                break;
        }

        return noteValStr;
    }
}
