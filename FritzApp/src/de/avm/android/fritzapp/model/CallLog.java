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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.os.Parcel;
import android.os.Parcelable;
import de.avm.android.fritzapp.com.SAXCallLogHandler;
import de.avm.android.fritzapp.util.error.DataMisformatException;
import de.usbi.android.util.error.exceptions.BaseException;

/* Represents a list of calls as received from the FRITZ!Box */
public class CallLog implements Parcelable {

	private ArrayList<Call> calls = new ArrayList<Call>();

	/**
	 * Instantiates a new call log.
	 */
	public CallLog() {
		super();
	}

	/**
	 * Instantiates a new call log.
	 * 
	 * @param in
	 *            the in
	 */
	public CallLog(Parcel in) {
		in.readParcelableArray(null);
	}

	/**
	 * Instantiates a new call log.
	 * 
	 * @param xmlStream
	 *            the xml stream
	 */
	public CallLog(InputStream xmlStream) {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		try {
			sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			SAXCallLogHandler saxHandler = new SAXCallLogHandler(this);
			xr.setContentHandler(saxHandler);
			xr.parse(new InputSource(xmlStream));
		} catch (ParserConfigurationException e) {
			throw new BaseException("Unhandled configuration Exception", e);
		} catch (SAXException e) {
			throw new DataMisformatException("Invalid Calllog Data",e);
		} catch (IOException e) {
			throw new DataMisformatException("Invalid Callog Data",e);
		}
	}

	/**
	 * Gets the calls.
	 * 
	 * @return the calls
	 */
	public ArrayList<Call> getCalls() {
		return calls;
	}

	/**
	 * Adds the call.
	 * 
	 * @param call
	 *            the call
	 */
	public void addCall(Call call) {
		this.calls.add(call);
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public CallLog createFromParcel(Parcel in) {
			return new CallLog(in);
		}

		public CallLog[] newArray(int size) {
			return new CallLog[size];
		}
	};

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
		out.writeParcelableArray((Call[])calls.toArray(), 0);
	}
}
