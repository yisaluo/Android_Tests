package stavetest.meetstudio.com.stavetest;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ChrisDu on 2017/8/24.
 *
 * 用于管理录音文件的录制、回放等
 */

public class RecordFileManager {

    private static RecordFileManager mInstance = null;

    private String recordFileName = "";

    public boolean isRecording = false;

    public boolean isPlaying = false;

    private MediaPlayer mediaPlayer = null;

    private int sampleRateInHz = AudioProcess.SAMPLE_RATE;

    private FileOutputStream recordFileOutputStream = null;

    private String basePath = "";

    private Context mContext;

    private Player mPlayer = null;

    /**
     * 单例模式
     * @return
     */
    public static RecordFileManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new RecordFileManager(context);

        }

        return mInstance;
    }

    public RecordFileManager(Context context) {
        mContext = context;
        mPlayer = new Player();
    }

    /**
     * 重置录音机播放状态
     */
    private void reset() {
        this.basePath = mContext.getFilesDir().toString() + "/";

        this.finishWriteRecordFileData();
        this.stopPlayRecordFile();

        isRecording = false;
        isPlaying = false;
    }

    /**
     * 准备录音文件，以便写入数据
     * @param fileName
     */
    public void prepareRecordFile(String fileName) {
        this.reset();

        this.recordFileName = basePath + fileName;

        File file = new File(this.recordFileName + ".raw");
        if (file.exists()) {
            file.delete();
            file = null;
            file = new File(this.recordFileName + ".raw");
        }

        isRecording = true;

        try {
            recordFileOutputStream = new FileOutputStream(file);// 建立一个可存取字节的文件

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向录音文件中写入数据
     * @param recordData
     */
    public void writeRecordFileData(byte[] recordData) {
        if (recordFileOutputStream != null && isRecording) {
            try {
                recordFileOutputStream.write(recordData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 停止向录音文件中写入数据
     */
    public void finishWriteRecordFileData() {
        isRecording = false;

        if (recordFileOutputStream != null) {
            try {
                recordFileOutputStream.close();
                recordFileOutputStream = null;
                saveAsWavFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * 将录音文件保存成wav格式的音频
     */
    public void saveAsWavFile() {
        File file = new File(this.recordFileName + ".wav");
        if (file.exists()) {
            file.delete();
            file = null;
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRateInHz;
        int channels = 1;
        long byteRate = 16 * sampleRateInHz * channels / 8;
        byte[] data = new byte[AudioProcess.BUFFER_SIZE * 4];
        try {
            in = new FileInputStream(this.recordFileName + ".raw");
            out = new FileOutputStream(this.recordFileName + ".wav");
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放录音文件
     */
    public void playRecordFile() {
        this.stopPlay();
        mPlayer.playLocalFile(this.recordFileName + ".wav");
    }

    /**
     * 停止播放录音文件
     */
    public void stopPlayRecordFile() {
        this.stopPlay();
    }

    /**
     * 播放本地文件
     * @param fileName
     */
    public void playLocalFile(String fileName) {
        mPlayer.playAssetsFile(fileName);
    }

    /**
     * 停止播放
     */
    public void stopPlay() {
        mPlayer.stop();
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
