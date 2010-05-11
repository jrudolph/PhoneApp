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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import de.avm.android.fritzapp.R;

/* Represents a call in the calllist */
public class Call implements Parcelable {

	public enum CALL_TYPE {
		MISSED, INCOMING, OUTGOING, UNSPECIFIED;

		/**
		 * Gets the icon for call type.
		 * 
		 * @param context
		 *            a valid context
		 * 
		 * @return the icon for call type
		 */
		public Drawable getIconForCallType(Context context) {
			int id;
			switch (this) {
			case MISSED:
				id = de.avm.android.fritzapp.R.drawable.call_missed;
				break;
			case INCOMING:
				id = de.avm.android.fritzapp.R.drawable.call_incoming;
				break;
			case OUTGOING:
				id = de.avm.android.fritzapp.R.drawable.call_outgoing;
				break;
			default:
				id = de.avm.android.fritzapp.R.drawable.call_new;
				break;
			}
			return context.getResources().getDrawable(id);
		}
	};

	protected CALL_TYPE type;
	protected Date timeStamp;
	protected String partnerName;
	protected String partnerNumber;
	protected String internPort;
	protected String internNumber;
	protected String id;
	protected int count;
	protected ContactNumber.NUMBER_TYPE numberType;

	protected int durationInSec; // in seconds

	/**
	 * Instantiates a new call.
	 * 
	 * @param in
	 *            the in
	 */
	public Call(Parcel in) {
		this.partnerName = in.readString();
		this.durationInSec = in.readInt();
		this.partnerNumber = in.readString();
		this.internPort = in.readString();
		this.timeStamp = new Date(in.readString());
		this.type = CALL_TYPE.values()[in.readInt()];
	}

	/**
	 * Gets the call type for key. Mapping from FRITZ!Box-key
	 * 
	 * @param aString
	 *            the a string
	 * 
	 * @return the call type for key
	 */
	public static CALL_TYPE getCallTypeForKey(String aString) {
		int key = Integer.parseInt(aString);
		switch (key) {
		case 1:
			return CALL_TYPE.INCOMING;
		case 2:
			return CALL_TYPE.MISSED;
		case 3:
			return CALL_TYPE.OUTGOING;
		default:
			return CALL_TYPE.UNSPECIFIED;
		}
	}

	/**
	 * Instantiates a new call.
	 */
	public Call() {
		super();
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date date) {
		this.timeStamp = date;
	}

	public String getPartnerName() {
		if(partnerName == null) {
			return "";
		} else {
			return partnerName;
		}
	}

	/**
	 * Gets the partner name or if that is empty the number.
	 * 
	 * @return the partner name if empty number
	 */
	public String getPartnerNameIfEmptyNumber() {
		if (isPartnerNameEmpty()) {
			return getPartnerNumber();
		}
		return getPartnerName();
	}

	/**
	 * Checks if the partner name is empty.
	 * 
	 * @return true, if is partner name empty
	 */
	public boolean isPartnerNameEmpty() {
		return this.getPartnerName().length() == 0;
	}

	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}

	public CALL_TYPE getType() {
		return type;
	}

	public void setType(CALL_TYPE type) {
		this.type = type;
	}

	/**
	 * Gets the partner number.
	 * 
	 * @return the partner number
	 */
	public String getPartnerNumber() {
		if(partnerNumber == null) {
			return "";
		} else {
			return partnerNumber;
		}
	}

	/**
	 * Sets the partner number.
	 * 
	 * @param partnerNumber
	 *            the new partner number
	 */
	public void setPartnerNumber(String partnerNumber) {
		this.partnerNumber = partnerNumber;
	}

	/**
	 * Gets the intern port.
	 * 
	 * @return the intern port
	 */
	public String getInternPort() {
		if(internPort == null) {
			return "";
		} else {
			return internPort;
		}
	}

	/**
	 * Sets the intern port.
	 * 
	 * @param internPort
	 *            the new intern port
	 */
	public void setInternPort(String internPort) {
		this.internPort = internPort;
	}

	/**
	 * Gets the intern number.
	 * 
	 * @return the intern number
	 */
	public String getInternNumber() {
		if(internNumber == null) {
			return "";
		} else {
			return internNumber;
		}
	}

	/**
	 * Sets the intern number.
	 * 
	 * @param internNumber
	 *            the new intern number
	 */
	public void setInternNumber(String internNumber) {
		this.internNumber = internNumber;
	}

	/**
	 * Gets the duration.
	 * 
	 * @return the duration
	 */
	public int getDuration() {
		return durationInSec;
	}

	/**
	 * Sets the duration.
	 * 
	 * @param duration
	 *            the new duration
	 */
	public void setDuration(int duration) {
		this.durationInSec = duration;
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
	 * Gets the count.
	 * 
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Sets the count.
	 * 
	 * @param count
	 *            the new count
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Gets the number type.
	 * 
	 * @return the number type
	 */
	public ContactNumber.NUMBER_TYPE getNumberType() {
		return numberType;
	}

	/**
	 * Sets the number type.
	 * 
	 * @param numberType
	 *            the new number type
	 */
	public void setNumberType(ContactNumber.NUMBER_TYPE numberType) {
		this.numberType = numberType;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<Call> CREATOR = new Parcelable.Creator<Call>() {
		public Call createFromParcel(Parcel in) {
			return new Call(in);
		}

		public Call[] newArray(int size) {
			return new Call[size];
		}
	};

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.partnerName);
		out.writeInt(this.durationInSec);
		out.writeString(this.partnerNumber);
		out.writeString(this.internPort);
		out.writeString(this.timeStamp.toGMTString());
		out.writeInt(this.type.ordinal());
	}

	/**
	 * Gibt Zeit und Datum des Anrufes "schön" formatiert zurück. Format:
	 * <VollesDatum_MonatAlsZahl>, <Uhrzeit_StundeMinute> Wobei das Datum, falls
	 * zutreffend, als "Heute" oder "Gestern" dargestellt wird.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the pretty date for list
	 */
	public String getPrettyDateForList(Context c)
	{
		String timeString = DateFormat.getTimeFormat(c).format(timeStamp);

		// today?
		Calendar now = GregorianCalendar.getInstance(); 
		Calendar cmp = new GregorianCalendar(now.get(Calendar.YEAR),
				now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
		if (!timeStamp.before(cmp.getTime()))
			return c.getString(R.string.call_log_today) + " " + timeString;
		
		// yesterday?
		cmp.roll(Calendar.DAY_OF_MONTH, false);
		if (!timeStamp.before(cmp.getTime()))
			return c.getString(R.string.call_log_yesterday) + " " + timeString;

		// older
		return DateFormat.getDateFormat(c).format(this.getTimeStamp()) + " "
				+ timeString;
	}

	/**
	 * Gibt Zeit und Datum des Anrufes "schön" formatiert zurück. Format:
	 * <VollesDatum_MonatAlsText> <Uhrzeit_StundeMinute>
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return the pretty date full
	 */
	public String getPrettyDateFull(Context c) {
		return DateFormat.getMediumDateFormat(c).format(this.getTimeStamp()) + " "
				+ DateFormat.getTimeFormat(c).format(this.getTimeStamp());
	}
}