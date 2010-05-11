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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import de.avm.android.fritzapp.com.DataHub;
import de.avm.android.fritzapp.com.IData;
import de.avm.android.fritzapp.model.WLANInfo;

/* GUI to show WLAN quality */
public class WlanTesterActivity extends Activity implements OfflineActivity {

	private IData fritzBox = new DataHub();

	private TextView bandwidthValueTextView;
	private TextView bandwidthValueBar;
	private TextView signalValueTextView;
	private TextView signalValueBar;
	
	// Gesamtbreite der Skala, in der der Wert aufgetragen wird.
	// TODO: Bestimmen aus Bildschirmbreite, wegen verschiedenen Auflösungen und Landscape Mode, damit die Bar nicht zu klein ist.
	// Breite der BarContainer dann im onCreate setzen.
	private static final int BAR_GLOBAL_WIDTH = 180;
	
	// Mindest-Wert damit man was sieht
	private static final int BAR_MIN_VALUE = 5;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupViews();
		updateData();

		final Button updateButton = (Button) findViewById(R.id.WLANStart);
		updateButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateData();
			}
		});
	}
	
	/**
	 * Setup views for the activity.
	 */
	void setupViews() {
		setContentView(R.layout.wlantester);

		// Signal
		ViewStub signalStub = (ViewStub) findViewById(R.id.signalStub);
		RelativeLayout signalView = (RelativeLayout) signalStub.inflate();
		
		TextView signalName = (TextView) signalView.findViewById(R.id.ValueName);
		signalName.setText(R.string.wlan_name_signal);
		
		signalValueTextView = (TextView) signalView.findViewById(R.id.Value);
		signalValueBar = (TextView) signalView.findViewById(R.id.ValueBar);
		//RelativeLayout signalValueBarContainer = (RelativeLayout) signalView.findViewById(R.id.ValueBarContainer);
		
		// Bandwidth
		ViewStub bandwidthStub = (ViewStub) findViewById(R.id.bandwidthStub);
		RelativeLayout bandwidthView = (RelativeLayout) bandwidthStub.inflate();
		
		TextView bandwidthName = (TextView) bandwidthView.findViewById(R.id.ValueName);
		bandwidthName.setText(R.string.wlan_name_bandwidth);
		
		bandwidthValueTextView = (TextView) bandwidthView.findViewById(R.id.Value);
		bandwidthValueBar = (TextView) bandwidthView.findViewById(R.id.ValueBar);
		//RelativeLayout bandwidthValueBarContainer = (RelativeLayout) bandwidthView.findViewById(R.id.ValueBarContainer);
	}
	
	private void updateData() {
		// Zeige an das Daten geladen werden
		signalValueTextView.setText(R.string.wlan_getting_data);
		bandwidthValueTextView.setText(R.string.wlan_getting_data);
		
		// Setzt die Wert-Balken zurück
		bandwidthValueBar.setWidth(BAR_MIN_VALUE);
		signalValueBar.setWidth(BAR_MIN_VALUE);

		// Hole Daten von der Fritz!Box
		new FetchWLANDataTask(this).execute();
	}
	
	private void updateViewWithFritzBoxData(WLANInfo info) {
		/*
		 * Keine WLANInfo gefunden, das kann eigentlich nicht vorkommen, solange
		 * das Handy mit dem WLAN verbunden sein muss. Beim Burchsuchen der
		 * WLANs wurde anhand der Mac Adresse das WLAN nicht gefunden, mit dem
		 * das Handy verbunden ist...
		 */
		if(info == null) {
			signalValueTextView.setText(R.string.wlan_error_no_wlan_found);
			bandwidthValueTextView.setText(R.string.wlan_error_no_wlan_found);
			return;
		}
		
		// Signal
		int signalStrength = info.getSignalStrength();
		signalValueTextView.setText(signalStrength + "/" + WLANInfo.SIGNAL_STRENGTH_RANGE);
		Double signalStrengthFraction = ((double)signalStrength / WLANInfo.SIGNAL_STRENGTH_RANGE) * BAR_GLOBAL_WIDTH;
		signalValueBar.setWidth(Math.max(signalStrengthFraction.intValue(), BAR_MIN_VALUE));
		
		// Bandwidth
		int bandwidth = info.getBandwidth();
		bandwidthValueTextView.setText(bandwidth + "/" + WLANInfo.SPEED_RANGE);
		Double bandwidthFraction = ((double)bandwidth / WLANInfo.SPEED_RANGE) * BAR_GLOBAL_WIDTH;
		bandwidthValueBar.setWidth(Math.max(bandwidthFraction.intValue(), BAR_MIN_VALUE));
	}
	
	private class FetchWLANDataTask extends
			AsyncTask<Integer, Integer, WLANInfo> {

		private final Context context;
		public FetchWLANDataTask(Context c) {
			this.context = c;
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected WLANInfo doInBackground(Integer... params)
		{
			try
			{
				return fritzBox.getWLANInfo(this.context);
			}
			catch(Exception exp)
			{
				exp.printStackTrace();
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		protected void onProgressUpdate(Integer... progress) {
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		protected void onPostExecute(WLANInfo info) {
			updateViewWithFritzBoxData(info);
		}
	}

	public static Intent showIntent(Context context)
	{
		return (GLOBAL.mStatus.getConn() &&
				(GLOBAL.mStatus.getTr064Level() >= ComSettingsChecker.TR064_BASIC)) ?
				new Intent(context, WlanTesterActivity.class) : null;
	}
	
	public static Boolean canShow()
	{
		return GLOBAL.mStatus.getConn() &&
			(GLOBAL.mStatus.getTr064Level() >= ComSettingsChecker.TR064_BASIC);
	}
}
