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

import java.util.ArrayList;
import java.util.Collection;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * Call route exceptions container, serialized to settings
 */
public class CallRouteExceptions extends ArrayList<String>
{
	private static final long serialVersionUID = -8551037649455779796L;
	private static final String PREF = "cre";
	private static final String DEFAULT = "110;112";
	private static final String PREF_DELIMITER = ";";
	
	private SharedPreferences mPreferences;
	private OnChangedListener mListener = null;

	public interface OnChangedListener
	{
		void onChanged();
	};
	
	/**
	 * Constructs instance and loads content from app's preferences 
	 * @param context
	 */
	public CallRouteExceptions(Context context)
	{
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String[] entries = mPreferences.getString(PREF, "").split(PREF_DELIMITER);
		for(String entry : entries)
			if (entry.length() > 0) super.add(entry);
	}
	
	public void setOnChangedListener(OnChangedListener listener)
	{
		mListener = listener;
	}

	/**
	 * Saves content to app's preferences
	 */
	public void save()
	{
		StringBuilder builder = new StringBuilder();
		for(String entry : this)
		{
			if (builder.length() > 0) builder.append(PREF_DELIMITER);
			builder.append(entry);
		}
		Editor editor = mPreferences.edit();
		editor.putString(PREF, builder.toString());
		editor.commit();
	}
	
	public static void saveDefault(Context context)
	{
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
				.edit();
		editor.putString(PREF, DEFAULT);
		editor.commit();
	}
	
	/**
	 * Checks if numer matches a list entry
	 * @param number
	 * @return
	 */
	public static boolean isException(Context context, String number)
	{
		// do we have a SIM card to use?
		int simState = ((TelephonyManager)context
				.getSystemService(Context.TELEPHONY_SERVICE)).getSimState();
		if ((simState == TelephonyManager.SIM_STATE_READY) ||
			(simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED))
		{
			String cmpNumber = PhoneNumberHelper.stripSeparators(number);
			CallRouteExceptions cre = new CallRouteExceptions(context);
			for(String entry : cre)
				if (cmpNumber.startsWith(PhoneNumberHelper.stripSeparators(entry)))
					return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(String pattern)
	{
		if (super.add(pattern))
		{
			if (mListener != null) mListener.onChanged();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int location, String pattern)
	{
		super.add(location, pattern);
		if (mListener != null) mListener.onChanged();
	}
	
	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(int, java.util.Collection)
	 */
	@Override
	public boolean addAll(int location, Collection<? extends String> collection)
	{
		if (super.addAll(location, collection))
		{
			if (mListener != null) mListener.onChanged();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends String> collection)
	{
		if (super.addAll(collection))
		{
			if (mListener != null) mListener.onChanged();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#set(int, java.lang.Object)
	 */
	@Override
	public String set(int location, String pattern)
	{
		String old = super.set(location, pattern);
		if (mListener != null) mListener.onChanged();
		return old;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(int)
	 */
	@Override
	public String remove(int location)
	{
		String old = super.remove(location);
		if (mListener != null) mListener.onChanged();
		return old;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object object)
	{
		if (super.remove(object))
		{
			if (mListener != null) mListener.onChanged();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#removeRange(int, int)
	 */
	protected void removeRange(int start, int end)
	{
		super.removeRange(start, end);
		if (mListener != null) mListener.onChanged();

	}
	
	/* (non-Javadoc)
	 * @see java.util.ArrayList#clear()
	 */
	@Override
	public void clear()
	{
		if (size() > 0)
		{
			super.clear();
			if (mListener != null) mListener.onChanged();
		}
	}
}
