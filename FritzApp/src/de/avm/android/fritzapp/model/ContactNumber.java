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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import de.avm.android.fritzapp.GLOBAL;

/* Represents a contactnumber in a contact in a phonebook */
public class ContactNumber implements Parcelable {

	public enum NUMBER_TYPE
	{
		HOME, MOBILE, WORK;
		//order is used for choosing first number if no main number available 

		/**
		 * Gets the text resource.
		 * 
		 * @param context
		 *            the context
		 * 
		 * @return the text resource
		 */
		public String getTextResource(Context context) {
			int id = context.getResources().getIdentifier("contact_number_" + this.toString().toLowerCase(), "string", GLOBAL.BASE_PACKAGE);
			if(id == 0) {
				return "";
			} else {
				return context.getString(id);
			}
		}
		
		/**
		 * Gets the number type for key.
		 * 
		 * @param key
		 *            the key
		 * 
		 * @return the number type for key
		 */
		public static NUMBER_TYPE getNumberTypeForKey(String key) {
			for(NUMBER_TYPE t : NUMBER_TYPE.values()) {
				if(key.toLowerCase().equals(t.toString().toLowerCase())) {
					return t;
				}
			}
			// Fallback, wenn kein anderer Type gefunden.
			return HOME;
		}
	};

	// Enum: home, mobile, work
	protected NUMBER_TYPE type;

	// Gibt an ob es die Hauptnummer ist.
	protected boolean isHauptnummer;
	
	protected String number;

	/**
	 * Instantiates a new contact number.
	 */
	public ContactNumber() {
		super();
	}

	/**
	 * Instantiates a new contact number.
	 * 
	 * @param in
	 *            the in
	 */
	public ContactNumber(Parcel in) {
		this.type = NUMBER_TYPE.values()[in.readInt()];
		this.isHauptnummer = ((in.readInt() == 1) ? true : false);
		this.number = in.readString();
	}

	/**
	 * Gets the type.
	 * 
	 * @return the type
	 */
	public NUMBER_TYPE getType() {
		return type;
	}

	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            the new type
	 */
	public void setType(NUMBER_TYPE type) {
		this.type = type;
	}

	/**
	 * Checks if is hauptnummer.
	 * 
	 * @return true, if is hauptnummer
	 */
	public boolean isHauptnummer() {
		return isHauptnummer;
	}

	/**
	 * Sets the hauptnummer.
	 * 
	 * @param isHauptnummer
	 *            the new hauptnummer
	 */
	public void setHauptnummer(boolean isHauptnummer) {
		this.isHauptnummer = isHauptnummer;
	}

	/**
	 * Gets the number.
	 * 
	 * @return the number
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * Sets the number.
	 * 
	 * @param number
	 *            the new number
	 */
	public void setNumber(String number) {
		this.number = number;
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
		out.writeInt(this.type.ordinal());
		out.writeInt((this.isHauptnummer) ? 1 : 0);
		out.writeString(this.number);
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public ContactNumber createFromParcel(Parcel in) {
			return new ContactNumber(in);
		}

		public ContactNumber[] newArray(int size) {
			return new ContactNumber[size];
		}
	};
}
