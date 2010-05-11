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

package de.avm.android.fritzapp.sipua.ui;

import org.zoolu.sip.provider.SipStack;

import de.avm.android.fritzapp.com.DataHub;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class Sipdroid
{
	public static final boolean release = true; // NB: to enable LOGGING change this to "false"
	public static final boolean market = false;

	// see de.avm.android.fritzapp.R.array.settings_callroute_options
	public enum Callroute
	{
		BOX, FON, ASK
	};
	
	/* Settings Keys */
	public static final String PREF_SIPUSER = "username";
	public static final String PREF_SIPUSER_DEFAULT = "620";
	public static final String PREF_SIPPASS = "password";
	public static final float PREF_EARGAIN_DEFAULT = (float)0.25; // not a pref any more
	public static final String PREF_AUTOON = "auto_on";
	public static final String PREF_AUTOONDEMAND = "auto_ondemand";
	public static final String PREF_CALLROUTE = "callroute";
	public static final Callroute PREF_CALLROUTE_DEFAULT = Callroute.BOX;
	public static final String PREF_RINGTONE = "sipringtone";

	public static final String PREF_HMICGAIN = "hmicgain";
	public static final String PREF_MICGAIN = "micgain";	
	public static final String PREF_NODATA = "nodata";
	public static final String PREF_SETMODE = "setmode";
	public static final String PREF_OLDVALID = "oldvalid";	
	public static final String PREF_OLDVIBRATE = "oldvibrate";
	public static final String PREF_OLDVIBRATE2 = "oldvibrate2";
	public static final String PREF_OLDPOLICY = "oldpolicy";
	public static final String PREF_HEARGAIN = "heargain";
	public static final String PREF_EARGAIN = "eargain";
	public static final String PREF_OLDRING = "oldring";
	
	public static final boolean	DEFAULT_NODATA = false;	
	public static final boolean	DEFAULT_OLDVALID = false;	
	public static final boolean	DEFAULT_SETMODE = false;
	public static final int		DEFAULT_OLDVIBRATE = 0;
	public static final int		DEFAULT_OLDVIBRATE2 = 0;
	public static final int		DEFAULT_OLDPOLICY = 0;
	public static final int		DEFAULT_OLDRING = 0;
	public static final float	DEFAULT_EARGAIN = (float) 0.25;
	public static final float	DEFAULT_MICGAIN = (float) 0.25;
	public static final float	DEFAULT_HEARGAIN = (float) 0.25;
	public static final float	DEFAULT_HMICGAIN = (float) 1.0;

	public static boolean on(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("on",false);
	}
	
	public static void on(Context context,boolean on) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
		edit.putBoolean("on",on);
		edit.commit();
        if (on) Receiver.engine(context).isRegistered();
	}

	public static String getVersion() {
		return getVersion(Receiver.mContext);
	}
	
	public static String getVersion(Context context) {
		final String unknown = "Unknown";
		
		if (context == null) {
			return unknown;
		}
		
		try {
			return context.getPackageManager()
				   .getPackageInfo(context.getPackageName(), 0)
				   .versionName;
		} catch(NameNotFoundException ex) {}
		
		return unknown;		
	}

	public static boolean mBackToMainActivity = false;
	
	public static Callroute getCallRoute(Context context)
	{
		try
		{
			return Enum.valueOf(Callroute.class,
					PreferenceManager.getDefaultSharedPreferences(context)
							.getString(Sipdroid.PREF_CALLROUTE, ""));
		}
		catch(Exception exp) {}
		
		return PREF_CALLROUTE_DEFAULT;
	}
	
	/**
	 * 
	 * Gets port number to use with SIP
	 * <p>
	 * Sipdroid used to read this from preferences, only the
	 * default is used now
	 * @return port to use
	 */
	public static int getPortPref()
	{
		return SipStack.default_port;
	}
	
	/**
	 * Gets current fritzbox uri to use with SIP
	 * @return server uri to use
	 */
	public static String getServerPref()
	{
		return DataHub.getFritzboxUrlWithoutProtocol(Receiver.mContext);
	}
	
	/**
	 * Gets protocol to use with SIP
	 * <p>
	 * Sipdroid used to read this from preferences, now it's
	 * a constant
	 * @return protocol to use
	 */
	public static String getProtocolPref()
	{
		return "udp";
	}
	
	/**
	 * Gets state of MWI setting
	 * <p>
	 * Sipdroid used to read this from preferences. Now it's
	 * a constant, since fritzbox' SIP registrar does not
	 * support it
	 * @return true if MWI has to be activated
	 */
	public static boolean getMWIPref()
	{
		return false;
	}

	public static String getSipUser()
	{
		return PreferenceManager.getDefaultSharedPreferences(Receiver.mContext)
				.getString(PREF_SIPUSER, PREF_SIPUSER_DEFAULT);
	}
	
	public static float getMicGain() {
		if (Receiver.headset > 0) {
			return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(PREF_HMICGAIN, "" + DEFAULT_HMICGAIN));
		}

		return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(PREF_MICGAIN, "" + DEFAULT_MICGAIN));
	}

	/**
	 * Helper to get ear gain preference value
	 * @return ear gain preference value
	 */
	public static float getEarGain() {
		try {
			return Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getString(Receiver.headset > 0 ? PREF_HEARGAIN : PREF_EARGAIN, "" + DEFAULT_EARGAIN));
		} catch (NumberFormatException i) {
			return DEFAULT_EARGAIN;
		}			
	}

	
}
