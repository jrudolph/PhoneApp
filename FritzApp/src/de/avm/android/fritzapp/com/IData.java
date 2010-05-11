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

package de.avm.android.fritzapp.com;

import android.content.Context;
import de.avm.android.fritzapp.model.CallLog;
import de.avm.android.fritzapp.model.PhoneBook;
import de.avm.android.fritzapp.model.WLANInfo;

/* Interface for FritzApp Communication (HighLevel)*/
public interface IData {

	public final String PREF_SITE = "fritzappURL";
	public final String PREF_PASS = "fritzappENC";
	public final String DEFAULT_SITE = "http://fritz.box";

	/**
	 * Gets the phone book list from the fritzbox.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the phone book list
	 */
	public String[] getPhoneBookList(Context c);

	/**
	 * Gets the phone book from the fritzbox.
	 * 
	 * @param id
	 *            the id
	 * @param c
	 *            a valid context
	 * 
	 * @return the phone book
	 */
	public PhoneBook getPhoneBook(String id, Context c);
	
	/**
	 * Gets the all phone books from the fritzbox
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the all phone books
	 */
	public PhoneBook[] getAllPhoneBooks(Context c);
	
	/**
	 * Gets the call log from the fritzbox
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the call log
	 */
	public CallLog getCallLog(Context c);

	/**
	 * Gets Information about the WLAN Quality from the fritzbox
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the call log
	 */
	public WLANInfo getWLANInfo(Context c); 
	
}