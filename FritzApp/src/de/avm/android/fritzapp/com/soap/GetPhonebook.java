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

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import de.avm.android.fritzapp.model.PhoneBook;
import de.avm.android.fritzapp.util.error.DataMisformatException;
import de.usbi.android.util.net.LazySSLHttpClientFactory;
import de.usbi.android.util.net.WebUtil;

/* Soap implementation for the phonebook-Interface */
public class GetPhonebook extends AbstractSoapHelper<PhoneBook> {

	private String phoneBookId;

	/**
	 * Instantiates a new gets the phonebook.
	 * 
	 * @param phoneBookid
	 *            the phone bookid
	 * @param c
	 *            the c
	 */
	public GetPhonebook(String phoneBookid, Context c) {
		super(c);
		this.phoneBookId = phoneBookid;
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getQualifiedResult()
	 */
	@Override
	public PhoneBook getQualifiedResult() {
		String input = getSoapBody();
		String url = null;
		//TODO der Namen des Telefonbuchs erscheint noch nirgends
		// Matcher m = Pattern.compile("NewPhonebookName>(.*?)<").matcher(input);
		 
		Matcher m = Pattern.compile("NewPhonebookURL>(.*?)<").matcher(input);
		if (m.find()) {
			url = m.group(1);
			// Die URL enthält ein &amp;
			url = url.replaceAll("&amp;", "&");
			return new PhoneBook(WebUtil.getContentAsInputStream(URLDecoder.decode(url),
					LazySSLHttpClientFactory.getClient(HTTPS_PORT)));
		} else {
			throw new DataMisformatException("Invalid Response from PhoneBook Service");
		}
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getSoapMethodParameter()
	 */
	@Override
	public String getSoapMethodParameter() {
		return "<NewPhonebookId>" + phoneBookId + "</NewPhonebookId>";
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getSoapMethod()
	 */
	@Override
	public String getSoapMethod() {
		return "GetPhonebook";
	}
}