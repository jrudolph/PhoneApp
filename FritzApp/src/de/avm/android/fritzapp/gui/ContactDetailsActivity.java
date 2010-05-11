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
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.model.Contact;
import de.avm.android.fritzapp.model.ContactNumber;
import de.usbi.android.util.adapter.ArrayAdapterExt;
import de.usbi.android.util.adapter.OnClickCallNumber;

/* GUI for a single contact (from the phonebook) */
public class ContactDetailsActivity extends Activity {

	private Contact contact;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		contact = getIntent().getParcelableExtra(
				PhoneBookActivity.EXTRA_CONTACT_DATA_KEY);
		setContentView(R.layout.contactdetails);

		TextView name = (TextView) findViewById(R.id.Name);
		name.setText(contact.getRealName());

		ImageView favorit = (ImageView) findViewById(R.id.FavoriteIcon);
		if (contact.getCategory() == Contact.CATEGORY_WICHTIG) {
			favorit.setImageDrawable(getResources().getDrawable(android.R.drawable.btn_star_big_on));
		} else {
			favorit.setImageDrawable(getResources().getDrawable(android.R.drawable.btn_star_big_off));
		}

		ListView liste = (ListView) findViewById(R.id.ContactNumbers);
		liste.setAdapter(new ContactNumberListAdapter(contact.getContactNumbers()));
	}

	public class ContactNumberListAdapter extends ArrayAdapterExt<ContactNumber> {
		
		public ContactNumberListAdapter(ArrayList<ContactNumber> numbers) {
			addEntries(numbers);
		}

		/* (non-Javadoc)
		 * @see de.usbi.android.util.adapter.ArrayAdapterExt#populateView(java.lang.Object, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View populateView(final ContactNumber item, View view,
				ViewGroup viewGroup) {
			if (view == null) {
				view = View.inflate(getBaseContext(), R.layout.t_contacnumbertlistitem, null);
			}

			TextView numberView = (TextView) view.findViewById(R.id.ContactNumber);
			numberView.setText(item.getNumber());

			TextView numberType = (TextView) view.findViewById(R.id.ContactNumberType);
			
			String anrufenTemplate = getResources().getString(R.string.contact_details_callX);
			numberType.setText(String.format(anrufenTemplate, item.getType().getTextResource(getBaseContext())));
			
			// Anrufen bei Klick
			view.setOnClickListener(new OnClickCallNumber(item.getNumber()));
			
			return view;
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
	}
}