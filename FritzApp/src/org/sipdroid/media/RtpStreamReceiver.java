/*
 * Copyright (C) 2010 AVM GmbH <info@avm.de>
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.sipdroid.media;

import java.io.IOException;
import java.net.SocketException;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import de.avm.android.fritzapp.sipua.UserAgent;
import de.avm.android.fritzapp.sipua.ui.Receiver;
import de.avm.android.fritzapp.sipua.ui.Sipdroid;

/**
 * RtpStreamReceiver is a generic stream receiver. It receives packets from RTP
 * and writes them into an OutputStream.
 */
public class RtpStreamReceiver extends Thread {

	/** Whether working in debug mode. */
	public static boolean DEBUG = true;

	/** Payload type */
	int p_type;

	/** Size of the read buffer */
	public static final int BUFFER_SIZE = 1024;

	/** Maximum blocking time, spent waiting for reading new bytes [milliseconds] */
	public static final int SO_TIMEOUT = 200;

	/** The RtpSocket */
	RtpSocket rtp_socket = null;

	public enum AudioEngineState {STATE_NOTHING, STATE_INITIALIZED, STATE_UNINITIALIZED};
	private AudioEngineState audioEngineState;
	
	/** Whether it is running */
	boolean restartAudioTrack = false;
	boolean running;
	AudioManager am;
	ContentResolver cr;
	public static int speakermode = -1;
	
	/**
	 * Constructs a RtpStreamReceiver.
	 * 
	 * @param output_stream
	 *            the stream sink
	 * @param socket
	 *            the local receiver SipdroidSocket
	 */
	public RtpStreamReceiver(SipdroidSocket socket, int payload_type) {
		init(socket);
		p_type = payload_type;
		audioEngineState = AudioEngineState.STATE_NOTHING;
	}

	/** Inits the RtpStreamReceiver */
	private void init(SipdroidSocket socket) {
		user = 0;
		if (socket != null)
			rtp_socket = new RtpSocket(socket);
	}

	/** Whether is running */
	public boolean isRunning() {
		return running;
	}

	/** Stops running */
	public void halt() {
		running = false;
	}
	
	public int speaker(int mode) {
		int old = speakermode;
		
		if ((Receiver.headset > 0 || Receiver.docked > 0) && mode != Receiver.speakermode())
			return old;
		setMode(speakermode = mode, false);
		setRestartAudioTrack();
		// NB: we better call the following two methods in the audio playback thread (see run()). 		
		// setCodec(); restoreVolume();
		return old;
	}

	static int oldvol = -1;

	static int stream() {
		return speakermode == AudioManager.MODE_IN_CALL?AudioManager.STREAM_VOICE_CALL:AudioManager.STREAM_MUSIC;
	}
	
	double smin = 200,s;
	public static int nearend;
	
	void calc(short[] lin,int off,int len) {
		int i,j;
		double sm = 30000,r;
		
		for (i = 0; i < len; i += 5) {
			j = lin[i+off];
			s = 0.03*Math.abs(j) + 0.97*s;
			if (s < sm) sm = s;
			if (s > smin) nearend = 5000*mu/5;
			else if (nearend > 0) nearend--;
		}
		for (i = 0; i < len; i++) {
			j = lin[i+off];
			if (j > 6550)
				lin[i+off] = 6550*5;
			else if (j < -6550)
				lin[i+off] = -6550*5;
			else
				lin[i+off] = (short)(j*5);
		}
		r = (double)len/(100000*mu);
		smin = sm*r + smin*(1-r);
	}
	
	public static void adjust(int keyCode) {
        AudioManager mAudioManager = (AudioManager) Receiver.mContext.getSystemService(
                    Context.AUDIO_SERVICE);
        mAudioManager.adjustStreamVolume(stream(),
                    keyCode == KeyEvent.KEYCODE_VOLUME_UP
                            ? AudioManager.ADJUST_RAISE
                            : AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI);
	}

	static void setStreamVolume(final int stream,final int vol,final int flags) {
        (new Thread() {
			public void run() {
				AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
				am.setStreamVolume(stream, vol, flags);
				if (stream == stream()) restored = true;
			}
        }).start();
	}
	
	static boolean restored;
	
	void restoreVolume() {
		switch (getMode()) {
		case AudioManager.MODE_IN_CALL:
				setStreamVolume(AudioManager.STREAM_RING,(int)(
						am.getStreamMaxVolume(AudioManager.STREAM_RING)*
						Sipdroid.getEarGain()), 0);
				track.setStereoVolume(AudioTrack.getMaxVolume()*
						Sipdroid.getEarGain()
						,AudioTrack.getMaxVolume()*
						Sipdroid.getEarGain());
				break;
		case AudioManager.MODE_NORMAL:
				track.setStereoVolume(AudioTrack.getMaxVolume(),AudioTrack.getMaxVolume());
				break;
		}
		setStreamVolume(stream(),
				PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("volume"+speakermode, 
				am.getStreamMaxVolume(stream())*
				(speakermode == AudioManager.MODE_NORMAL?4:3)/4
				),0);
	}
	
	void saveVolume() {
		if (restored) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putInt("volume"+speakermode,am.getStreamVolume(stream()));
			edit.commit();
		}
	}
	
	void saveSettings() {
		if (!PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(Sipdroid.PREF_OLDVALID, Sipdroid.DEFAULT_OLDVALID)) {
			int oldvibrate = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
			int oldvibrate2 = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
			if (!PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).contains(Sipdroid.PREF_OLDVIBRATE2))
				oldvibrate2 = AudioManager.VIBRATE_SETTING_ON;
			int oldpolicy = android.provider.Settings.System.getInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, 
					Settings.System.WIFI_SLEEP_POLICY_DEFAULT);
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putInt(Sipdroid.PREF_OLDVIBRATE, oldvibrate);
			edit.putInt(Sipdroid.PREF_OLDVIBRATE2, oldvibrate2);
			edit.putInt(Sipdroid.PREF_OLDPOLICY, oldpolicy);
			edit.putInt(Sipdroid.PREF_OLDRING,am.getStreamVolume(AudioManager.STREAM_RING));
			edit.putBoolean(Sipdroid.PREF_OLDVALID, true);
			edit.commit();
		}
	}
	
	private static int sdkVersion() {
		return Integer.parseInt(Build.VERSION.SDK); // accessing SDK_INT causes exception on cupcake (os 1.5). NB
	}
	
	public static int getMode() {
		AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
		if (sdkVersion() >= 5)
			return am.isSpeakerphoneOn()?AudioManager.MODE_NORMAL:AudioManager.MODE_IN_CALL;
		else
			return am.getMode();
	}
	
	public static void setMode(int mode, boolean speakerOff) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
		edit.putBoolean(Sipdroid.PREF_SETMODE, true);
		edit.commit();
		AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
		if (sdkVersion() >= 5) {
			if(speakerOff)
				am.setSpeakerphoneOn(false);
			else
				am.setSpeakerphoneOn(mode == AudioManager.MODE_NORMAL);
		}
		else
			am.setMode(mode);
	}
	
	public static void restoreMode(boolean speakerOff) {
		if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(Sipdroid.PREF_SETMODE, Sipdroid.DEFAULT_SETMODE)) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putBoolean(Sipdroid.PREF_SETMODE, false);
			edit.commit();
			if (Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE")) {
				AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
				if(sdkVersion() >= 5)
					am.setSpeakerphoneOn(false);
				else
					am.setMode(AudioManager.MODE_NORMAL);
			}
		}
	}

	void initMode() {
		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL &&
				(Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE")))
			setMode(AudioManager.MODE_NORMAL, false);
	}
	
	public static void restoreSettings(boolean speakerOff) {
		if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(Sipdroid.PREF_OLDVALID, Sipdroid.DEFAULT_OLDVALID)) {
			AudioManager am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
	        ContentResolver cr = Receiver.mContext.getContentResolver();
			int oldvibrate = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt(Sipdroid.PREF_OLDVIBRATE, Sipdroid.DEFAULT_OLDVIBRATE);
			int oldvibrate2 = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt(Sipdroid.PREF_OLDVIBRATE2, Sipdroid.DEFAULT_OLDVIBRATE2);
			int oldpolicy = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt(Sipdroid.PREF_OLDPOLICY, Sipdroid.DEFAULT_OLDPOLICY);
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,oldvibrate);
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,oldvibrate2);
			Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY, oldpolicy);
			am.setStreamVolume(AudioManager.STREAM_RING, PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getInt("oldring",0), 0);
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
			edit.putBoolean(Sipdroid.PREF_OLDVALID, false);
			edit.commit();
			PowerManager pm = (PowerManager) Receiver.mContext.getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
					PowerManager.ACQUIRE_CAUSES_WAKEUP, "Sipdroid.RtpStreamReceiver");
			wl.acquire(1000);
		}
		restoreMode(speakerOff);
	}

	public static float good, late, lost, loss;
	double avgheadroom;
	public static int timeout;
	
	void empty() {
		try {
			rtp_socket.getDatagramSocket().setSoTimeout(1);
			for (;;)
				rtp_socket.receive(rtp_packet);
		} catch (SocketException e2) {
			if (!Sipdroid.release) e2.printStackTrace();
		} catch (IOException e) {
		}
		try {
			rtp_socket.getDatagramSocket().setSoTimeout(1000);
		} catch (SocketException e2) {
			if (!Sipdroid.release) e2.printStackTrace();
		}
	}
	
	public synchronized AudioEngineState audioEngineInitialized() {
		return audioEngineState;
	}
	
	private synchronized void changeAudioEngineState(AudioEngineState state) {
		audioEngineState = state;
	}
	
	RtpPacket rtp_packet;
	AudioTrack track;
	int maxjitter,minjitter,minjitteradjust,minheadroom;
	int cnt, cnt2, user, luser, luser2, lserver;		

	public static int jitter,mu;
	
	void setCodec() {
		mu = 1;
		maxjitter = AudioTrack.getMinBufferSize(8000,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		if (maxjitter < 2*2*BUFFER_SIZE*3*mu)
			maxjitter = 2*2*BUFFER_SIZE*3*mu;
		if(track != null) {
			track.stop();
			track.release();
			track = null;
		}
		track  = new AudioTrack(stream(), 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
				maxjitter, AudioTrack.MODE_STREAM);
		maxjitter /= 2*2;
		minjitter = minjitteradjust = 500*mu;
		jitter = 875*mu;
		minheadroom = maxjitter*2;
		timeout = 1;
		luser = luser2 = -8000*mu;
		cnt = cnt2 = user = lserver = 0;
	}
	
	void write(short a[],int b,int c) {
		user += track.write(a,b,c);
	}
	
	synchronized void setRestartAudioTrack() {
		restartAudioTrack = true;
	}

	synchronized boolean restartAudioTrack() {
		boolean ret = restartAudioTrack;
		restartAudioTrack = false;
		return ret;
	}
	
	/** Runs it in a new Thread. */
	public void run() {
		boolean nodata = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(Sipdroid.PREF_NODATA, Sipdroid.DEFAULT_NODATA);
		
		if (rtp_socket == null) {
			if (DEBUG)
				println("ERROR: RTP socket is null");
			return;
		}

		byte[] buffer = new byte[BUFFER_SIZE+12];
		rtp_packet = new RtpPacket(buffer, 0);

		if (DEBUG)
			println("Reading blocks of max " + buffer.length + " bytes");

		running = true;
		speakermode = Receiver.speakermode();
		restored = false;

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		am = (AudioManager) Receiver.mContext.getSystemService(Context.AUDIO_SERVICE);
        cr = Receiver.mContext.getContentResolver();
		saveSettings();
		Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY,Settings.System.WIFI_SLEEP_POLICY_NEVER);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,AudioManager.VIBRATE_SETTING_OFF);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,AudioManager.VIBRATE_SETTING_OFF);
		if (oldvol == -1) oldvol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		initMode();
		setCodec();
		int engineState = track.getState(); 
		if(engineState == AudioTrack.STATE_UNINITIALIZED) {
			android.util.Log.d("RtpStreamReceiver", "Error initializing audio playback engine.");
			changeAudioEngineState(AudioEngineState.STATE_UNINITIALIZED);
			running = false;
			track = null;
			return;
		}
		else
			changeAudioEngineState(AudioEngineState.STATE_INITIALIZED);
		track.setStereoVolume(AudioTrack.getMaxVolume()*
				Sipdroid.getEarGain()
				,AudioTrack.getMaxVolume()*
				Sipdroid.getEarGain());
		short lin[] = new short[BUFFER_SIZE];
		short lin2[] = new short[BUFFER_SIZE];
		int server, headroom, todo, len = 0, seq = 0, m = 1, expseq, getseq, vm = 1, gap, gseq;
		
		timeout = 1;
		luser = luser2 = -8000*mu;
		cnt = cnt2 = user = lserver = 0;
		minheadroom = maxjitter*2;
		ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,(int)(ToneGenerator.MAX_VOLUME*2*Sipdroid.getEarGain()));
		G711.init();
		track.play();
		//empty();
		System.gc();
		while (running) {
			if(restartAudioTrack()) {
				setCodec();
				restoreVolume();
			}
			if (Receiver.call_state == UserAgent.UA_STATE_HOLD) {
				tg.stopTone();
				track.pause();
				while (running && Receiver.call_state == UserAgent.UA_STATE_HOLD) {
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
				track.play();
				System.gc();
				timeout = 1;
				seq = 0;
				luser = luser2 = -8000*mu;
			}
			try {
				rtp_socket.receive(rtp_packet);
				if (timeout != 0) {
					tg.stopTone();
					track.pause();
					for (int i = maxjitter*2; i > 0; i -= BUFFER_SIZE)
						write(lin2,0,i>BUFFER_SIZE?BUFFER_SIZE:i);
					cnt += maxjitter*2;
					track.play();
					empty();
				}
				timeout = 0;
			} catch (IOException e) {
				if (timeout == 0 && nodata) {
					tg.startTone(ToneGenerator.TONE_SUP_RINGTONE);
				}
				rtp_socket.getDatagramSocket().disconnect();
				if (++timeout > 22) {
					Receiver.engine(Receiver.mContext).rejectcall();
					break;
				}
			}
			if (running && timeout == 0) {		
				 gseq = rtp_packet.getSequenceNumber();
				 if (seq == gseq) {
					 m++;
					 continue;
				 }
				 server = track.getPlaybackHeadPosition();
				 headroom = user-server;
				 
				 if (headroom > 1 * jitter) // 2*
					 cnt += len;
				 else
					 cnt = 0;
				 
				 if (lserver == server)
					 cnt2++;
				 else
					 cnt2 = 0;

				 if (cnt <= 500 || cnt2 >= 2 || headroom - jitter < len) { // NB: 600, 875
					 switch (rtp_packet.getPayloadType()) {
					 case 0:
						 len = rtp_packet.getPayloadLength();
						 G711.ulaw2linear(buffer, lin, len);
						 break;
					 case 8:
						 len = rtp_packet.getPayloadLength();
						 G711.alaw2linear(buffer, lin, len);
						 break;
					 }
					 
		 			 if (speakermode == AudioManager.MODE_NORMAL)
		 				 calc(lin,0,len);
				 }
				 
				 avgheadroom = avgheadroom * 0.99 + (double)headroom * 0.01;
				 if (headroom < minheadroom)
					 minheadroom = headroom;
	 			 if (headroom < 250*mu) {// 200, 250
	 				 late++;
	 				 if (good != 0 && lost/good < 0.01) {
	 					 if (late /good > 0.01 && jitter + minjitteradjust < maxjitter) {
	 						 jitter += minjitteradjust;
	 						 late = 0;
	 						 luser2 = user;
	 						 minheadroom = maxjitter * 2;
	 					 }
	 				 }
					todo = jitter - headroom; // 600, 875
					write(lin2,0,todo> BUFFER_SIZE ? BUFFER_SIZE : todo);
				 }

				 if (cnt > 500*mu && cnt2 < 2) {
					 todo = headroom - jitter; // 600, 875
					 //println("cut "+todo);
					 if (todo < len)
						 write(lin,todo,len-todo);
				 } else
					 write(lin,0,len);
				 
				 if (seq != 0) {
					 getseq = gseq&0xff;
					 expseq = ++seq&0xff;
					 if (m == RtpStreamSender.m) vm = m;
					 gap = (getseq - expseq) & 0xff;
					 if (gap > 0) {
						 if (gap > 100) gap = 1;
						 loss += gap;
						 lost += gap;
						 good += gap - 1;
					 } else {
						 if (m < vm)
							 loss++;
					 }
					 good++;
					 if (good > 100) {
						 good *= 0.99;
						 lost *= 0.99;
						 loss *= 0.99;
						 late *= 0.99;
					 }
				 }
				 m = 1;
				 seq = gseq;
				 
				 if (user >= luser + 8000*mu && (
						 Receiver.call_state == UserAgent.UA_STATE_INCALL ||
						 Receiver.call_state == UserAgent.UA_STATE_OUTGOING_CALL)) {
					 if (luser == -8000*mu || getMode() != speakermode) {
						 saveVolume();
						 setMode(speakermode, false);
						 restoreVolume();
					 }
					 luser = user;
					 if (user >= luser2 + 160000*mu && good != 0 && lost/good < 0.01 && avgheadroom > minheadroom) {
						 int newjitter = (int)avgheadroom - minheadroom + minjitter;
						 if (jitter-newjitter > minjitteradjust)
							 jitter = newjitter;
						 minheadroom = maxjitter*2;
						 luser2 = user;
					 }
				 }
				 lserver = server;
			}
		}
		track.stop();
		track.release();
		tg.stopTone();
		tg.release();
		saveVolume();
		am.setStreamVolume(AudioManager.STREAM_MUSIC,oldvol,0);
		restoreSettings(true);
		am.setStreamVolume(AudioManager.STREAM_MUSIC,oldvol,0);
		oldvol = speakermode = -1;
		
		/* NB: don't beep at end of call.
		tg = new ToneGenerator(AudioManager.STREAM_RING,ToneGenerator.MAX_VOLUME/4*3);
		tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
		try {
			sleep(500);
		} catch (InterruptedException e) {
		}
		tg.stopTone();
		tg.release();
		*/
		
		rtp_socket.close();
		rtp_socket = null;

		if (DEBUG)
			println("rtp receiver terminated");
	}

	/** Debug output */
	private static void println(String str) {
		if (!Sipdroid.release) System.out.println("RtpStreamReceiver: " + str);
	}

	public static int byte2int(byte b) {
		return (b + 0x100) % 0x100;
	}

	public static int byte2int(byte b1, byte b2) {
		return (((b1 + 0x100) % 0x100) << 8) + (b2 + 0x100) % 0x100;
	}
}
