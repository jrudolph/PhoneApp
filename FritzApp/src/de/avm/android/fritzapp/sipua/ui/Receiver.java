/*
 * Copyright (C) 2010 AVM GmbH <info@avm.de>
 * Copyright (C) 2009 The Sipdroid Open Source Project
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

package de.avm.android.fritzapp.sipua.ui;

import org.sipdroid.media.RtpStreamReceiver;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.gui.FRITZApp;
import de.avm.android.fritzapp.sipua.SipdroidEngine;
import de.avm.android.fritzapp.sipua.UserAgent;
import de.avm.android.fritzapp.sipua.phone.Call;
import de.avm.android.fritzapp.sipua.phone.Connection;

	public class Receiver extends BroadcastReceiver {

		final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
		final static String ACTION_SIGNAL_STRENGTH_CHANGED = "android.intent.action.SIG_STR";
		final static String ACTION_DATA_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
		final static String ACTION_DOCK_EVENT = "android.intent.action.DOCK_EVENT";
		final static String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
		final static String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
		final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
		
		public final static int CALL_NOTIFICATION = 2;
		public final static int MISSED_CALL_NOTIFICATION = 3;
		public final static int AUTO_ANSWER_NOTIFICATION = 4;
		public final static int MWI_NOTIFICATION = 5;
		
		final static long[] vibratePattern = {0,1000,1000};
		
		public static int docked = -1;
		public static int headset = -1;
		public static SipdroidEngine mSipdroidEngine;
		
		public static Context mContext;
		public static FRITZApp.AskCallrouteHandler mAskCallrouteHandler = null;
		public static Call ccCall;
		public static Connection ccConn;
		public static int call_state;
		
		public static String pstn_state;
		public static long pstn_time;
		public static String MWI_account;
		private static String laststate,lastnumber;	
		
		public static MediaPlayer ringbackPlayer;

		public static SipdroidEngine engine(Context context) {
			mContext = context;
			if (mSipdroidEngine == null) {
				mSipdroidEngine = new SipdroidEngine();
				mSipdroidEngine.StartEngine();
			} /*else
				mSipdroidEngine.CheckEngine();
				*/
        	context.startService(new Intent(context,RegisterService.class));
			return mSipdroidEngine;
		}
		
		static Ringtone oRingtone;
		static PowerManager.WakeLock wl;
				
		public static void stopRingtone() {
			android.os.Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
			v.cancel();
			if (Receiver.oRingtone != null) {
				Ringtone ringtone = Receiver.oRingtone;
				Receiver.oRingtone = null;
				ringtone.stop();
			}
		}
		
		public static void onState(int state,String caller) {
			if (ccCall == null) {
		        ccCall = new Call();
		        ccConn = new Connection();
		        ccCall.setConn(ccConn);
		        ccConn.setCall(ccCall);
			}
			if (call_state != state) {
				call_state = state;
				android.os.Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
				switch(call_state)
				{
				case UserAgent.UA_STATE_INCOMING_CALL:
					lock(true);
					RtpStreamReceiver.good = RtpStreamReceiver.lost = RtpStreamReceiver.loss = RtpStreamReceiver.late = 0;
					String text = caller.toString();
					if (text.indexOf("<sip:") >= 0 && text.indexOf("@") >= 0)
						text = text.substring(text.indexOf("<sip:")+5,text.indexOf("@"));
					String text2 = caller.toString();
					if (text2.indexOf("\"") >= 0)
						text2 = text2.substring(text2.indexOf("\"")+1,text2.lastIndexOf("\""));
					else 
						text2 = text;
					
					broadcastCallStateChanged("RINGING", caller);
			        mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
					ccCall.setState(Call.State.INCOMING);
					ccConn.setUserData(null);
					ccConn.setAddress(text,text2);
					ccConn.setIncoming(true);
					ccConn.date = System.currentTimeMillis();
					ccCall.base = 0;
					AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
					int rm = am.getRingerMode();
					int vs = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
			        KeyguardManager mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
					if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Sipdroid.PREF_AUTOON, false) &&
							!mKeyguardManager.inKeyguardRestrictedInputMode())
						v.vibrate(vibratePattern,1);
					else {
						if ((pstn_state == null || pstn_state.equals("IDLE")) &&
								(rm == AudioManager.RINGER_MODE_VIBRATE ||
								(rm == AudioManager.RINGER_MODE_NORMAL && vs == AudioManager.VIBRATE_SETTING_ON)))
							v.vibrate(vibratePattern,1);
						if (am.getStreamVolume(AudioManager.STREAM_RING) > 0) {				 
							String sUriSipRingtone = PreferenceManager.getDefaultSharedPreferences(mContext)
									.getString(Sipdroid.PREF_RINGTONE, "");
							Uri oUriSipRingtone = null;
							if(!TextUtils.isEmpty(sUriSipRingtone))
								oUriSipRingtone = Uri.parse(sUriSipRingtone);
							else
								oUriSipRingtone = Settings.System.DEFAULT_RINGTONE_URI;						
							oRingtone = RingtoneManager.getRingtone(mContext, oUriSipRingtone);
							oRingtone.play();						
						}
					}
					moveTop();
					if (wl == null) {
						PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
						wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
								PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "FRITZApp");
					}
					wl.acquire();
					break;
				case UserAgent.UA_STATE_OUTGOING_CALL:
					lock(true);
					RtpStreamReceiver.good = RtpStreamReceiver.lost = RtpStreamReceiver.loss = RtpStreamReceiver.late = 0;
					onText(MISSED_CALL_NOTIFICATION, null, 0,0);
					if(!engine(mContext).isRegistered()) // Fix NB
						engine(mContext).register();
					broadcastCallStateChanged("OFFHOOK", caller);
					ccCall.setState(Call.State.DIALING);
					ccConn.setUserData(null);
					ccConn.setAddress(caller,caller);
					ccConn.setIncoming(false);
					ccConn.date = System.currentTimeMillis();
					ccCall.base = 0;
					moveTop();
					break;
				case UserAgent.UA_STATE_IDLE:
					lock(false);
					broadcastCallStateChanged("IDLE", null);
					onText(CALL_NOTIFICATION, null, 0,0);
					ccCall.setState(Call.State.DISCONNECTED);
					stopRingtone();
					if (wl != null && wl.isHeld())
						wl.release();
			        mContext.startActivity(createIntent(InCallScreen.class));
					ccConn.log(ccCall.base);
					ccConn.date = 0;
					engine(mContext).listen();
					break;
				case UserAgent.UA_STATE_INCALL:
					lock(true);
					broadcastCallStateChanged("OFFHOOK", null);
					if (ccCall.base == 0) {
						ccCall.base = SystemClock.elapsedRealtime();
					}
					onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_in_progress), android.R.drawable.stat_sys_phone_call,ccCall.base);
					ccCall.setState(Call.State.ACTIVE);
					stopRingtone();
					if (wl != null && wl.isHeld())
						wl.release();
			        mContext.startActivity(createIntent(InCallScreen.class));
		       		if (docked > 0)
	    				engine(mContext).speaker(AudioManager.MODE_NORMAL);
					break;
				case UserAgent.UA_STATE_HOLD:
					lock(false);
					onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_on_hold), android.R.drawable.stat_sys_phone_call_on_hold,ccCall.base);
					ccCall.setState(Call.State.HOLDING);
			        mContext.startActivity(createIntent(InCallScreen.class));
					break;
				}
				if (ringbackPlayer != null && ringbackPlayer.isPlaying()) {
					ringbackPlayer.stop();
				}
			}
		}
		
		public static void onText(int type,String text,int mInCallResId,long base) {
	        NotificationManager mNotificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	        if (text != null) {
		        Notification notification = new Notification();
		        notification.icon = mInCallResId;
				if (type == MISSED_CALL_NOTIFICATION) {
			        	notification.flags |= Notification.FLAG_AUTO_CANCEL;
			        	notification.setLatestEventInfo(mContext, text, mContext.getString(R.string.app_name),
			        			PendingIntent.getActivity(mContext, 0, createCallLogIntent(), 0));
		        		// blue LED flashing
			        	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			        	notification.ledARGB = 0xff0000ff; /* blue */
			        	notification.ledOnMS = 125;
			        	notification.ledOffMS = 2875;
	        	} else {
	        		switch (type) {
		        	case MWI_NOTIFICATION:
			        	notification.flags |= Notification.FLAG_AUTO_CANCEL;
						notification.contentIntent = PendingIntent.getActivity(mContext, 0, 
								createMWIIntent(), 0);	
			        	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			        	notification.ledARGB = 0xff00ff00; /* green */
			        	notification.ledOnMS = 125;
			        	notification.ledOffMS = 2875;
						break;
		        	case AUTO_ANSWER_NOTIFICATION:
						notification.contentIntent = PendingIntent.getActivity(mContext, 0,
				                createIntent(AutoAnswer.class), 0);
						break;
		        	default:
						notification.contentIntent = PendingIntent.getActivity(mContext, 0,
					            createIntent(FRITZApp.class), 0);
		        		break;
		        	}			
		        	notification.flags |= Notification.FLAG_ONGOING_EVENT;
			        RemoteViews contentView = new RemoteViews(mContext.getPackageName(),
	                        R.layout.ongoing_call_notification);
			        contentView.setImageViewResource(R.id.icon, notification.icon);
					if (base != 0) {
						contentView.setChronometer(R.id.text1, base, text+" (%s)", true);
					} else
						contentView.setTextViewText(R.id.text1, text);
					notification.contentView = contentView;
		        }
		        mNotificationMgr.notify(type,notification);
	        } else {
	        	mNotificationMgr.cancel(type);
	        }
	        if (type != AUTO_ANSWER_NOTIFICATION)
	        	updateAutoAnswer();
		}
		
		static void updateAutoAnswer() {
			if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Sipdroid.PREF_AUTOONDEMAND, false) &&
				Sipdroid.on(mContext)) {
				if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Sipdroid.PREF_AUTOONDEMAND, false))
					updateAutoAnswer(1);
				else
					updateAutoAnswer(0);
			} else
				updateAutoAnswer(-1);
		}

		// no notification for auto answer setting
//		private static int autoAnswerState = -1;
		
		static void updateAutoAnswer(int status) {
//			if (status != autoAnswerState) {
//				switch (autoAnswerState = status) {
//				case 0:
//					Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION,mContext.getString(R.string.auto_disabled),R.drawable.auto_answer_disabled,0);
//					break;
//				case 1:
//					Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION,mContext.getString(R.string.auto_enabled),R.drawable.auto_answer,0);
//					break;
//				case -1:
//					Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION, null, 0, 0);
//					break;
//				}
//			}
		}
		
		public static void registered() {
		}
	    
		static PowerManager.WakeLock mPartialWakeLock = null;
		static PowerManager.WakeLock mDimmWakeLock = null;

		static void lock(boolean lock)
		{
//			Log.d("Receiver", String.format("lock(%s) - currently %s",
//					Boolean.toString(lock), (mPartialWakeLock == null) ?
//					"uninitialized" : Boolean.toString(mPartialWakeLock.isHeld())));
		
			if (lock)
			{
				if (mPartialWakeLock == null)
				{
					PowerManager pm = (PowerManager)mContext
							.getSystemService(Context.POWER_SERVICE);
					mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
							"FRITZApp.Receiver");

					// workaround for broken audio output while SCREEN_OFF
					// possibly a bug on HTC Desire/Google Nexus One
					// prevent SCREEN_OFF, dimm instead
					if (Build.MODEL.equals("HTC Desire") ||
							Build.MODEL.equals("Nexus One") ||
							Build.MODEL.equals("Archos5"))
						mDimmWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
								PowerManager.ON_AFTER_RELEASE, "FRITZApp.Receiver");
				}
				if (!mPartialWakeLock.isHeld()) mPartialWakeLock.acquire();
				if ((mDimmWakeLock != null) && !mDimmWakeLock.isHeld())
					mDimmWakeLock.acquire();
			}
			else
			{
				if ((mPartialWakeLock != null) && mPartialWakeLock.isHeld())
					mPartialWakeLock.release();
				if ((mDimmWakeLock != null) && mDimmWakeLock.isHeld())
					mDimmWakeLock.release();
			}
		}
		
		static boolean was_playing;
		
		static void broadcastCallStateChanged(String state,String number) {
			if (state == null) {
				state = laststate;
				number = lastnumber;
			}
			Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
			intent.putExtra("state",state);
			if (number != null)
				intent.putExtra("incoming_number", number);
			intent.putExtra(mContext.getString(R.string.app_name), true);
			mContext.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
			if (state.equals("IDLE")) {
				if (was_playing) {
					if (pstn_state == null || pstn_state.equals("IDLE"))
						mContext.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
					was_playing = false;
				}
			} else {
				AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
				if ((laststate == null || laststate.equals("IDLE")) && (was_playing = am.isMusicActive()))
					mContext.sendBroadcast(new Intent(PAUSE_ACTION));
			}
			laststate = state;
			lastnumber = number;
		}
		
		public static void alarm(int renew_time,Class <?>cls) {
       		if (!Sipdroid.release) Log.i("SipUA:","alarm "+renew_time);
	        Intent intent = new Intent(mContext, cls);
	        PendingIntent sender = PendingIntent.getBroadcast(mContext,
	                0, intent, 0);
			AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
			am.cancel(sender);
			if (renew_time > 0)
				am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+renew_time*1000, sender);
		}
		
		public static void reRegister(int renew_time) {
       		alarm(renew_time-15, OneShotAlarm.class);
		}

		static Intent createIntent(Class<?>cls) {
        	Intent startActivity = new Intent();
        	startActivity.setClass(mContext,cls);
    	    startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	    return startActivity;
		}
		
		public static Intent createCallLogIntent() {
	        Intent intent = new Intent(Intent.ACTION_VIEW, null);
	        intent.setType("vnd.android.cursor.dir/calls");
	        return intent;
		}
		
		public static Intent createHomeIntent() {
	        Intent intent = new Intent(Intent.ACTION_MAIN, null);
	        intent.addCategory(Intent.CATEGORY_HOME);
	        return intent;
		}

	    static Intent createMWIIntent() {
			Intent intent;

			if (MWI_account != null)
				intent = new Intent(Intent.ACTION_CALL, Uri.parse(MWI_account));
			else
				intent = new Intent(Intent.ACTION_DIAL);
			return intent;
		}
		
		public static void moveTop() {
			onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_in_progress), android.R.drawable.stat_sys_phone_call, 0);
			mContext.startActivity(createIntent(Activity2.class)); 
		}

		public static boolean on_wlan;
		
		public static boolean isFast()
		{
			if (GLOBAL.DEBUG_NO_COM_CHECK)
			{
				// pretend to connect over WLAN for debugging
        		on_wlan = true;
        		return on_wlan;
			}
			else
			{
	        	WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	        	WifiInfo wi = wm.getConnectionInfo();
	
	        	if (wi != null) {
	        		if (!Sipdroid.release) Log.i("SipUA:","isFast() "+WifiInfo.getDetailedStateOf(wi.getSupplicantState())
	        				+" "+wi.getIpAddress());
		        	if (wi.getIpAddress() != 0 && (WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.OBTAINING_IPADDR
		        			|| WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.CONNECTED)) {
		        		on_wlan = true;
		        		return on_wlan;
		        	}
	        	}
	        	on_wlan = false;
	         	return isFast2();
			}
		}
		
		static boolean isFast2() {
			// wlan only!
        	return false;
		}
		
		public static int speakermode() {
			if(docked > 0 && headset <= 0)
				return AudioManager.MODE_NORMAL;
			else
				return AudioManager.MODE_IN_CALL;
		}
		
	    @Override
		public void onReceive(Context context, Intent intent) {
	        String intentAction = intent.getAction();
	        if (!Sipdroid.on(context)) return;
        	if (!Sipdroid.release) Log.i("SipUA:",intentAction);
        	if (mContext == null) mContext = context;
        	/*
	        if (intentAction.equals(Intent.ACTION_BOOT_COMPLETED)){
	            //if(!engine(mContext).isRegistered()) // Fix NB/SH
	               engine(context).register();
	        } else */
	        if (intentAction.equals(ACTION_DATA_STATE_CHANGED)) {
	            if(!engine(mContext).isRegistered()) // Fix NB/SH
	               engine(context).register();
	        } else
	        if (intentAction.equals(ACTION_PHONE_STATE_CHANGED) &&
	        		!intent.getBooleanExtra(context.getString(R.string.app_name),false)) {
	        	stopRingtone();
	    		pstn_state = intent.getStringExtra("state");
	    		pstn_time = SystemClock.elapsedRealtime();
	    		if (pstn_state.equals("IDLE") && call_state != UserAgent.UA_STATE_IDLE)
	    			broadcastCallStateChanged(null,null);
	    		if ((pstn_state.equals("OFFHOOK") && call_state == UserAgent.UA_STATE_INCALL) ||
		    			(pstn_state.equals("IDLE") && call_state == UserAgent.UA_STATE_HOLD))
		    			engine(context).togglehold();
	        } else
	        if (intentAction.equals(ACTION_DOCK_EVENT)) {
	        	docked = intent.getIntExtra(EXTRA_DOCK_STATE, -1);
	        	if (call_state == UserAgent.UA_STATE_INCALL)
	        		engine(mContext).speaker(speakermode());
	        } else
		    if (intentAction.equals(Intent.ACTION_HEADSET_PLUG)) {
		        	headset = intent.getIntExtra("state", -1);
		        	if(call_state == UserAgent.UA_STATE_INCALL)
		        		engine(mContext).speaker(speakermode());
	        }
		}

		public static void setAskCallrouteHandler(FRITZApp.AskCallrouteHandler handler)
		{
			mAskCallrouteHandler = handler; 
		}   
}
