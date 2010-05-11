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

import java.util.Timer;
import java.util.TimerTask;

import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.gui.ComStatus;
import de.avm.android.fritzapp.gui.Dialpad;
import de.avm.android.fritzapp.util.PhoneNumberHelper;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class Caller extends BroadcastReceiver
{
	// used by android's phone
	private static final String EXTRA_ALREADY_CALLED = "android.phone.extra.ALREADY_CALLED";
    private static final String EXTRA_ORIGINAL_URI = "android.phone.extra.ORIGINAL_URI";

	/**
	 * to ignore own call intents, blacklist the number for some seconds
	 */
	private static Blacklist mBlacklist = new Blacklist();
	
	/**
	 * Sends intent to launch phone call which will be ignored by Caller instances
	 * @param context context to send from
	 * @param target phone number
	 */
	public static void sendCallIntent(Context context, String number)
			throws ActivityNotFoundException
	{
		// emergency calls cannot be invoked by ACTION_CALL!
		String action = (PhoneNumberUtils.isEmergencyNumber(number)) ?
				Intent.ACTION_DIAL : Intent.ACTION_CALL;
		
		// escape and filter
		number = number.replace("+", "%2b").replace("#", "");
		
		// send intent
		Uri uri = Uri.parse("tel:" + number);
		mBlacklist.add(uri.toString());
		context.startActivity(new Intent(action, null).setData(uri));
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
        String intentAction = intent.getAction();
        String number = getResultData();

		// ignore intents resent by this app after rejected by user
		// in Sipdroid.Callroute.ASK configuration
		// (in mBlacklist)
        if (intentAction.equals(Intent.ACTION_NEW_OUTGOING_CALL) && (number != null) &&
        	!mBlacklist.test(intent.getStringExtra(EXTRA_ORIGINAL_URI)) &&
        	!intent.getBooleanExtra(EXTRA_ALREADY_CALLED, false)) 
        {
        	if (!Sipdroid.release) Log.i("SipUA:","outgoing call");

    		Log.d("Caller", number);

            boolean invertOption = number.endsWith("+");
            if (invertOption) number = number.substring(0, number.length() - 1);

			boolean isSipCall = false;
			if (Sipdroid.on(context) &&
    			(GLOBAL.mStatus != null) && (GLOBAL.mStatus.getSip() == ComStatus.SIP_AVAILABLE))
			{
    			switch (Sipdroid.getCallRoute(context))
    			{
	    			case FON:
	    				isSipCall = invertOption;
	    				break;
    			
	    			case BOX:
	    				isSipCall = !invertOption;
	    				break;
	    				
	    			case ASK:
	    				if ((Receiver.mAskCallrouteHandler != null) &&
	    					Receiver.mAskCallrouteHandler.askCallroute(number))
        					setResultData(null); // we do it
    					else
    						setResultData(number); // we can't do it
	    				return;
    			}
			}
			if (!isSipCall || !Sipdroid.on(context) ||
				((GLOBAL.mStatus != null) && (GLOBAL.mStatus.getSip() != ComStatus.SIP_AVAILABLE)))
			{
				setResultData(number);
			} 
			else if (number != null)
			{
				if (Receiver.engine(context).call(PhoneNumberHelper
						.fixInternationalDialingPrefix(number)))
				{
					setResultData(null);
					Dialpad.saveAsRedial(context, number);
				}
				else if (Receiver.mAskCallrouteHandler != null)
				{
					// fallback to mobile
					Receiver.mAskCallrouteHandler.fallbackCallroute(number,
							R.string.problem_wlan_disconnect);
				}
            }
        }
    }

	/**
	 * to ignore own call intents, blacklist the number for some seconds
	 */
	private static class Blacklist
	{
		private static final long HOLD_IN_LIST_MILLIS = 4000;
		private String[] mNumbers = new String[10];

		public Blacklist()
		{
			int len = mNumbers.length;
			for(int ii = 0; ii < len; ii++) mNumbers[ii] = null;
		}
		
		public synchronized void add(String number)
		{
			int len = mNumbers.length;
			for(int ii = 0; ii < len; ii++)
			{
				if (mNumbers[ii] == null)
				{
					mNumbers[ii] = number;
					Log.d("Caller.Blacklist",
							String.format("added [%d] %s", ii, number));
					final int added = ii;
					new Timer().schedule(new TimerTask()
					{
						public void run()
						{
							synchronized(Blacklist.this)
							{
								Log.d("Caller.Blacklist",
										String.format("free [%d] %s", added,
												mNumbers[added]));
								mNumbers[added] = null;
							}
						}
					}, HOLD_IN_LIST_MILLIS);
					break;
				}
			}
		}
		
		public synchronized boolean test(String number)
		{
			if (number != null)
			{
				int len = mNumbers.length;
				for(int ii = 0; ii < len; ii++)
				{
					if ((mNumbers[ii] != null) &&
						mNumbers[ii].equals(number))
					{
						Log.d("Caller.Blacklist",
								String.format("test hit [%d] %s", ii,
										number));
						mNumbers[ii] = "";
						return true;
					}
				}
			}
			return false;
		}
	}
}
