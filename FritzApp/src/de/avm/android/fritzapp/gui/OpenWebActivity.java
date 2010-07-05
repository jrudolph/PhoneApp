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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import de.avm.android.fritzapp.com.DataHub;
import de.avm.android.fritzapp.com.JasonBoxinfo;
import de.avm.android.fritzapp.util.InetAddressHelper;

/*
 * Activity to show info about connected box and to
 * open a browser with the FRITZ!Box configuration-menu
 */
public class OpenWebActivity extends Activity implements OfflineActivity
{
	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart()
	{
		super.onStart();
		setContentView(R.layout.boxinfo);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		GLOBAL.mStatus.addStatusChangedHandler(mStatusChangedHandler);
		mStatusChangedHandler.run();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		GLOBAL.mStatus.removeStatusChangedHandler(mStatusChangedHandler);
	}

	private Runnable mStatusChangedHandler = new Runnable()
	{
		public void run()
		{
			onUpdateDisplay();
		}
	};

	/**
	 * @return true if info available
	 */
	private void onUpdateDisplay()
	{
		JasonBoxinfo boxInfo = ComSettingsChecker.getJasonBoxinfo();
		if (boxInfo != null)
		{
			// box info
			((TextView)findViewById(R.id.Name)).setText(boxInfo.getName());
			((TextView)findViewById(R.id.Version)).setText(boxInfo.getVersion());
			if (boxInfo.getLab().length() > 0)
			{
				TextView text = ((TextView)findViewById(R.id.Lab));
				text.setText(boxInfo.getLab());
				text.setVisibility(View.VISIBLE);
				((TextView)findViewById(R.id.LabLabel))
						.setVisibility(View.VISIBLE);
			}
			else
			{
				findViewById(R.id.LabLabel).setVisibility(View.GONE);
				findViewById(R.id.Lab).setVisibility(View.GONE);
			}
			
			// my IP
			String locationIP = ComSettingsChecker.getLocationIP();
			if (locationIP.indexOf(':') >= 0)
			{
				// IPv6 -> show IPv4 too
				try
				{
					locationIP += "\n(" + 
							InetAddressHelper.getByName(DataHub
									.getFritzboxUrlWithoutProtocol(this))
									.getHostAddress()
							+ ")";
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			((TextView)findViewById(R.id.Address)).setText(locationIP);
			
			// connected SSID
			WifiInfo wifiInfo = ((WifiManager)getSystemService(Context.WIFI_SERVICE))
					.getConnectionInfo();
			String ssid = (wifiInfo == null) ? "" : wifiInfo.getSSID();
			if ((ssid != null) && (ssid.length() > 0))
			{
				TextView text = ((TextView)findViewById(R.id.Ssid));
				text.setText(ssid);
				text.setVisibility(View.VISIBLE);
				findViewById(R.id.SsidLabel).setVisibility(View.VISIBLE);
			}
			else
			{
				findViewById(R.id.Ssid).setVisibility(View.GONE);
				findViewById(R.id.SsidLabel).setVisibility(View.GONE);
			}

			findViewById(R.id.NoBoxinfoView).setVisibility(View.GONE);
			findViewById(R.id.BoxinfoView).setVisibility(View.VISIBLE);
		}
		else
		{
			findViewById(R.id.BoxinfoView).setVisibility(View.GONE);
			findViewById(R.id.NoBoxinfoView).setVisibility(View.VISIBLE);
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    getMenuInflater().inflate(R.menu.boxinfo_menu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		Uri uri = null;
		try
		{
			uri = Uri.parse(DataHub.getFritzboxUrl(getBaseContext()));
		}
		catch(Exception exp)
		{ }

		menu.findItem(R.id.WebUI).setEnabled(uri != null);
		return true;
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
			case R.id.WebUI:
				try
				{
					Uri uri = Uri.parse(DataHub.getFritzboxUrl(getBaseContext()));
					startActivity(new Intent(Intent.ACTION_VIEW , uri));
				}
				catch(Exception exp)
				{ }
				break;
		
			case R.id.About:
				FRITZApp.showAbout(this);
				break;
				
			case R.id.Help:
				FRITZApp.showHelp(this);
				break;
		}
		return true;
	}
	
	public static Intent showIntent(Context context)
	{
		return new Intent(context, OpenWebActivity.class);
	}
	
	public static Boolean canShow()
	{
		return true;
	}
}