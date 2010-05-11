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

package de.avm.android.fritzapp.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.os.Parcel;
import android.os.Parcelable;
import de.avm.android.fritzapp.com.SAXPhoneBookHandler;
import de.avm.android.fritzapp.util.error.DataMisformatException;
import de.usbi.android.util.error.exceptions.BaseException;

/* Represents a phonebook as received from the FRITZ!Box */
public class PhoneBook implements Parcelable {

	protected String id;
	protected String name;
	
	protected ArrayList<Contact> contacts = new ArrayList<Contact>();
	
	/**
	 * Instantiates a new phone book.
	 */
	public PhoneBook() {
		super();
	}
	
	/**
	 * Instantiates a new phone book.
	 * 
	 * @param in
	 *            the in
	 */
	public PhoneBook(Parcel in) {
		this.contacts = (ArrayList<Contact>) Arrays
		.asList((Contact[]) in.readParcelableArray(null));
	}

	/**
	 * Instantiates a new phone book.
	 * 
	 * @param xmlStream
	 *            the xml stream
	 */
	public PhoneBook(InputStream xmlStream) {

		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		try {
			sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			SAXPhoneBookHandler saxHandler = new SAXPhoneBookHandler(this);
			xr.setContentHandler(saxHandler);
			xr.parse(new InputSource(xmlStream));
		} catch (ParserConfigurationException e) {
			throw new BaseException("Unhandled configuration Exception", e);
		} catch (SAXException e) {
			throw new DataMisformatException("Invalid Phonebookdata",e);
		} catch (IOException e) {
			throw new DataMisformatException("Invalid Phonebookdata",e);
		}
	}

	/**
	 * Adds the contact.
	 * 
	 * @param contact
	 *            the contact
	 */
	public void addContact(Contact contact) {
		contacts.add(contact);
	}
	
	/**
	 * Gets the contacts.
	 * 
	 * @return the contacts
	 */
	public ArrayList<Contact> getContacts() {
		return contacts;
	}

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	public int describeContents() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelableArray((Contact[])contacts.toArray(), 0);
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public PhoneBook createFromParcel(Parcel in) {
			return new PhoneBook(in);
		}

		public PhoneBook[] newArray(int size) {
			return new PhoneBook[size];
		}
	};


}
