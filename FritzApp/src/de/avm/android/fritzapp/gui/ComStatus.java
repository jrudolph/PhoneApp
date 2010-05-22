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

package de.avm.android.fritzapp.gui;

import java.util.LinkedList;

import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * Overall connection status and status notifications
 */
public class ComStatus
{
	private static final String TAG = "ComStatus";
	
	public static final int CONN_NOTFOUND = 0;
	public static final int CONN_AWAY = 1;
	public static final int CONN_AVAILABLE = 2;
	
	public static final int SIP_NOTREGISTERED = 0;
	public static final int SIP_AWAY = 1;
	public static final int SIP_IDLE = 2;
	public static final int SIP_AVAILABLE = 3;
	
	public static final int ID = 1;

	private Context mContext;
	private Handler mHandler = new Handler();
	private LinkedList<Runnable> mStatusChangedHandlers = new LinkedList<Runnable>(); 
	
	private Notification mNotification = null;

	// for info and TR-064
	private int mConnState = CONN_NOTFOUND;
//	private String mHost = "";
	private int mTr064Level = ComSettingsChecker.TR064_NONE;
	
	// for SIP client
	private int mSipState = SIP_NOTREGISTERED;
//	private String mSipError = "";

	public ComStatus(Context context)
	{
		Log.d(TAG, "ComStatus.ComStatus()");
		mContext = context.getApplicationContext();
	}

	public synchronized Bundle getAsBundle()
	{
		Bundle bundle = new Bundle();
		bundle.putInt("ConnState", mConnState);
//		bundle.putString("Host", mHost);
		bundle.putInt("Tr064Level", mTr064Level);
		return bundle;
	}
	
	public synchronized void set(Bundle bundle)
	{
		if (bundle != null)
		{
			mConnState = bundle.getInt("ConnState", CONN_NOTFOUND);
//			mHost = bundle.getString("Host");
			mTr064Level = bundle.getInt("Tr064Level", ComSettingsChecker.TR064_NONE);
			update();
			onStatusChanged();
		}
	}
	
	public synchronized void clear()
	{
		mConnState = CONN_NOTFOUND;
		mTr064Level = ComSettingsChecker.TR064_NONE;
		mSipState = SIP_NOTREGISTERED;
		update();
		onStatusChanged();
	}
	
	public synchronized boolean isConnected()
	{
		return (mConnState == CONN_AVAILABLE) || (mSipState == SIP_AVAILABLE);
	}
	
	/**
	 * @return box' connection status
	 */
	public int getConn()
	{
		return mConnState;
	}
	
	/**
	 * @return true if box has been connected recently
	 */
	public boolean isConn()
	{
		return mConnState == CONN_AVAILABLE;
	}
	
	/**
	 * Sets box' connection status
	 * @param conn box' connection status
	 * @param host box connected
	 */
	public synchronized void setConn(int conn, String host)
	{
		if ((mConnState != conn) /*|| !mHost.equalsIgnoreCase(host)*/)
		{
			Log.d(TAG, String.format("new conn state: %s (%s)",
					Integer.toString(conn), host));
			mConnState = conn;
//			mHost = host;
			if (conn != CONN_AVAILABLE)
			{
				mTr064Level = ComSettingsChecker.TR064_NONE;
//				mHost = "";
			}
			update();
			onStatusChanged();
		}
	}
	
	/**
	 * Gets SIP connection state
	 * @return ComStatus.SIP_*
	 */
	public int getSip()
	{
		return mSipState;
	}
	
	public synchronized void setSip(int sipState, String error)
	{
		if (mSipState != sipState)
		{
			Log.d(TAG, String.format("new sip state: %s (%s)",
					Integer.toString(sipState), error));
			mSipState = sipState;
//			mSipError = error;
			update();
			onStatusChanged();
		}
	}
	
	/**
	 * Gets TR-064 support level of recently found box
	 * @return ComSettingsChecker.TR064_*
	 */
	public synchronized int getTr064Level()
	{
		return (isConnected()) ? mTr064Level : ComSettingsChecker.TR064_NONE;
	}
	
	/**
	 * Sets TR-064 support level of recently found box
	 * @param support level as ComSettingsChecker.TR064_*
	 */
	public synchronized void setTr064Level(int level)
	{
		if (mTr064Level != level)
		{
			Log.d(TAG, "new tr064 level: " + Integer.toString(level));
			mTr064Level = level;
			onStatusChanged();
		}
	}
	
	public synchronized void addStatusChangedHandler(Runnable runnable)
	{
		if (!mStatusChangedHandlers.contains(runnable))
			mStatusChangedHandlers.add(runnable);
	}
	
	public synchronized void removeStatusChangedHandler(Runnable runnable)
	{
		if (mStatusChangedHandlers.contains(runnable))
			mStatusChangedHandlers.remove(runnable);
	}
	
	private void update()
	{
		if (!isConnected())
		{
			if (mNotification != null)
			{
				Log.d(TAG, "remove notification");
				NotificationManager notificationManager =
						((NotificationManager)mContext
								.getSystemService(Context.NOTIFICATION_SERVICE));
				notificationManager.cancel(ID);
				mNotification = null;
			}
			return;
		}

		if (mNotification == null)
		{
			Log.d(TAG, "create new notification");
			mNotification = new Notification(R.drawable.state_connected,
					mContext.getResources().getText(R.string.connection_on).toString(),
					System.currentTimeMillis());
			mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		}

		String message;
		switch(mSipState)
		{
			case SIP_AWAY:
				mNotification.icon = R.drawable.state_sipaway;
				mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
				mNotification.ledARGB = 0xffff0000; /* red */
				mNotification.ledOnMS = 125;
				mNotification.ledOffMS = 2875;
				message = mContext.getResources().getText(R.string.regfailed).toString(); 
				break;
				
			case SIP_IDLE:
				mNotification.icon = R.drawable.state_sipidle;
				mNotification.flags &= ~Notification.FLAG_SHOW_LIGHTS;
				message = mContext.getResources().getText(R.string.reg).toString();
				break;
				
			case SIP_AVAILABLE:
				mNotification.icon = R.drawable.state_sipavailable;
				mNotification.flags &= ~Notification.FLAG_SHOW_LIGHTS;
				message = mContext.getResources().getText(R.string.regok).toString();
				break;

			case SIP_NOTREGISTERED:
			default:
				mNotification.icon = R.drawable.state_connected;
				mNotification.flags &= ~Notification.FLAG_SHOW_LIGHTS;
				message = mContext.getResources().getText(R.string.regno).toString();
				break;
		}
		
		Log.d(TAG, "update notification: " + message);
		mNotification.setLatestEventInfo(mContext,
				mContext.getResources().getText(R.string.app_name).toString(),
				message, PendingIntent.getActivity(mContext, 0, new Intent(mContext,
						FRITZApp.class), 0));
		NotificationManager notificationManager =
			((NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE));
		notificationManager.notify(ID, mNotification);
	}
	
	private void onStatusChanged()
	{
		for (Runnable runnable : mStatusChangedHandlers)
			mHandler.post(runnable);
	}
}
