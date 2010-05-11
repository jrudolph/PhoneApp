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
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import de.avm.android.fritzapp.com.soap.GetCallList;
import de.avm.android.fritzapp.com.soap.GetCountWLANDevices;
import de.avm.android.fritzapp.com.soap.GetPhonebook;
import de.avm.android.fritzapp.com.soap.GetPhonebookList;
import de.avm.android.fritzapp.com.soap.GetWLANStatus;
import de.avm.android.fritzapp.model.CallLog;
import de.avm.android.fritzapp.model.PhoneBook;
import de.avm.android.fritzapp.model.WLANInfo;

public class DataHub implements IData 
{
	private static String mDiscoveredFritzboxUrl = "";

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.IData#getPhoneBookList(android.content.Context)
	 */
	public String[] getPhoneBookList(Context c) {
		GetPhonebookList soap = new GetPhonebookList(c);
		return soap.getQualifiedResult();
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.IData#getPhoneBook(java.lang.String, android.content.Context)
	 */
	public PhoneBook getPhoneBook(String id, Context c) {
		GetPhonebook soap = new GetPhonebook(id, c);
		return soap.getQualifiedResult();
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.IData#getAllPhoneBooks(android.content.Context)
	 */
	public PhoneBook[] getAllPhoneBooks(Context c) {
		String[] list = this.getPhoneBookList(c);
		PhoneBook[] phoneBooks = new PhoneBook[list.length];
		int i = 0;
		for (String key : list) {
			phoneBooks[i] = this.getPhoneBook(key, c);
			i++;
		}
		return phoneBooks;
	}

	/**
	 * @return Gibt null zurück falls keine Infos von der Fritz!Box gefunden wurden mit denen das Handy verbunden ist.
	 */
	public WLANInfo getWLANInfo(Context c) {
		
		GetCountWLANDevices countSoap = new GetCountWLANDevices(c);
		int count = countSoap.getQualifiedResult();
		
		WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String macOfWifiStation = wifiInfo.getMacAddress();
		
		// Geht alle gefundenen Devices durch und holt die Daten vom richigen (anhand der MacAdresse)
		for (int i = 0; i < count; i++) {
			WLANInfo result = new GetWLANStatus(c, i, macOfWifiStation).getQualifiedResult();
			if(result != null) {
				return result;
			}
		}
		return null;
	}
	
	/**
	 * Gets the first (main) phone book of the fritzbox.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the main phone book
	 */
	public PhoneBook getSinglePhoneBook(Context c) {
		return getAllPhoneBooks(c)[0];
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.IData#getCallLog(android.content.Context)
	 */
	public CallLog getCallLog(Context c) {
		GetCallList soap = new GetCallList(c);
		return soap.getQualifiedResult();
	}

	/**
	 * Gets info whether getFritzboxUrl() delivers defaulted/discovered
	 * or configured Url
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return true if fritzbox url is defaulted or discovered (empty in configuration)
	 */
	public static boolean isFritzboxUrlDefaulted(Context c)
	{
		SharedPreferences mySharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(c);
		return mySharedPreferences.getString(IData.PREF_SITE, "").length() == 0;
	}
	
	/**
	 * Gets the fritzbox url from settings.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the fritzbox url
	 */
	public static String getFritzboxUrl(Context c)
	{
		SharedPreferences mySharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(c);
		String url = mySharedPreferences.getString(IData.PREF_SITE, "");
		if (url.length() > 0)
		{
			if (url.indexOf("://") < 0) url = "http://" + url;
			return url; // configured url
		}
		
		if (mDiscoveredFritzboxUrl.length() > 0)
			return mDiscoveredFritzboxUrl; // previously set with setFritzboxUrl
		
		return IData.DEFAULT_SITE; // defaulted
	}

	/**
	 * Sets the discovered fritzbox url
	 * 
	 * @param adress
	 *            the adress to set the url to
	 * @param c
	 *            a valid context
	 */
	public static void setFritzboxUrl(String adress, Context c)
	{
		mDiscoveredFritzboxUrl = adress;
	}

	/**
	 * Gets the fritzbox url without protocol.
	 * 
	 * @param c
	 *            the c
	 * 
	 * @return the fritzbox url without protocol
	 */
	public static String getFritzboxUrlWithoutProtocol(Context c) {
		String url = getFritzboxUrl(c);
		int pos;
		if ((pos = url.indexOf("//")) >= 0) {
			return url.substring(pos + 2);
		}
		return url;
	}

	/**
	 * Gets the fritzbox password from settings.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the fritzbox pass
	 */
	public static String getFritzboxPass(Context c) {
		SharedPreferences mySharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(c);
		String pass = mySharedPreferences.getString(
				IData.PREF_PASS, "");
		if (pass == null) {
			return "";
		}
		return pass;
	}
}