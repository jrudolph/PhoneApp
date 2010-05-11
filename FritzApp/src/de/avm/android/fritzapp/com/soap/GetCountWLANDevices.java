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

public class GetCountWLANDevices extends WLANSoapHelper<Integer> {

	public GetCountWLANDevices(Context c) {
		super(c);
	}

	@Override
	public Integer getQualifiedResult() {
		String input = getSoapBody();
		Matcher m = Pattern.compile("NewTotalAssociations>(.*?)<\\/NewTotalAssociations")
				.matcher(input);
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}
		return 0;
	}

	@Override
	public String getSoapMethod() {
		return "GetTotalAssociations";
	}

}