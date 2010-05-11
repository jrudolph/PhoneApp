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

/*
 *  Represents information about a WLAN Device. As received from the Fritzbox 
 */
public class WLANInfo {
	
	/*
		NewAssociatedDeviceIndex  In  Int  Index in der Liste (0 <= index < Anzahl) 
		NewAssociatedDeviceMACAddress  Out  String  MAC-Adresse des Gerätes 
		NewAssociatedDeviceIPAddress  Out  String  IP-Adresse des Gerätes 
		NewAssociatedDeviceAuthState  Out  Bool  Gerät angemeldet 
		NewX_AVM-DE_Speed  Out  Int  Datenrate (0 ... 300) 
		NewX_AVM-DE_SignalStrength  Out  Int  Signalstärke (0 … 70)
	*/
	
	// NewX_AVM-DE_SignalStrength  Out  Int  Signalstärke (0 … 70)
	public static final int SIGNAL_STRENGTH_RANGE = 70;
	
	// NewX_AVM-DE_Speed  Out  Int  Datenrate (0 ... 300) 
	public static final int SPEED_RANGE = 300;
	
	private int index;
	private String macAdress;
	private String ipAdress;
	private boolean AuthState;
	private int speed;
	private int signalStrength;
	
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public String getMacAdress() {
		return macAdress;
	}
	
	public void setMacAdress(String macAdress) {
		this.macAdress = macAdress;
	}
	
	public String getIpAdress() {
		return ipAdress;
	}
	
	public void setIpAdress(String ipAdress) {
		this.ipAdress = ipAdress;
	}
	
	public boolean isAuthState() {
		return AuthState;
	}
	
	public void setAuthState(boolean authState) {
		AuthState = authState;
	}
	
	public int getBandwidth() {
		return speed;
	}
	
	public void setBandwidth(int bandwidth) {
		this.speed = bandwidth;
	}
	
	public int getSignalStrength() {
		return signalStrength;
	}
	
	public void setSignalStrength(int signalStrength) {
		this.signalStrength = signalStrength;
	}

}