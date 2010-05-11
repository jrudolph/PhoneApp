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
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.model.Call;
import de.usbi.android.util.adapter.OnClickCallNumber;

/* GUI for Call Details (one call of a calllist) */
public class CallDetailsActivity extends Activity {
	
	private Call call;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		call = getIntent().getParcelableExtra(
				CallLogActivity.EXTRA_CALL_DATA_KEY);
		setContentView(R.layout.calllog_details);

		ImageView type = (ImageView) findViewById(R.id.TypeIcon);
		type.setImageDrawable(call.getType().getIconForCallType(this));

		TextView name = (TextView) findViewById(R.id.CallLogEntryName);
		name.setText(call.getPartnerNameIfEmptyNumber());

		TextView info = (TextView) findViewById(R.id.CallLogEntryInfo);
		info.setText(call.getPrettyDateFull(getBaseContext()) + "\n" + call.getDuration() + " min");
		
		// LinearLayout Placeholder holen und einen View mit Template inflaten und hinzufügen.
		LinearLayout rootLayout = (LinearLayout) findViewById(R.id.Content);
		View numbers = View.inflate(this, R.layout.t_contacnumbertlistitem, null);
		rootLayout.addView(numbers);
		
		// Template befüllen
		TextView numberView = (TextView) numbers.findViewById(R.id.ContactNumber);
		numberView.setText(call.getPartnerNumber());

		TextView numberType = (TextView) numbers.findViewById(R.id.ContactNumberType);
		numberType.setText(R.string.call_log_call_now);
		
		// Nummer anrufen bei Klick
		numbers.setOnClickListener(new OnClickCallNumber(call.getPartnerNumber()));
	}
}