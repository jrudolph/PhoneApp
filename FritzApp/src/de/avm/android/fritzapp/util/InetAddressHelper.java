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

package de.avm.android.fritzapp.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressHelper
{
	private InetAddressHelper()
	{
 		// don't create an instance
	}

	/**
	 * Returns the v4 address of a host according to the given host
	 * string name host. The host string may be either a machine name
	 * or a dotted string IP address. If the latter, the hostName field
	 * is determined upon demand. host can be null which means that an
	 * address of the loopback interface is returned.
	 * 
	 * @param host the hostname or literal IP string to be resolved.
	 * @return the InetAddress instance representing the host.
	 * @throws UnknownHostException if the address lookup fails.
	 */
	public static InetAddress getByName(String host)
			throws UnknownHostException
	{
		InetAddress[] addresses = InetAddress.getAllByName(host);
		for(InetAddress address : addresses)
		{
			if (address.getClass() == Inet4Address.class)
				return address;
		}
		throw new UnknownHostException(String
				.format("No IPv4 address found for \"%s\"", host));
	}
}
