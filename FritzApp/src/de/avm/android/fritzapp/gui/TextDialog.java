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

import de.avm.android.fritzapp.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 *	Wrapper für Dialoge
 */
public class TextDialog
{
	/**
	 * Creates builder without buttons
	 * 
	 * @param context
	 * @param titel dialog's title
	 * @param text dialog's message
	 * @return the builder
	 */
	public static AlertDialog.Builder create(Context context,
			String titel, String text)
	{
		return create(context, titel, text, R.drawable.icon22);
	}

	/**
	 * Creates builder without buttons
	 * 
	 * @param context
	 * @param titel dialog's title
	 * @param text dialog's message
	 * @param iconId dialog title's icon
	 * @return the builder
	 */
	public static AlertDialog.Builder create(Context context,
			String titel, String text, int iconId)
	{
	    // AlertDialog with custom view
	    LayoutInflater inflater = (LayoutInflater)context.getApplicationContext()
	    						  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View layout = inflater.inflate(R.layout.text_dialog, null);
		((TextView)layout.findViewById(R.id.message)).setText(text);

		return new AlertDialog.Builder(context)
			.setTitle(titel)
			.setIcon(iconId)
			.setCancelable(true)
			.setInverseBackgroundForced(true)
			.setView(layout);
	}

	/**
	 * Creates builder with OK button and app's name as title
	 * 
	 * @param context
	 * @param text dialog's message
	 * @return the builder
	 */
	public static AlertDialog.Builder createOk(Context context,
			String text)
	{
		return create(context, context.getString(R.string.app_name), text)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
					}
				});
	}

	/**
	 * Creates builder with OK button and app's name as title
	 * 
	 * @param context
	 * @param text dialog's message
	 * @param iconId dialog title's icon
	 * @return the builder
	 */
	public static AlertDialog.Builder createOk(Context context,
			String text, int iconId)
	{
		return create(context, context.getString(R.string.app_name),
				text, iconId)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
					}
				});
	}
}
