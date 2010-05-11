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
 * SAX-Handler for FRITZ!Box X_AVM-DE_OnTel1 description
 */
public class SAXOnTelScpdHandler extends SAXScpdHandler
{
	public static final int NOT_AVAILABLE_LEVEL = ComSettingsChecker.TR064_NONE; 
	
	/**
	 * Instantiates a new sAX FRITZ!Box X_AVM-DE_OnTel1 description handler.
	 */
	public SAXOnTelScpdHandler()
	{
		mActions = new String[]
		{
			"GetCallList",
			"GetPhonebookList",
			"GetPhonebook"
		};
	}

	@Override
	public int getTr064Level()
	{
		// all have to be available since TR064_BASIC
		for(boolean b : mAvalability)
		{
			if (!b) return NOT_AVAILABLE_LEVEL;
		}
		return ComSettingsChecker.TR064_MOSTRECENT;
	}
}
