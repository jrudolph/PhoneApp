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

/* 
 * Copyright (C) 2010 AVM GmbH <info@avm.de>
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.LinearLayout.LayoutParams;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.DataHub;
import de.avm.android.fritzapp.com.IData;
import de.avm.android.fritzapp.sipua.ui.Receiver;
import de.avm.android.fritzapp.sipua.ui.SipRingtonePreference;
import de.avm.android.fritzapp.sipua.ui.Sipdroid;

/* GUI for the settings of the app */
public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener
{
	private EditTextPreference mUrlPref = null;
	private EditTextPreference mSipUserPref = null;

	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Rahmenview
		LinearLayout view = (LinearLayout) View.inflate(this,
				R.layout.settings, null);

		// Liste für Preferences
		ListView preferenceView = new ListView(this);
		preferenceView.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		preferenceView.setSelector(R.drawable.list_selector_background);
		preferenceView.setId(android.R.id.list);
		view.addView(preferenceView, 1);

		PreferenceScreen screen = createPreferenceScreen();
		screen.setPersistent(true);
		screen.bind(preferenceView);
		preferenceView.setAdapter(screen.getRootAdapter());

		this.setContentView(view);
		setPreferenceScreen(screen);

		screen.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Creates the preference screen. Use standardframework of android
	 * 
	 * @return the preference screen
	 */
	private PreferenceScreen createPreferenceScreen()
	{
		PreferenceScreen root = getPreferenceManager()
				.createPreferenceScreen(this);

		/* ******* category Box */
		PreferenceCategory group = new PreferenceCategory(this); 
		group.setTitle(getString(R.string.pref_cat_box));
		root.addPreference(group);
		
		// Adresse Box
		mUrlPref = new EditTextPreference(this)
		{
			@Override
			protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
			{
				super.onPrepareDialogBuilder(builder);
				builder.setInverseBackgroundForced(true);
			}
		};
		mUrlPref.setKey(IData.PREF_SITE);
		mUrlPref.setTitle(R.string.pref_address);
		mUrlPref.setDialogTitle(R.string.pref_address);
		mUrlPref.setSummary(DataHub.getFritzboxUrl(this));
		group.addPreference(mUrlPref);

		// Kennwort Box
		final EditTextPreference passPref = new EditTextPreference(this)
		{
			@Override
			protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
			{
				super.onPrepareDialogBuilder(builder);
				builder.setInverseBackgroundForced(true);
			}
		};
		passPref.setKey(IData.PREF_PASS);
		passPref.setTitle(R.string.pref_boxpassword);
		passPref.setDialogTitle(R.string.pref_boxpassword);
		passPref.getEditText().setTransformationMethod(new PasswordTransformationMethod());
		passPref.getEditText().setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		group.addPreference(passPref);
		
		/* ******* category SIP */
		group = new PreferenceCategory(this); 
		group.setTitle(R.string.pref_cat_sip);
		root.addPreference(group);
		
		// Benutzer SIP
		mSipUserPref = new EditTextPreference(this)
		{
			@Override
			protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
			{
				super.onPrepareDialogBuilder(builder);
				builder.setInverseBackgroundForced(true);
			}
		};
		mSipUserPref.setKey(Sipdroid.PREF_SIPUSER);
		mSipUserPref.setTitle(R.string.settings_sipuser);
		mSipUserPref.setDialogTitle(R.string.settings_sipuser);
		mSipUserPref.setSummary(Sipdroid.getSipUser());
		group.addPreference(mSipUserPref);
		
		// Kennwort SIP
		final EditTextPreference sipPass = new EditTextPreference(this)
		{
			@Override
			protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
			{
				super.onPrepareDialogBuilder(builder);
				builder.setInverseBackgroundForced(true);
			}
		};
		sipPass.setKey(Sipdroid.PREF_SIPPASS);
		sipPass.setTitle(R.string.pref_sippassword);
		sipPass.setDialogTitle(R.string.pref_sippassword);
		sipPass.getEditText().setTransformationMethod(new PasswordTransformationMethod());
		sipPass.getEditText().setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		group.addPreference(sipPass);
		
		// Klingelton
		SipRingtonePreference ringtonePref = new SipRingtonePreference(this);
		ringtonePref.setKey(Sipdroid.PREF_RINGTONE);
		ringtonePref.setTitle(R.string.settings_sipringtone);
		ringtonePref.setSummary(R.string.settings_sipringtone2);
		ringtonePref.setRingtoneType(RingtoneManager.TYPE_RINGTONE);
		ringtonePref.setPersistent(false);
		group.addPreference(ringtonePref);
		
		// bevorzugte Anrufart
		ListPreference listPref = new ListPreference(this); 
		listPref.setKey(Sipdroid.PREF_CALLROUTE);
		listPref.setDefaultValue(Sipdroid.PREF_CALLROUTE_DEFAULT.toString());
		listPref.setTitle(R.string.settings_callroute);
		listPref.setSummary(R.string.settings_callroute2);
		listPref.setDialogTitle(R.string.settings_callroute_dialogtitle);
		listPref.setEntries(R.array.settings_callroute_optiontitles);
		listPref.setEntryValues(R.array.settings_callroute_options);
		group.addPreference(listPref);

		return root;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key)
	{
		if (key.equals(IData.PREF_SITE) ||
			key.equals(Sipdroid.PREF_CALLROUTE))
		{
			if (mUrlPref != null)
				mUrlPref.setSummary(DataHub.getFritzboxUrl(this));
			Receiver.engine(this).updateDNS();
		}
		
		if (key.equals(Sipdroid.PREF_SIPUSER) ||
	        key.equals(Sipdroid.PREF_SIPPASS) ||
	        key.equals(IData.PREF_SITE))
		{
			Receiver.engine(this).halt();
	    	Receiver.engine(this).StartEngine();
 	    }
		
		if ((key.equals(IData.PREF_SITE) ||
	         key.equals(IData.PREF_PASS)) &&
	         (GLOBAL.mConnectivityChangeReceiver != null))
		{
			GLOBAL.mConnectivityChangeReceiver.kick();
 	    }

		if (key.equals(Sipdroid.PREF_SIPUSER))
		{
			mSipUserPref.setSummary(Sipdroid.getSipUser());
	 	}
	}
	
	/**
	 * Preparations on settings to do on app's start
	 */
	public static void prepareSettings(Context context)
	{
		// we hide settings from user which where changeable before,
		// so restore default values
		Editor edit = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		
		// "Rufannahme im Betrieb"
		// "Bei eingeschaltetem Bildschirm mit kurzem Vibrationsalarm"
		edit.putBoolean(Sipdroid.PREF_AUTOON, false);				
		
		// "Rufannahme nach Bedarf"
		// "Option in Statusleiste anzeigen"
		edit.putBoolean(Sipdroid.PREF_AUTOONDEMAND, false);

		edit.commit();
	}
	
}
