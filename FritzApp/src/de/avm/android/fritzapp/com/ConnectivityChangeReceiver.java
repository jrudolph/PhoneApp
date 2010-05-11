/* 
 * Copyright 2010 by AVM GmbH <info@avm.de>
 *
 * This software contains free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License ("License") as 
 * published by the Free Software Foundation  (version 3 of the License). 
 * This software is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the copy of the 
 * License you received along with this software for more details.
 */

package de.avm.android.fritzapp.com;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

/**
 * Starts an object with Runnable interface on receiving
 * android.net.conn.CONNECTIVITY_CHANGE
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver
{
	// only one action to filter (for more, change skipping sticky intent!!)
	private static final String ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
	
	private Context mContext = null;
	private Handler mHandler;
	private Runnable mRunnable;
	private Intent mStickyIntent = null;

	/**
	 * @param handler
	 *            Handler for thread where runnable will be executed
	 * @param runnable
	 *            Runnable executed when android.net.conn.CONNECTIVITY_CHANGE
	 *            has been received
	 */
	public ConnectivityChangeReceiver(Handler handler, Runnable runnable)
	{
		mHandler = handler;
		mRunnable = runnable;
	}

	/**
	 * Register this receiver
	 * 
	 * @param context
	 *            Context the receiver has to be registered for
	 * @return The first sticky intent found that matches filter, or null if
	 *         there are none.
	 */
	public Intent Register(Context context)
	{
		mContext = context;
		mStickyIntent = context.registerReceiver(this,
				new IntentFilter(ACTION));
		return mStickyIntent;
	}

	/**
	 * Unregister this receiver
	 */
	public void Unregister()
	{
		if (mContext != null)
		{
			mContext.unregisterReceiver(this);
			mContext = null;
		}
	}
	
	/**
	 * Executes Runnable unconditional
	 */
	public void kick()
	{
		mHandler.post(mRunnable);
	}

	/**
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 *      android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ACTION))
		{
			if (mStickyIntent == null)
				mHandler.post(mRunnable);
			else
				mStickyIntent = null; // skip sticky intent 
		}
	}
}
