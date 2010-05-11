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

/*
 * SAX-Handler for FRITZ!Box WLANConfiguration1 description
 */
public class SAXWlanConfScpdHandler extends SAXScpdHandler
{
	public static final int NOT_AVAILABLE_LEVEL = ComSettingsChecker.TR064_NONE; 
	
	/**
	 * Instantiates a new sAX FRITZ!Box WLANConfiguration1 description handler.
	 */
	public SAXWlanConfScpdHandler()
	{
		mActions = new String[]
  		{
			"GetTotalAssociations",
			"GetGenericAssociatedDeviceInfo"
  		};
	}

	@Override
	public int getTr064Level()
	{
		// all have to be available since TR064_BASIC
		return (mAvalability[0] && mAvalability[1]) ?
				ComSettingsChecker.TR064_MOSTRECENT : NOT_AVAILABLE_LEVEL;
	}
}
