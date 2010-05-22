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

package de.avm.android.fritzapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import de.avm.android.fritzapp.com.ConnectivityChangeReceiver;
import de.avm.android.fritzapp.gui.ComStatus;
import de.avm.android.fritzapp.gui.TextDialog;
import de.avm.android.fritzapp.util.ResourceHelper;
public class GLOBAL {

	// TODO Package Name bestimmen: "de.avm.android.fritzapp"
	public final static String BASE_PACKAGE = "de.avm.android.fritzapp";
	// public final static String BASE_PACKAGE =
	// GLOBAL.class.getPackage().getName();

	// Checkt bei der Kommunikation nicht ob WLAN aktiv ist etc., damit man auch
	// aus der Entwicklungsumgebung testen kann.
	public static boolean DEBUG_NO_COM_CHECK = false;

	public static ComStatus mStatus = null;
	public static ConnectivityChangeReceiver mConnectivityChangeReceiver = null;
	public static ShowDisconnectedHandler mShowDisconnectedHandler = null;

	// original wifi sleep policy to restore on exit
	public static int mWifiSleepPolicy = -1;
	
	/**
	 * Handler Show message when disconnected.
	 */
	public static class ShowDisconnectedHandler extends Handler
	{
		private static final String PARAM = "problem";
		private Context mContext;
 
		/**
		 * Create instance in GUI thread
		 * @param context context to access resources
		 */
		public ShowDisconnectedHandler(Context context)
		{
			mContext = context;
		}
		
		/**
		 * Show disconnected message
		 * @param problem problem caused disconnect
		 * @return
		 */
		public boolean postProblem(
				ComSettingsChecker.CONNECTION_PROBLEM problem)
		{
			Bundle param = new Bundle();
			param.putString(PARAM, problem.toString());
			Message message = Message.obtain();
			message.setData(param);
			
			return sendMessage(message);
		}

		@Override
		public void handleMessage (Message message)
		{
			try
			{
				ComSettingsChecker.CONNECTION_PROBLEM problem =
					Enum.valueOf(ComSettingsChecker.CONNECTION_PROBLEM.class,
							message.getData().getString(PARAM));

				if (mStatus != null)
					mStatus.setConn((problem.isError()) ?
							ComStatus.CONN_AWAY : ComStatus.CONN_NOTFOUND, "");

				TextDialog.createOk(mContext,
						ResourceHelper.getTextForConnectionProblem(problem,
								mContext),
						android.R.drawable.ic_dialog_alert)
						.show();
			}
			catch (Exception exp) { }
		}
	}
}