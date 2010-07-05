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

import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import de.avm.android.fritzapp.com.DataHub;
import de.avm.android.fritzapp.com.IData;
import de.avm.android.fritzapp.model.Call;
import de.avm.android.fritzapp.model.CallLog;
import de.avm.android.fritzapp.model.Call.CALL_TYPE;
import de.usbi.android.util.adapter.ArrayAdapterExt;
import de.usbi.android.util.adapter.OnClickStartActivity;

/* GUI for the callist.*/
public class CallLogActivity extends TabActivity implements OfflineActivity {
	
	public static final String EXTRA_CALL_DATA_KEY = "CALL_DATA";
	public static final int SOAP_FAILED = 1;

	private IData fritzBox = new DataHub();

	/* (non-Javadoc)
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calllog);

		TabHost tabHost = getTabHost();
		CallLog callLog = null;

		try
		{
			// Lade die Daten von der FRITZ!Box
			callLog = fritzBox.getCallLog(getBaseContext());
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		
		// Tabs anlegen
		createTab(callLog, tabHost, null, R.id.ListContentAll);
		createTab(callLog, tabHost, CALL_TYPE.OUTGOING, R.id.ListContent1);
		createTab(callLog, tabHost, CALL_TYPE.INCOMING, R.id.ListContent2);
		createTab(callLog, tabHost, CALL_TYPE.MISSED, R.id.ListContent3);

		if (callLog == null) showDialog(SOAP_FAILED);
	}
	
	/**
	 * Helper-Method. Creates aTab for the Calllist  
	 * 
	 * @param callLogData
	 *            the call log data
	 * @param tabHost
	 *            the tab host
	 * @param callType
	 *            the call type (MISSED, INCOMING, OUTGOING, UNSPECIFIED = all)
	 * @param contentId
	 *            the content id
	 */
	private void createTab(CallLog callLogData, TabHost tabHost, CALL_TYPE callType, int contentId) {
		// Tabs erstellen
		// Tag (erster Parameter): Wird nur verwendet um das Tab intern zu referenzieren und muss gesetzt sein.
		if(callType == null) {
			String tabText = getResources().getString(R.string.call_log_tab_all);
			tabHost.addTab(tabHost.newTabSpec("ALL").setIndicator(tabText).setContent(contentId));
		} else {
			tabHost.addTab(tabHost.newTabSpec(callType.toString()).setIndicator("", callType.getIconForCallType(this)).setContent(contentId));
		}
		
		// Content des Tabs erstellen
		final ListView listView = (ListView) findViewById(contentId);
		listView.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
				{
					View item = listView.getSelectedView();
					if (item != null)
						return item.performClick();
				}
				return false;
			}
		});
		listView.setAdapter((callLogData == null) ?
				null : new CallLogAdapter(callLogData, callType));
	}

	/**
	 * Adapter der die Daten der CallLog der Liste zur Verfügung stellt und es
	 * außerdem ermöglicht nach dem CALL_TYPE zu filtern.
	 */
	private class CallLogAdapter extends ArrayAdapterExt<Call> {

		private final CallLog callLog;
	
		/**
		 * Instantiates a new call log adapter.
		 * 
		 * @param cl
		 *            the calllog
		 * @param filter
		 *            the initial filter
		 */
		public CallLogAdapter(CallLog cl, CALL_TYPE filter) {
			this.callLog = cl;
			this.applyFilter(filter);
		}

		/**
		 * Apply filter to the List (Tab change).
		 * 
		 * @param filter
		 *            the filter
		 */
		private void applyFilter(CALL_TYPE filter) {
			clearEntries();
			
			if (filter == null) {
				// Kein Filter, alle anzeigen
				addEntries(this.callLog.getCalls());
				
			} else {
				// Filtern nach filter
				for (Call c : this.callLog.getCalls()) {
					if (c.getType() != null && c.getType().equals(filter)) {
						addEntry(c);
					}
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.usbi.android.util.adapter.ArrayAdapterExt#populateView(java.lang.Object, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View populateView(final Call item, View view, ViewGroup viewGroup) {
			// Kann bei Bedarf noch mit einem ViewWrapper optimiert werden, um
			// die findViewById Aufrufe zu verringern. Die Referenzen werden
			// gespeichert und beim Neu-Befüllen dann direkt verwendet, ohne
			// die Felder nochmal zu suchen.
			// Siehe auch: http://www.androidguys.com/2008/07/22/fancy-listviews-part-three/
			
			if (view == null) {
				view = View.inflate(getBaseContext(), R.layout.t_callloglistitem, null);
			}
			
			((ImageView) view.findViewById(R.id.TypeIcon)).setImageDrawable(item.getType().getIconForCallType(getBaseContext()));
			
			if(item.getPartnerName().trim().length() == 0) {
				((TextView) view.findViewById(R.id.CallLogEntryName)).setText(item.getPartnerNumber());
				((TextView) view.findViewById(R.id.CallLogEntryNumber)).setText("");
			} else {
				((TextView) view.findViewById(R.id.CallLogEntryName)).setText(item.getPartnerName());
				((TextView) view.findViewById(R.id.CallLogEntryNumber)).setText(item.getPartnerNumber());
			}
			
			((TextView) view.findViewById(R.id.CallLogEntryInfo)).setText(item.getPrettyDateForList(getBaseContext()));
			
			// Click Listener (Ein alter ClickListener wird überschrieben falls vorhanden.)
			view.setOnClickListener(new OnClickStartActivity(CallDetailsActivity.class, item, EXTRA_CALL_DATA_KEY));
			
			return view;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case SOAP_FAILED:
				return TextDialog.create(this, getString(R.string.app_name),
						getString(R.string.soap_tranfer_failed))
						.setPositiveButton(R.string.ok,
								new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int which)
							{
								CallLogActivity.this.finish();
							}
						})
						.create();
		}
		return null;
	}
	
	public static Intent showIntent(Context context)
	{
		if (GLOBAL.mStatus.isConn() &&
			(GLOBAL.mStatus.getTr064Level() >= ComSettingsChecker.TR064_BASIC))
		{
			return new Intent(context, CallLogActivity.class);
		}
		// show system's call log if no connection or old Fritzbox
		return (new Intent(Intent.ACTION_VIEW, android.provider.CallLog.Calls.CONTENT_URI))
				.setType(android.provider.CallLog.Calls.CONTENT_TYPE)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
	}
	
	public static Boolean canShow()
	{
		return GLOBAL.mStatus.isConn() &&
			(GLOBAL.mStatus.getTr064Level() >= ComSettingsChecker.TR064_BASIC);
	}
}
