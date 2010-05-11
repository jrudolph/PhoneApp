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

import android.content.Context;

public abstract class WLANSoapHelper<RESULT> extends AbstractSoapHelper<RESULT> {

	/*
	Service Id  urn:WLANConfiguration-com:serviceId:WLANConfiguration1 
	Service Type  urn:dslforum-org:service:WLANConfiguration:1 
	Control Url  /upnp/control/wlanconfig1 
	Scpd Url  /wlanconfigSCPD.xml 
	*/
	
	public WLANSoapHelper(Context c) {
		super(c);
	}

	public String getNamespace() {
		return "urn:dslforum-org:service:WLANConfiguration:1";
	}

	public String getControlURL() {
		return "/upnp/control/wlanconfig1";
	}

}