/*
 * Copyright (C) 2010 AVM GmbH <info@avm.de>
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2008 Hughes Systique Corporation, USA (http://www.hsc.com)
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

package de.avm.android.fritzapp.sipua;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import org.sipdroid.net.KeepAliveSip;
import org.zoolu.net.IpAddress;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.gui.ComStatus;
import de.avm.android.fritzapp.sipua.ui.LoopAlarm;
import de.avm.android.fritzapp.sipua.ui.Receiver;
import de.avm.android.fritzapp.sipua.ui.Sipdroid;

public class SipdroidEngine implements RegisterAgentListener {

	public static final int UNINITIALIZED = 0x0;
	public static final int INITIALIZED = 0x2;

	/** User Agent */
	private UserAgent ua;

	/** Register Agent */
	private RegisterAgent ra;

	private KeepAliveSip ka;
	
	/** UserAgentProfile */
	private UserAgentProfile user_profile;

	private SipProvider sip_provider;
	
	private static final long REREGISTER_TIMEOUT = 15000; // 15s
	private Timer reregister_timeout = null;
	
	static PowerManager.WakeLock wl;
	
	public void finalize() {
		remove_reregister_timeout();
	}
	
	public boolean StartEngine() {
		try {
			PowerManager pm = (PowerManager) getUIContext().getSystemService(Context.POWER_SERVICE);
			if (wl == null) wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FRITZApp.SipdroidEngine");

			user_profile = new UserAgentProfile(null);
			user_profile.username = Sipdroid.getSipUser();
			user_profile.passwd = PreferenceManager.getDefaultSharedPreferences(getUIContext())
					.getString(Sipdroid.PREF_SIPPASS,"");
			user_profile.realm = Sipdroid.getServerPref();

			SipStack.init(null);
			SipStack.debug_level = 0; // NB: change this for debug messages
			//SipStack.log_path = "/data/data/org.sipdroid.sipua";  // NB: uncomment this for debug messages
			SipStack.max_retransmission_timeout = 4000;
			SipStack.transaction_timeout = 30000;
			SipStack.default_transport_protocols = new String[1];
			SipStack.default_transport_protocols[0] = Sipdroid.getProtocolPref();
			SipStack.default_port = Sipdroid.getPortPref();
			
			String version = "FRITZApp/" + Sipdroid.getVersion() + "/" + Build.MODEL;
			SipStack.ua_info = version;
			SipStack.server_info = version;
				
			IpAddress.setLocalIpAddress();
			
			sip_provider = new SipProvider(IpAddress.localIpAddress, 0);
			
			user_profile.contact_url = user_profile.username
				+ "@"
				+ IpAddress.localIpAddress + (sip_provider.getPort() != 0?":"+sip_provider.getPort():"");
			
			user_profile.from_url = user_profile.username
				+ "@"
				+ user_profile.realm;
			if (PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("callerid","").length() == 0) {
				user_profile.callerid = user_profile.from_url;
			} else {
				user_profile.callerid = PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("callerid","")
					+ "@"
					+ user_profile.realm;
			}

			ua = new UserAgent(sip_provider, user_profile);
			ra = new RegisterAgent(sip_provider, user_profile.callerid, // modified
					user_profile.contact_url, user_profile.username,
					user_profile.realm, user_profile.passwd, this, user_profile);
			// ka = new KeepAliveSip(sip_provider,100000); // we don't need keep alives. NB

			register();
			listen();
		} catch (Exception E) {
		}

		return true;
	}
	
	// This method has to be called on Wifi connection state changes.
	public void onWifiChanged() {
		// try to register whether or not we have a wifi connection. NB
		this.register();
	}
	
	void setOutboundProxy() {
		try {
			sip_provider.setOutboundProxy(new SocketAddress(
					IpAddress.getByName(PreferenceManager.getDefaultSharedPreferences(getUIContext()).getString("dns","")),
					SipStack.default_port));
		} catch (UnknownHostException e) {
		}
	}
	
	public void CheckEngine() {
		if (!sip_provider.hasOutboundProxy())
			setOutboundProxy();
	}

	public Context getUIContext() {
		return Receiver.mContext;
	}
	
	public int getRemoteVideo() {
		return ua.remote_video_port;
	}
	
	public int getLocalVideo() {
		return ua.local_video_port;
	}
	
	public String getRemoteAddr() {
		return ua.remote_media_address;
	}
	
	private void remove_reregister_timeout() {
		if(reregister_timeout != null) {
			reregister_timeout.cancel();
			reregister_timeout.purge();
			reregister_timeout = null;
		}
	}
	
	public void expire() {
		if (ra != null && ra.CurrentState == RegisterAgent.REGISTERED) {
			remove_reregister_timeout();
			reregister_timeout = new Timer();
			reregister_timeout.schedule(new TimerTask() {
				public void run() {
					if (ra != null && ra.CurrentState == RegisterAgent.REGISTERED) {					
						ra.CurrentState = RegisterAgent.UNREGISTERED;
						GLOBAL.mStatus.setSip(ComStatus.SIP_NOTREGISTERED, "");
						cancel();
					}
				}
			}, REREGISTER_TIMEOUT);
		}
		register();
	}
	
	public void unregister() {
		if (ra != null && ra.unregister()) {
			Receiver.alarm(0, LoopAlarm.class);
			GLOBAL.mStatus.setSip(ComStatus.SIP_IDLE, "");
			wl.acquire();
		}		
	}
	
	public void register() {
		try {
		if (user_profile == null || user_profile.username.equals("") ||
				user_profile.realm.equals("") || user_profile.passwd.equals("")) return;
		IpAddress.setLocalIpAddress();
		user_profile.contact_url = user_profile.username
			+ "@"
			+ IpAddress.localIpAddress + (sip_provider.getPort() != 0?":"+sip_provider.getPort():"");
		if (!Receiver.isFast()) {
			unregister();
		} else {
			if (ra != null && ra.register()) {
				if(reregister_timeout == null)
					GLOBAL.mStatus.setSip(ComStatus.SIP_IDLE, "");
				wl.acquire();
			}
		}
		} catch (Exception ex) {
		}
	}

	public void halt() { // modified
		remove_reregister_timeout();
		if (wl.isHeld())
			wl.release();
		if (ka != null) {
			Receiver.alarm(0, LoopAlarm.class);
			ka.halt();
		}
		GLOBAL.mStatus.setSip(ComStatus.SIP_NOTREGISTERED, "");
		if (ra != null)
			ra.halt();
		if (ua != null)
			ua.hangup();
		if (sip_provider != null)
			sip_provider.halt();
	}

	public boolean isRegistered()
	{
		if (ra == null)
		{
			return false;
		}
		return ra.isRegistered();
	}
	
	public void onUaRegistrationSuccess(RegisterAgent ra, NameAddress target,
			NameAddress contact, String result) {
		remove_reregister_timeout();		
		if (isRegistered()) {
			if (Receiver.on_wlan)
				Receiver.alarm(60, LoopAlarm.class);
			GLOBAL.mStatus.setSip(ComStatus.SIP_AVAILABLE, "");
		} else
			GLOBAL.mStatus.setSip(ComStatus.SIP_NOTREGISTERED, "");
		Receiver.registered();
		ra.subattempts = 0;
		ra.startMWI();
		if (wl.isHeld())
			wl.release();
	}

	String lastmsgs;
	
    public void onMWIUpdate(boolean voicemail, int number, String vmacc) {
		if (voicemail) {
			String msgs = getUIContext().getString(R.string.voicemail);
			if (number != 0) {
				msgs = msgs + ": " + number;
			}
			Receiver.MWI_account = vmacc;
			if (lastmsgs == null || !msgs.equals(lastmsgs)) {
				Receiver.onText(Receiver.MWI_NOTIFICATION, msgs,android.R.drawable.stat_notify_voicemail,0);
				lastmsgs = msgs;
			}
		} else {
			Receiver.onText(Receiver.MWI_NOTIFICATION, null, 0,0);
			lastmsgs = null;
		}
	}

	static long lasthalt;
	
	/** When a UA failed on (un)registering. */
	public void onUaRegistrationFailure(RegisterAgent ra, NameAddress target,
			NameAddress contact, String result) {
		GLOBAL.mStatus.setSip(ComStatus.SIP_AWAY, result);
		if (wl.isHeld())
			wl.release();
		if (SystemClock.uptimeMillis() > lasthalt + 45000) {
			lasthalt = SystemClock.uptimeMillis();
			sip_provider.haltConnections();
		}
		updateDNS();
		ra.stopMWI();
	}
	
	public void updateDNS() {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(getUIContext()).edit();
		try {
			edit.putString("dns", IpAddress.getByName(Sipdroid.getServerPref()).toString());
		} catch (UnknownHostException e1) {
			return;
		}
		edit.commit();
	}

	/** Receives incoming calls (auto accept) */
	public void listen() 
	{
		ua.printLog("UAS: WAITING FOR INCOMING CALL");
		
		if (!ua.user_profile.audio && !ua.user_profile.video)
		{
			ua.printLog("ONLY SIGNALING, NO MEDIA");
		}
		
		ua.listen();
	}
	
	public void info(char c, int duration) {
		ua.info(c, duration);
	}
	
	/** Makes a new call */
	public boolean call(String target_url) {
		ua.printLog("UAC: CALLING " + target_url);
		
		if (!ua.user_profile.audio && !ua.user_profile.video)
		{
			 ua.printLog("ONLY SIGNALING, NO MEDIA");
		}
		return ua.call(target_url, false);
	}

	public void answercall() 
	{
		ua.accept();
	}

	public void rejectcall() {
		ua.printLog("UA: HANGUP");
		ua.hangup();
	}

	public void togglehold() {
		ua.reInvite(null, 0);
	}

	public void transfer(String number) {
		ua.callTransfer(number, 0);
	}
	
	public void togglemute() {
		if (ua.muteMediaApplication())
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.menu_mute), android.R.drawable.stat_notify_call_mute,Receiver.ccCall.base);
		else
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call,Receiver.ccCall.base);			
	}
	
	public int speaker(int mode) {
		if (mode == AudioManager.MODE_NORMAL)
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.menu_speaker), android.R.drawable.stat_sys_speakerphone,Receiver.ccCall.base);
		else
			Receiver.onText(Receiver.CALL_NOTIFICATION, getUIContext().getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call,Receiver.ccCall.base);
		return ua.speakerMediaApplication(mode);
	}
	
	/** When a new call is incoming */
	public void onState(int state,String text) {
			Receiver.onState(state,text);
	}

	public void keepAlive() {
		if (ka != null && Receiver.on_wlan && isRegistered())
			try {
				ka.sendToken();
				Receiver.alarm(60, LoopAlarm.class);
			} catch (IOException e) {
				if (!Sipdroid.release) e.printStackTrace();
			}
	}
}
