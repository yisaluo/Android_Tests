////////////////////////////////////////////////////////////////////////////////
//
//  MidiDriver - An Android Midi Driver.
//
//  Copyright (C) 2013	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.mididriver;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

// MidiDriver

@SuppressWarnings("JniMissingFunction")
public class MidiDriver implements Runnable
{
    private static final int SAMPLE_RATE = 22050;
    private static final int BUFFER_SIZE = 4096;

    private Thread thread;
    private AudioTrack audioTrack;
    private OnMidiStartListener listener;

    private short buffer[];

    // Constructor

    public MidiDriver()
    {
    }

    // Start midi

    public void start()
    {
	// Start the thread

	thread = new Thread(this, "MidiDriver");
	thread.start();
    }

	public int noteOn(int key, int vel){
		Log.i("midicpp", "preSend event:" + key + ", " + vel);
		return noteon(key, vel);
	}

	public int noteOff(int key){
		return noteoff(key);
	}

    // Run

    @Override
    public void run()
    {
	processMidi();
    }

    // Stop

    public void stop()
    {
	Thread t = thread;
	thread = null;

	// Wait for the thread to exit

	while (t != null && t.isAlive())
	    Thread.yield();
    }

    // Queue event
    public void sendMidi(int m, int n, int v)
    {

    }


    // Process MidiDriver

    private void processMidi()
    {
	int status = 0;
	int size = 0;

	// Init midi

	if ((size = init()) == 0)
	    return;

	buffer = new short[size/2];

	// Create audio track

	audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
				    AudioFormat.CHANNEL_OUT_STEREO,
				    AudioFormat.ENCODING_PCM_16BIT,
				    size/2, AudioTrack.MODE_STREAM);
	// Check audiotrack

	if (audioTrack == null)
	{
	    shutdown();
	    return;
	}

	// Check state

	int state = audioTrack.getState();

	if (state != AudioTrack.STATE_INITIALIZED)
	{
	    audioTrack.release();
	    shutdown();
	    return;
	}

	// Call listener

	if (listener != null)
	    listener.onMidiStart();

	// Play track

	audioTrack.play();

	// Keep running until stopped

	while (thread != null)
	{
	    // Write the midi events

	    // Render the audio

	    if (render(buffer) == 0)
		break;

	    // Write audio to audiotrack

	    status = audioTrack.write(buffer, 0, buffer.length);

	    if (status < 0)
		break;
	}

	// Render and write the last bit of audio

	if (status > 0)
	    if (render(buffer) > 0)
		audioTrack.write(buffer, 0, buffer.length);

	// Shut down audio

	shutdown();
	audioTrack.release();
    }

    // Set listener

    public void setOnMidiStartListener(OnMidiStartListener l)
    {
	listener = l;
    }

    // Listener interface

    public interface OnMidiStartListener
    {
	public abstract void onMidiStart();
    }

    // Native midi methods

    private native int     init();
    public  native int[]   config();
    private native int     render(short a[]);
    private native boolean write(byte a[]);
    public native boolean shutdown();
    public native int noteon(int key, int vel);
    public native int noteoff(int key);
	public native int sfload(String path);
	public native int pitch(int value);
	public native int mod(int value);
    // Load midi library

    static
    {
		System.loadLibrary("glib-2.0");
		System.loadLibrary("gthread-2.0");
		System.loadLibrary("midi");
    }
}
