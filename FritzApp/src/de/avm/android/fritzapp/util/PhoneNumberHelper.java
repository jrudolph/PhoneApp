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

/**
 * Helpers to be used with phone numbers
 */
public class PhoneNumberHelper
{
	private static final String[] mCountries = new String[] { "DE", "AT", "CH", null };
	private static final String[] mInternationalPrefix = new String[] { "49", "43", "41" };
	private static final String mReplacement = "0";
	
	/**
	 * Stripps international dialing prefix with "+" for national calls in
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
}
