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

package de.avm.android.fritzapp.com.soap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.avm.android.fritzapp.util.error.DataMisformatException;

import android.content.Context;

/* Soap implementation for the phonebooklist-Interface */
public class GetPhonebookList extends AbstractSoapHelper<String[]> {

	/**
	 * Instantiates a new gets the phonebook list.
	 * 
	 * @param c
	 *            the c
	 */
	public GetPhonebookList(Context c) {
		super(c);
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getQualifiedResult()
	 */
	@Override
	public String[] getQualifiedResult()
	{
		String input = getSoapBody();
		Matcher m = Pattern.compile("NewPhonebookList>(.*?)<\\/NewPhonebookList")
				.matcher(input);
		if (m.find())
			return m.group(1).split(",");
		else
			throw new DataMisformatException("Invalid Response from PhoneBook Service");
	}


	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getSoapMethod()
	 */
	@Override
	public String getSoapMethod() {
		return "GetPhonebookList";
	}
}