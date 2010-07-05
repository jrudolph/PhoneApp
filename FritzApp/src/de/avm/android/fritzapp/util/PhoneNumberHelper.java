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

import java.util.Locale;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Helpers to be used with phone numbers
 */
public class PhoneNumberHelper
{
	public static final String PREF_CLIR = "clir";
	public static final boolean DEFAULT_CLIR = false;
	private static final String CLIR_ON = "*31#";
	private static final String CLIR_OFF = "#31#";
	
	private static final String[] mCountries = new String[] { "DE", "AT", "CH", null };
	private static final String[] mInternationalPrefix = new String[] { "49", "43", "41" };
	private static final String mReplacement = "0";
	private static final String VALID_CHAR = "+0123456789*#";
	
	/**
	 * Strips international dialing prefix with "+" for national calls in
	 * some countries 
	 * @param number
	 * @return fixed number
	 */
	public static String fixInternationalDialingPrefix(String number)
	{
		if (number.startsWith("+"))
		{
			String whereAmI = Locale.getDefault().getCountry();
			for (int ii = 0; mCountries[ii] != null; ii++)
				if (mCountries[ii].equalsIgnoreCase(whereAmI) &&
					number.substring(1).startsWith(mInternationalPrefix[ii]))
				{
					number = mReplacement +
							number.substring(1 + mInternationalPrefix[ii].length());
					break;
				}
		}
		return number;
	}
	
	/**
	 * Strips invalid characters from phone number
	 * @param number
	 * @return
	 */
	public static String stripSeparators(String number)
	{
		StringBuilder builder = new StringBuilder(number.length());
		char[] chars = number.toCharArray();
		for(char ch : chars)
			if (VALID_CHAR.indexOf(ch) >= 0)
				builder.append(ch);
		return builder.toString();
	}
	
	/**
	 * Gets CLIR preference
	 * @param context context to access saved preferences
	 * @return CLIR preference
	 */
	public static boolean isClir(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context) 
				.getBoolean(PREF_CLIR, DEFAULT_CLIR);
	}
	
	/**
	 * Adds prefixes to phone number according to settings
	 * @param context context to access saved preferences
	 * @param number
	 * @return decorated number
	 */
	public static String decorateNumber(Context context, String number)
	{
		if (isClir(context))
			return CLIR_ON + number;
		return number;
	}
	
	/**
	 * Adds prefixes to phone number according to settings
	 * @param context context to access saved preferences
	 * @param override true to invert preference
	 * @param number
	 * @return decorated number
	 */
	public static String decorateNumber(Context context, boolean override, String number)
	{
		if (isClir(context) != override)
			return CLIR_ON + number;
		return number;
	}
	
	/**
	 * Strips known prefixes from beginning of phone number
	 * @param number possibly decorated phone number
	 * @return number phone number
	 */
	public static String stripNumber(String number)
	{
		for (int ii = 0; true;)
		{
			if (number.startsWith(CLIR_ON, ii))
			{
				ii += CLIR_ON.length();
			}
			else if (number.startsWith(CLIR_OFF, ii))
			{
				ii += CLIR_OFF.length();
			}
			else
			{
				number = number.substring(ii);
				break;
			}
		}
		return number;
	}
}
