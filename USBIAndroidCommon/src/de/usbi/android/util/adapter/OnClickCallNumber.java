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

import android.content.Intent;
import android.net.Uri;
import android.view.View;

/**
 * ClickListener der bei Click die übergebene Nummer anruft.
 */
public class OnClickCallNumber implements View.OnClickListener {

	protected final String normalizedNumber;
	
	// Pattern mit den Zeichen einer validen Nummer.
	// (public damit sichtbar für den Test.)
	public final static String PATTERN = "[^0-9+]";

	public OnClickCallNumber(String number) {
		// Nummer darf nur noch + und 0-9 enthalten
		this.normalizedNumber = number.replaceAll(PATTERN, "");
	}
	
	public OnClickCallNumber(int number) {
		// Wenn es ein int ist, ist die Nummer schon normalisiert.
		this.normalizedNumber = number + "";
	}

	public void onClick(View v) {
		Intent intent = new Intent(Intent.ACTION_CALL);
		intent.setData(Uri.parse("tel:" + this.normalizedNumber));
		v.getContext().startActivity(intent);
	}
}
