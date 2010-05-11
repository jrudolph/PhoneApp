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

package de.usbi.android.util.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.View;

/**
 * ClickListener der eine neue Activity startet.
 * 
 * Bei Bedarf kann er ein Parceable mitsenden.
 */
public class OnClickStartActivity implements View.OnClickListener {

	protected final Class<? extends Activity> activity;
	
	private Parcelable parcelable = null;
	private String parcelableName = null;

	public OnClickStartActivity(Class<? extends Activity> activity) {
		this.activity = activity;
	}
	
	public OnClickStartActivity(Class<? extends Activity> activity, Parcelable parcelable, String parcelableName) {
		this.activity = activity;
		this.parcelable = parcelable;
		this.parcelableName = parcelableName;
	}

	public void onClick(View v) {
		Context c = v.getContext();
		
		Intent i = new Intent(c, activity);
		
		if (this.parcelable != null && this.parcelableName != null && this.parcelableName.length() != 0) {
			i.putExtra(this.parcelableName, this.parcelable);
		}
		
		c.startActivity(i);
	}
}
