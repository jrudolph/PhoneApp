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

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

/* Represents a contact in a phonebook */
public class Contact implements Parcelable {

	protected int category;
	protected String realName;

	public static final int CATEGORY_WICHTIG = 1;

	protected ArrayList<ContactNumber> numbers = new ArrayList<ContactNumber>();

	/**
	 * Instantiates a new contact.
	 */
	public Contact() {
		super();
	}
	
	/**
	 * Instantiates a new contact.
	 * 
	 * @param in
	 *            the in
	 */
	public Contact(Parcel in) {
		this.category = in.readInt();
		this.realName = in.readString();
		in.readTypedList(this.numbers, ContactNumber.CREATOR);
	}

	/**
	 * Adds the number.
	 * 
	 * @param cn
	 *            the cn
	 */
	public void addNumber(ContactNumber cn) {
		numbers.add(cn);
	}

	/**
	 * Gets the contact numbers.
	 * 
	 * @return the contact numbers
	 */
	public ArrayList<ContactNumber> getContactNumbers() {
		return numbers;
	}

	/**
	 * Gets the category.
	 * 
	 * @return the category
	 */
	public int getCategory() {
		return category;
	}

	/**
	 * Sets the category.
	 * 
	 * @param category
	 *            the new category
	 */
	public void setCategory(int category) {
		this.category = category;
	}

	/**
	 * Gets the real name.
	 * 
	 * @return the real name
	 */
	public String getRealName()
	{
		return (realName == null) ? "" : realName;
	}

	/**
	 * Sets the real name.
	 * 
	 * @param realName
	 *            the new real name
	 */
	public void setRealName(String realName) {
		this.realName = realName;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	public int describeContents() {
		return 0;
	}
	
	/**
	 * Gets the Hauptnummer or an other preferred number.
	 * 
	 * @return the number
	 */
	public ContactNumber getHauptnummer()
	{
		// return number with main property
		// if no main number available, choose one in order of NUMBER_TYPE 
		ContactNumber firstNumber = null;
		for (ContactNumber cn : numbers)
		{
			if (cn.isHauptnummer())
			{
				return cn;
			}
			else if ((firstNumber == null) ||
					(firstNumber.getType().compareTo(cn.getType()) > 0))
			{
				firstNumber = cn;
			}
		}
		
		return firstNumber;
	}
	
	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(this.category);
		out.writeString(this.realName);
		out.writeTypedList(numbers);
	}

	public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
		public Contact createFromParcel(Parcel in) {
			return new Contact(in);
		}

		public Contact[] newArray(int size) {
			return new Contact[size];
		}
	};
}