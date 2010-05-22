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

package de.avm.android.fritzapp.util;

import java.util.List;

import de.avm.android.fritzapp.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * List adapter for call route exceptions
 */
public class CallRouteExceptionsAdapter extends ArrayAdapter<String>
{
    private LayoutInflater mLayoutInflater;
    private int mTextColor;

	/**
	 * Constructs list adapter
	 * 
	 * @param context
	 *            The current context.
	 * @param objects
	 *            The objects to represent in the ListView.
	 */
	public CallRouteExceptionsAdapter(Context context, List<String> objects)
	{
		super(context, android.R.id.text1,
				android.R.layout.simple_list_item_1, objects);
        mLayoutInflater = (LayoutInflater)context
        		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTextColor = context.getResources().getColor(R.color.FRITZBlueText);
	}

	/* (non-Javadoc)
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
        if (convertView == null)
        {
        	View view = mLayoutInflater.inflate(
        			android.R.layout.simple_list_item_1, parent, false);
        	TextView textView = (TextView)view.findViewById(android.R.id.text1);
            textView.setTextColor(mTextColor);
            textView.setText(getItem(position));
            return view;
        }

        ((TextView)convertView.findViewById(android.R.id.text1))
        		.setText(getItem(position));
		return convertView;
	}
}
