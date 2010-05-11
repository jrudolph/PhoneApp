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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.avm.android.fritzapp.model.Contact;
import de.avm.android.fritzapp.model.ContactNumber;
import de.avm.android.fritzapp.model.PhoneBook;
import de.avm.android.fritzapp.model.ContactNumber.NUMBER_TYPE;

/*
 * SAX-Handler for AVM Phonebook-Format
 */
public class SAXPhoneBookHandler extends DefaultHandler {

	private boolean in_phonebook = false;
	private boolean in_contact = false;
	private boolean in_category = false;
	private boolean in_person = false;
	private boolean in_realName = false;
	private boolean in_telephony = false;
	private boolean in_number = false;

	private PhoneBook phoneBook;
	private Contact currentContact = new Contact();
	private ContactNumber currentContactNumber = new ContactNumber();

	/**
	 * Instantiates a new sAX phone book handler.
	 * 
	 * @param phoneBook
	 *            the phone book
	 */
	public SAXPhoneBookHandler(PhoneBook phoneBook) {
		this.phoneBook = phoneBook;
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		if (localName.toLowerCase().equals("phonebook")) {
			this.in_phonebook = true;
			phoneBook.setId(atts.getValue("id"));
			phoneBook.setName(atts.getValue("name"));
		}
		if (localName.toLowerCase().equals("contact")) {
			currentContact = new Contact();
			this.in_contact = true;
		}
		if (localName.toLowerCase().equals("category")) {
			this.in_category = true;
		}
		if (localName.toLowerCase().equals("person")) {
			this.in_person = true;
		}
		if (localName.toLowerCase().equals("telephony")) {
			this.in_telephony = true;
		}
		if (localName.toLowerCase().equals("realname")) {
			this.in_realName = true;
		}
		if (localName.toLowerCase().equals("number")) {
			currentContactNumber = new ContactNumber();
			
			NUMBER_TYPE type = NUMBER_TYPE.getNumberTypeForKey(atts.getValue("type"));
			currentContactNumber.setType(type);
			
			boolean isHauptnummer = atts.getValue("prio").toLowerCase().equals("1");
			currentContactNumber.setHauptnummer(isHauptnummer);
			
			this.in_number = true;
		}
	}

	/**
	 * on closing tags like: </tag>.
	 * 
	 * @param namespaceURI
	 *            the namespace uri
	 * @param localName
	 *            the local name
	 * @param qName
	 *            the q name
	 * 
	 * @throws SAXException
	 *             the SAX exception
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		if (localName.toLowerCase().equals("phonebook")) {
			this.in_phonebook = false;
		}
		if (localName.toLowerCase().equals("contact")) {
			phoneBook.addContact(currentContact);
			this.in_contact = false;
		}
		if (localName.toLowerCase().equals("category")) {
			this.in_category = false;
		}
		if (localName.toLowerCase().equals("person")) {
			this.in_person = false;
		}
		if (localName.toLowerCase().equals("telephony")) {
			this.in_telephony = false;
		}
		if (localName.toLowerCase().equals("realname")) {
			this.in_realName = false;
		}
		if (localName.toLowerCase().equals("number")) {
			currentContact.addNumber(currentContactNumber);
			this.in_number = false;
		}
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>.
	 * 
	 * @param ch
	 *            the ch
	 * @param start
	 *            the start
	 * @param length
	 *            the length
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		String str = new String(ch, start,length);
		if (this.in_category) {
			currentContact.setCategory(Integer.parseInt(str));
		}
		if (this.in_realName) {
			currentContact.setRealName(str);			
		}
		if (this.in_number) {
			currentContactNumber.setNumber(str);
		}
	}

}
