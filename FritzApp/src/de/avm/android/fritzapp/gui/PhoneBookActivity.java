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

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.ListView;
import android.widget.TextView;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import de.avm.android.fritzapp.com.DataHub;
import de.avm.android.fritzapp.com.IData;
import de.avm.android.fritzapp.model.Contact;
import de.avm.android.fritzapp.model.ContactNumber;
import de.usbi.android.util.adapter.ArrayAdapterExt;
import de.usbi.android.util.adapter.OnClickStartActivity;

/* GUI for the phonebbok (list of contacts)*/
public class PhoneBookActivity extends Activity implements OfflineActivity {

	public static final String EXTRA_CONTACT_DATA_KEY = "CONTACT_DATA";

	private IData fritzBox = new DataHub();

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.phonebook);

		try
		{
			// TODO: Nur Phonebook 0 oder ???
			// Lade die Daten von der FRITZ!Box
			//PhoneBook[] phoneBooks = fritzBox.getAllPhoneBooks(this);
	
			// Alle Kontakte in eine Liste laden. Am Anfang soll es nur ein
			// Telefonbuch geben.
			ArrayList<Contact> contacts = new ArrayList<Contact>();
			//for (PhoneBook pb : phoneBooks) {
			//	contacts.addAll(pb.getContacts());
			//}
			contacts.addAll(fritzBox.getPhoneBook("0", this).getContacts());

			// ListView erstellen
			final ListView listView = (ListView) findViewById(R.id.ListViewContent);
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
			listView.setAdapter(new PhoneBookAdapter(contacts));
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
			TextDialog.create(this, getString(R.string.app_name),
					getString(R.string.soap_tranfer_failed))
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							PhoneBookActivity.this.finish();
						}
					})
					.show();
		}
	}

	/**
	 * Adapter der die Kontakte der Liste zur Verfügung stellt.
	 */
	private class PhoneBookAdapter extends ArrayAdapterExt<Contact> {

		/**
		 * Instantiates a new phone book adapter.
		 * 
		 * @param contacts
		 *            the contacts
		 */
		public PhoneBookAdapter(ArrayList<Contact> contacts) {
			addEntries(contacts);
		}

		/* (non-Javadoc)
		 * @see de.usbi.android.util.adapter.ArrayAdapterExt#populateView(java.lang.Object, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View populateView(final Contact item, View view, ViewGroup viewGroup) {
			// Kann bei Bedarf noch mit einem ViewWrapper optimiert werden, um
			// die findViewById Aufrufe zu verringern. Die Referenzen werden
			// gespeichert und beim Neu-Befüllen dann direkt verwendet, ohne
			// die Felder nochmal zu suchen.
			// Siehe auch:
			// http://www.androidguys.com/2008/07/22/fancy-listviews-part-three/

			if (view == null) {
				view = View.inflate(getBaseContext(), R.layout.t_contactlistitem, null);
			}

			((TextView) view.findViewById(R.id.ContactName)).setText(item.getRealName());

			// Hauptnummer anzeigen, falls vorhanden
			ContactNumber hauptnummer = item.getHauptnummer();
			if(hauptnummer != null) {
				String numberToShowString = hauptnummer.getType().getTextResource(getBaseContext()) + ": " + hauptnummer.getNumber();
				((TextView) view.findViewById(R.id.ContactNumber)).setText(numberToShowString);
			}

			// Click Listener (Ein alter ClickListener wird überschrieben falls vorhanden.)
			view.setOnClickListener(new OnClickStartActivity(ContactDetailsActivity.class, item, EXTRA_CONTACT_DATA_KEY));

			return view;
		}
	}

	public static Intent showIntent(Context context)
	{
		if (GLOBAL.mStatus.isConn() &&
			(GLOBAL.mStatus.getTr064Level() >= ComSettingsChecker.TR064_BASIC))
		{
			return new Intent(context, PhoneBookActivity.class);
		}
		// show system's contacts if no connection or old Fritzbox
		return (new Intent(Intent.ACTION_VIEW, People.CONTENT_URI))
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
	}
	
	public static Boolean canShow()
	{
		return GLOBAL.mStatus.isConn() &&
			(GLOBAL.mStatus.getTr064Level() >= ComSettingsChecker.TR064_BASIC);
	}
}
