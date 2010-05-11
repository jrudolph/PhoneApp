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

import android.content.Context;
import de.avm.android.fritzapp.model.WLANInfo;
import de.avm.android.fritzapp.util.error.DataMisformatException;

/* Soap implementation for the message-status-Interface */
public class GetWLANStatus extends WLANSoapHelper<WLANInfo> {

	private String macAdress;
	private int index;
	
	public GetWLANStatus(Context c, int index, String macAdress) {
		super(c);
		this.macAdress = macAdress;
		this.index = index;
	}
	
	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getSoapMethodParameter()
	 */
	@Override
	public String getSoapMethodParameter() {
		return "<NewAssociatedDeviceIndex>" + index + "</NewAssociatedDeviceIndex>";
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getQualifiedResult()
	 */
	@Override
	public WLANInfo getQualifiedResult() {
		String input = getSoapBody();
		// Überprüfen ob es die Fritz!Box ist mit der das Handy per WLAN verbunden ist.
		if( getValueByName("NewAssociatedDeviceMACAddress", input).equals(macAdress)) {
			WLANInfo result = new WLANInfo();
			result.setIndex(this.index);
			result.setMacAdress(getValueByName("NewAssociatedDeviceMACAddress", input));
			result.setIpAdress(getValueByName("NewAssociatedDeviceIPAddress", input));
			result.setAuthState(Boolean.parseBoolean(getValueByName("NewAssociatedDeviceAuthState", input)));
			result.setBandwidth(Integer.parseInt(getValueByName("NewX_AVM-DE_Speed", input)));
			result.setSignalStrength(Integer.parseInt(getValueByName("NewX_AVM-DE_SignalStrength", input)));
			return result;
		} else {
			return null;
		}
	}
	
	/**
	 * Helper method. Get a value for a given xml-tagname
	 * 
	 * @param name
	 *            the name of the tag
	 * @param input
	 *            the input-xml-string
	 * 
	 * @return the value by name
	 */
	private String getValueByName(String name, String input) {
		if (input.length() == 0) {
			return "";
		}
		Matcher m = Pattern.compile(name + ">(.*?)<").matcher(input);
		if (m.find()) {
			return m.group(1);
		} else {
			throw new DataMisformatException(
					"Invalid Response from WLAN-Service");
		}
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.com.soap.AbstractSoapHelper#getSoapMethod()
	 */
	@Override
	public String getSoapMethod() {
		return "GetGenericAssociatedDeviceInfo";
	}
}
