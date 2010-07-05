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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import de.avm.android.fritzapp.com.ConnectivityChangeReceiver;
import de.avm.android.fritzapp.com.DataHub;
import de.avm.android.fritzapp.com.ComSettingsChecker.CONNECTION_PROBLEM;
import de.avm.android.fritzapp.sipua.UserAgent;
import de.avm.android.fritzapp.sipua.ui.Caller;
import de.avm.android.fritzapp.sipua.ui.Receiver;
import de.avm.android.fritzapp.sipua.ui.RegisterService;
import de.avm.android.fritzapp.sipua.ui.Sipdroid;
import de.avm.android.fritzapp.util.CallRouteExceptions;
import de.avm.android.fritzapp.util.PhoneNumberHelper;
import de.avm.android.fritzapp.util.ResourceHelper;

/*
 * Main GUI.
 */
public class FRITZApp extends Activity
{
	private static final String TAG = "FRITZApp";

	private static final int COLUMS_LANDSCAPE = 4;

	private static final String CONNECTION_STATE = "conn_state";
	private static final String DIALPAD_INPUT = "dialpad";
	private static final String PREF_FIRSTRUN = "firstrun";
	
	private Dialpad mDialpad = null;
	private static AlertDialog mAlertDlg = null;
	private StatusDisplayHandler mStatusDisplayHandler = null;
	private AskCallrouteHandler mAskCallrouteHandler = new AskCallrouteHandler();
	
	private boolean mOverrideClirOnce = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// manage settings
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); 
		boolean isFirstRun = prefs.getBoolean(PREF_FIRSTRUN, true);
		if (isFirstRun)
		{
			Editor edit = prefs.edit();
			edit.putBoolean(PREF_FIRSTRUN, false);
			edit.commit();
		}
		SettingsActivity.prepareSettings(this, isFirstRun);
		
		GridView grid = (GridView) findViewById(R.id.DashBoard);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			grid.setNumColumns(COLUMS_LANDSCAPE);
		}
		grid.setAdapter(new StartButtonAdapter());
		grid.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					GridView gridView = (GridView) v;
					View view = gridView.getSelectedView();
					if (view != null) {
						ImageButton button = (ImageButton) view
								.findViewWithTag("launcher");
						if (button != null) {
							return button.performClick();
						}
					}
				}
				return false;
			}
		});

		if (GLOBAL.mStatus == null)
		{
			GLOBAL.mStatus = new ComStatus(this); 
			// wenn wir wiederbelebt werden den Status beibehalten.
			if (savedInstanceState != null)
				GLOBAL.mStatus.set(savedInstanceState.getBundle(CONNECTION_STATE));
		}

		// disconnect message from other threads
		if (GLOBAL.mShowDisconnectedHandler != null)
			GLOBAL.mShowDisconnectedHandler =
				new GLOBAL.ShowDisconnectedHandler(getApplicationContext());
		
		if (GLOBAL.mConnectivityChangeReceiver == null)
		{
			// start receiving and handling ConnectivityManager's
			// android.net.conn.CONNECTIVITY_CHANGE
			GLOBAL.mConnectivityChangeReceiver =
					new ConnectivityChangeReceiver(new Handler(), new Runnable()
			{
				public void run()
				{
					new CheckConnectionTask().execute(false);
					Receiver.engine(FRITZApp.this).onWifiChanged();
				}
			});
			GLOBAL.mConnectivityChangeReceiver.Register(this);
		}

		// status display
		mStatusDisplayHandler = new StatusDisplayHandler(this);
		
		// don't send unhandled exceptions via email
		//ExceptionHandler.register(this, FRITZAppEMailExeptionHandler.class);
		
		// initially try to connect to fritzbox
		new CheckConnectionTask().execute(true);

		// Telefon
		if (GLOBAL.mWifiSleepPolicy == -1)
			GLOBAL.mWifiSleepPolicy = setWifiSleepPolicy(this,
					Settings.System.WIFI_SLEEP_POLICY_NEVER);
		Sipdroid.on(this,true);
		
		// Dialpad
		mDialpad = (Dialpad)findViewById(R.id.dtmf_dialer);
		if (savedInstanceState != null)
			mDialpad.setText(savedInstanceState.getString(DIALPAD_INPUT));
		mDialpad.setInitiallyOpen(true);
		final ImageButton btnDialpad = ((ImageButton)findViewById(R.id.SlideDialpad));
		btnDialpad.setImageResource(R.drawable.btn_dialpadclose);
		mDialpad.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener()
		{
			public void onDrawerClosed()
			{
				btnDialpad.setImageResource(R.drawable.btn_dialpadopen);
			}
		});
		mDialpad.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener()
		{
			public void onDrawerOpened()
			{
				btnDialpad.setImageResource(R.drawable.btn_dialpadclose);
			}
		});

		// Footer Buttons
		btnDialpad.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				mDialpad.animateToggle();
			}
		});
		((Button)findViewById(R.id.CallNow)).setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				callNow();
			}
		});
		((ImageButton)findViewById(R.id.Redial)).setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				mDialpad.loadRedial(FRITZApp.this);
			}
		});
		
		// call once after install
		if (isFirstRun)
		{
			// show introduction
			showHelp(this);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();

		// Telefon
		if (!Receiver.engine(this).isRegistered())
			Receiver.engine(this).register();
		Receiver.setAskCallrouteHandler(mAskCallrouteHandler);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		// show in call screen if call is active
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE) Receiver.moveTop();

		// status display
		GLOBAL.mStatus.addStatusChangedHandler(mStatusDisplayHandler);
		mStatusDisplayHandler.run();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();

		// status display
		GLOBAL.mStatus.removeStatusChangedHandler(mStatusDisplayHandler);
	}
	
	@Override
	protected void onDestroy()
	{
		if (GLOBAL.mConnectivityChangeReceiver != null)
		{
			try { GLOBAL.mConnectivityChangeReceiver.Unregister(); }
			catch(Exception exp) { }
			GLOBAL.mConnectivityChangeReceiver = null;
		}
		
		super.onDestroy();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.findItem(R.id.Clir)
				.setTitle((PhoneNumberHelper.isClir(this) == mOverrideClirOnce) ?
							R.string.menu_clir_on : R.string.menu_clir_off);
		return true;
		
	}
	
	/**
	 * Sets WifiSleepPolicy
	 * @param wifiSleepPolicy value to set
	 * @return value before setting it
	 */
	private static int setWifiSleepPolicy(Context context, int wifiSleepPolicy)
	{
        ContentResolver contentResolver = context.getContentResolver();
		int oldValue = Settings.System.getInt(contentResolver,
				Settings.System.WIFI_SLEEP_POLICY, -1);

		if ((wifiSleepPolicy > -1) && (wifiSleepPolicy != oldValue))
		{
			Settings.System.putInt(contentResolver,
					Settings.System.WIFI_SLEEP_POLICY, wifiSleepPolicy);
		}
		
		return oldValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.Clir:
				mOverrideClirOnce = !mOverrideClirOnce;
				if (mOverrideClirOnce)
					Toast.makeText(this, (PhoneNumberHelper.isClir(this)) ?
							R.string.hint_clir_off_once : R.string.hint_clir_on_once,
							Toast.LENGTH_SHORT).show();
				break;
		
			case R.id.Settings: 
				startActivity(new Intent(this, SettingsActivity.class));
				break;
	
			case R.id.About:
				showAbout(this);
				break;
				
			case R.id.Help:
				showHelp(this);
				break;
				
			case R.id.Exit:
				shutdown(this);
				finish();
				break;
		}
		return true;
	}

	public static void showAbout(Context context)
	{
		TextDialog.create(context,
				context.getString(R.string.menu_about),
				context.getString(R.string.about_text).replace("\\n","\n")
				.replace("${VERSION}", Sipdroid.getVersion(context))).show();
	}

	public static void showHelp(final Context context)
	{
		TextDialog.create(context,
				context.getString(R.string.menu_help),
				context.getString(R.string.intro_text))
				.setNegativeButton(R.string.settings_label,
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						Intent i = new Intent(context, SettingsActivity.class);
						context.startActivity(i);
					}
				}).show();

	}
	
	public static void shutdown(Context context)
	{
		Receiver.reRegister(0);
		Receiver.engine(context).unregister();
		try
		{
			Thread.sleep(2000);
		}
		catch (InterruptedException e1)
		{
		}
		Sipdroid.on(context, false);
		Receiver.engine(context).halt();
		Receiver.mSipdroidEngine = null;
		Receiver.setAskCallrouteHandler(null);
		context.stopService(new Intent(context, RegisterService.class));
		GLOBAL.mStatus.clear();
		setWifiSleepPolicy(context, GLOBAL.mWifiSleepPolicy);
		GLOBAL.mWifiSleepPolicy = -1;
		if (GLOBAL.mConnectivityChangeReceiver != null)
		{
			try { GLOBAL.mConnectivityChangeReceiver.Unregister(); }
			catch(Exception exp) { }
			GLOBAL.mConnectivityChangeReceiver = null;
		}
	}

	/**
	 * Background Task to check if the connection to the FRITZ!Box can be established.
	 * 
	 */
	private class CheckConnectionTask extends
			AsyncTask<Boolean, Integer, CONNECTION_PROBLEM>
	{
		private boolean showMessage;
		private String mCurrentFritzboxUrl = "";

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			mCurrentFritzboxUrl = DataHub.getFritzboxUrl(getBaseContext());
			Log.d(TAG, String.format("CheckConnectionTask.onPreExecute() url=\"%s\"",
					mCurrentFritzboxUrl));
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected CONNECTION_PROBLEM doInBackground(Boolean... showMessageParms)
		{
			showMessage = showMessageParms[0];
			CONNECTION_PROBLEM prob = ComSettingsChecker
					.checkStartUpConnection(getBaseContext());
			if (prob != CONNECTION_PROBLEM.NO_PROBLEM)
			{
				GLOBAL.mStatus.setConn((prob.isError()) ?
						ComStatus.CONN_AWAY : ComStatus.CONN_NOTFOUND, "");
			}
			else
			{
				GLOBAL.mStatus.setConn(ComStatus.CONN_AVAILABLE, DataHub.getFritzboxUrl(getBaseContext()));
				GLOBAL.mStatus.setTr064Level(ComSettingsChecker.getTr064Level());
			}
			return prob;
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(CONNECTION_PROBLEM problem)
		{
			super.onPostExecute(problem);
			Log.d(TAG, String.format("CheckConnectionTask.onPostExecute() - %s",
					problem.toString()));

			if (problem == CONNECTION_PROBLEM.NO_PROBLEM)
			{
				String currentFritzboxUrl = DataHub.getFritzboxUrl(getBaseContext());
				if (!mCurrentFritzboxUrl.equals(currentFritzboxUrl))
				{
					// url has been changed while checking connection
					// (discovered using SSDP)
					Log.d(TAG, String.format("  url has been changed to \"%s\".",
							currentFritzboxUrl));
					Receiver.engine(FRITZApp.this).updateDNS();
					Receiver.engine(FRITZApp.this).halt();
			    	Receiver.engine(FRITZApp.this).StartEngine();
				}
			}
			else if (showMessage && problem.isError())
			{
				showTextBox(problem);
			}
		}
	}

	/**
	 * Shows a text box to the user in case of a connection problem.
	 * 
	 * @param problem
	 *            the problem
	 * 
	 */
	protected void showTextBox(ComSettingsChecker.CONNECTION_PROBLEM problem) {

		if (problem == CONNECTION_PROBLEM.NO_PROBLEM) {
			return;
		}

		// WLAN Einstellungen müssen außerhalb der FRITZ!App richtig gestellt
		// werden
		boolean showEinstellungen = true;
		if (problem == CONNECTION_PROBLEM.WLAN_DISCONNECT
				|| problem == CONNECTION_PROBLEM.WLAN_OFF) {
			showEinstellungen = false;
		}

		Builder builder = TextDialog.createOk(this,
				ResourceHelper.getTextForConnectionProblem(problem,
						getBaseContext()),
				android.R.drawable.ic_dialog_alert);
		if (showEinstellungen) {
			builder.setNegativeButton(R.string.settings_label,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent i = new Intent(FRITZApp.this,
									SettingsActivity.class);
							startActivity(i);
						}
					});
		}
		builder.show();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBundle(CONNECTION_STATE, GLOBAL.mStatus.getAsBundle());
		outState.putString(DIALPAD_INPUT, ((mDialpad != null) && (mDialpad.isOpened())) ?
				mDialpad.getText() : "");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// Dialpad
		boolean handled = (mDialpad == null) ?
				false : mDialpad.onKey(null, keyCode, event);
		if (!handled)
		{
			switch(keyCode)
			{
				case KeyEvent.KEYCODE_BACK:
		            moveTaskToBack(true);
		            return true;
		        case KeyEvent.KEYCODE_CALL:
					callNow();
					return true;
			}
		}
		return (handled) ? true : super.onKeyDown(keyCode, event);
	}
	
	
	/**
	 * Für das Grid der Launcherbuttons auf dem Einstiegsscreen
	 * 
	 * @author am
	 * 
	 */
	protected class StartButtonAdapter extends BaseAdapter {
		protected Class<Activity>[] allActivities;
		protected View[] buttons;

		// Einträge im Startgrid in der gewünschten Reihenfolge
		/**
		 * Instantiates a new start button adapter.
		 */
		@SuppressWarnings("unchecked")
		public StartButtonAdapter() {
			allActivities = new Class[] { CallLogActivity.class,
					PhoneBookActivity.class, FeaturesActivity.class };
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getCount()
		 */
		public int getCount() {
			return allActivities.length;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItem(int)
		 */
		public Object getItem(int pos) {
			// das Grid arbeitet in falscher Reihenfolge für unsere Darstellung
			return allActivities[pos];
		}

		/* (non-Javadoc)
		 * @see android.widget.BaseAdapter#hasStableIds()
		 */
		@Override
		public boolean hasStableIds() {
			return true;
		}

		/*
		 * baut den View für einen Launcherbutton zusammen
		 */
		/* (non-Javadoc)
		 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			Drawable icon = null;
			String label = null;
			RelativeLayout activityLauncher = null;
			if (convertView == null) {
				activityLauncher = (RelativeLayout) View.inflate(FRITZApp.this,
						R.layout.t_launcherbutton, null);
			} else {
				activityLauncher = (RelativeLayout) convertView;
			}
			Class<Activity> currActivityClass = (Class<Activity>) allActivities[position];
			icon = ResourceHelper.getIconForClass(currActivityClass, true,
					FRITZApp.this);
			label = ResourceHelper.getLabelForClass(currActivityClass,
					FRITZApp.this);
			ImageButton button = (ImageButton) activityLauncher
					.findViewById(R.id.launcher_button);
			TextView text = (TextView) activityLauncher
					.findViewById(R.id.launcher_text);
			button.setImageDrawable(icon);
			text.setText(label, TextView.BufferType.NORMAL);
			button.setOnClickListener(new OnClickStartActivityIntent(
					currActivityClass));
			return activityLauncher;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return position;
		}
	}

	void callNow()
	{
		callNow((mDialpad == null) ? "" : mDialpad.getText(), true, true);
		mOverrideClirOnce = false;
	}
	
	void callNow(String number, boolean allowFallback, boolean fromDialpad)
	{
		if (mAlertDlg != null) mAlertDlg.cancel();
		int errorMessage = 0;
		
		if (number.length() == 0)
		{
			errorMessage = R.string.empty;
			allowFallback = false;
		}
		else if (fromDialpad && CallRouteExceptions.isException(this, number))
		{
			// call route exception -> Mobile only
			mAskCallrouteHandler.fallbackCallroute(number, 0);
		}
		else if ((GLOBAL.mStatus == null) ||
			(GLOBAL.mStatus.getSip() != ComStatus.SIP_AVAILABLE))
		{
			errorMessage = (GLOBAL.mStatus.isConnected()) ?
					R.string.regno : R.string.problem_wlan_disconnect;
		}
		else 
		{
			Sipdroid.mBackToMainActivity = true;
			if (Receiver.engine(this).call(PhoneNumberHelper.decorateNumber(this,
					(fromDialpad) ? mOverrideClirOnce : false,
					PhoneNumberHelper.fixInternationalDialingPrefix(number))))
			{
				if (fromDialpad)
					mDialpad.saveTextAsRedial(this);
				else
					Dialpad.saveAsRedial(this, number);
			}
			else errorMessage = R.string.problem_wlan_disconnect;
		}

		if (errorMessage > 0)
		{
			if (allowFallback)
			{
				// retry via Mobile
				mAskCallrouteHandler.fallbackCallroute(number, errorMessage);
			}
			else
			{
				// show error message
				mAlertDlg = TextDialog.createOk(this,
						getString(errorMessage))
						.show();
			}
		}
	}

	/**
	 * Handler for process user request in main thread
	 */
	public class AskCallrouteHandler extends Handler
	{
		private static final String PARAM_ASK = "ask";
		private static final String PARAM_NUMBER = "number";
		private static final String PARAM_ERRORID = "resid";

		/**
		 * Request user feedback on fallback to mobile network
		 * @param number number to call
		 * @param errorResId error message on failed calling via box
		 * @return true if request will be shown
		 */
		public boolean fallbackCallroute(String number, int errorResId)
		{
			Bundle param = new Bundle();
			param.putBoolean(PARAM_ASK, false);
			param.putString(PARAM_NUMBER, number);
			param.putInt(PARAM_ERRORID, errorResId);
			Message message = Message.obtain();
			message.setData(param);
			
			return sendMessage(message);
		}
		
		/**
		 * Request user feedback on call routing
		 * @param number number to call
		 * @return true if request will be shown
		 */
		public boolean askCallroute(String number)
		{
			Bundle param = new Bundle();
			param.putBoolean(PARAM_ASK, true);
			param.putString(PARAM_NUMBER, number);
			Message message = Message.obtain();
			message.setData(param);
			
			return sendMessage(message);
		}

		@Override
		public void handleMessage (Message message)
		{
			try
			{
				if (mAlertDlg != null) mAlertDlg.cancel();
				// TODO bring task to front
//				boolean hadFocus = FRITZApp.this.hasWindowFocus();
//				Log.d("FRITZApp.AskCallrouteHandler", "hadFocus == " + Boolean.toString(hadFocus));
//				if (!hadFocus)
//					FRITZApp.this.startActivity(new Intent(FRITZApp.this, FRITZApp.class));

				if (message.getData().getBoolean(PARAM_ASK))
					handleAskCallroute(message.getData().getString(PARAM_NUMBER));
				else
					handleFallback(message.getData().getString(PARAM_NUMBER),
							message.getData().getInt(PARAM_ERRORID));
//				if (!hadFocus) moveTaskToBack(true);
			}
			catch (Exception exp) { }
		}

		private void handleAskCallroute(final String number)
		{
			// ask for call route
			mAlertDlg = new AlertDialog.Builder(FRITZApp.this)
					.setTitle(R.string.callroute_dialogtitle)
					.setCancelable(false)
					.setItems(R.array.callroute_optiontitles,
							new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog,
								int which)
						{
							String option = FRITZApp.this.getResources()
									.getStringArray(R.array.settings_callroute_options)[which];
							if (option.equals(Sipdroid.Callroute.FON.toString()))
							{
								Dialpad.saveAsRedial(FRITZApp.this, number);
								try
								{
									Caller.sendCallIntent(FRITZApp.this, number);
								}
								catch (Exception exp)
								{
									exp.printStackTrace();
									// couldn't send intent to route to mobile -> try to use box
									callNow(number, false, false);
								}
							}
							else if (option.equals(Sipdroid.Callroute.BOX.toString()))
							{
								callNow(number, true, false);
							}
							dialog.dismiss();
						}
					})
					.show();
		}
		
		private void handleFallback(final String number, final int resId)
		{
			if (resId == 0)
			{
				// no error to show -> just fall back
				Dialpad.saveAsRedial(FRITZApp.this, number);
				try
				{
					Caller.sendCallIntent(FRITZApp.this, number);
				}
				catch (Exception exp)
				{
					exp.printStackTrace();
				}
				return;
			}
			
			// ask for fallback
			mAlertDlg = TextDialog.create(FRITZApp.this,
					FRITZApp.this.getString(R.string.app_name),
					String.format(FRITZApp.this.getString(R.string.callroute_fallback),
							FRITZApp.this.getString(resId)))
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							Dialpad.saveAsRedial(FRITZApp.this, number);
							try
							{
								Caller.sendCallIntent(FRITZApp.this, number);
							}
							catch (Exception exp)
							{
								exp.printStackTrace();
								// couldn't send intent to fall back -> show error message
								mAlertDlg = TextDialog.createOk(FRITZApp.this,
										FRITZApp.this.getString(resId))
										.show();
							}
							dialog.dismiss();
						}
					})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					})
					.show();
		}
	}
}
