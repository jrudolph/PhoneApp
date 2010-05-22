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
import android.widget.EditText;
import android.widget.TextView;

/**
 *	Wrapper für Dialoge
 */
public class TextDialog
{
	public static final int DEFAULT_TITLE = R.string.app_name; 
	public static final int DEFAULT_MESSAGE_ICON = R.drawable.icon22;
	public static final int DEFAULT_EDIT_ICON = R.drawable.ic_dialog_menu_generic;
		
	/**
	 * Creates builder without buttons
	 * 
	 * @param context
	 * @param titel dialog's title
	 * @param text dialog's message
	 * @return the builder
	 */
	public static AlertDialog.Builder create(Context context,
			String title, String text)
	{
		return create(context, title, text, DEFAULT_MESSAGE_ICON);
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
			String title, String text, int iconId)
	{
	    // AlertDialog with custom view
	    LayoutInflater inflater = (LayoutInflater)context.getApplicationContext()
	    						  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View layout = inflater.inflate(R.layout.text_dialog, null);
		((TextView)layout.findViewById(R.id.message)).setText(text);

		return new AlertDialog.Builder(context)
			.setTitle(title)
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
		return create(context, context.getString(DEFAULT_TITLE), text)
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
		return create(context, context.getString(DEFAULT_TITLE),
				text, iconId)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
					}
				});
	}
	
	/**
	 * Creates builder with default icon and without buttons
	 * for editing Text. In button handlers get text with
	 * findViewById(R.id.message).
	 * 
	 * @param context
	 * @param titel dialog's title
	 * @param text dialog's message
	 * @param inputType input type for text editing
	 * @return the builder
	 */
	public static AlertDialog.Builder createEdit(Context context,
			String title, String initText, int inputType)
	{
		return createEdit(context, title, initText, inputType,
				DEFAULT_EDIT_ICON);
	}

	/**
	 * Creates builder without buttons for editing Text.
	 * In button handlers get text with
	 * findViewById(R.id.message)
	 * 
	 * @param context
	 * @param titel dialog's title
	 * @param text dialog's message
	 * @param inputType input type for text editing
	 * @param iconId dialog title's icon
	 * @return the builder
	 */
	public static AlertDialog.Builder createEdit(Context context,
			String title, String initText, int inputType, int iconId)
	{
	    // AlertDialog with custom view
	    LayoutInflater inflater = (LayoutInflater)context.getApplicationContext()
	    						  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View layout = inflater.inflate(R.layout.textedit_dialog, null);
	    EditText text = (EditText)layout.findViewById(R.id.message);
		text.setInputType(inputType);
		text.setText(initText);

		return new AlertDialog.Builder(context)
			.setTitle(title)
			.setIcon(iconId)
			.setCancelable(true)
			.setInverseBackgroundForced(true)
			.setView(layout);
	}
}
