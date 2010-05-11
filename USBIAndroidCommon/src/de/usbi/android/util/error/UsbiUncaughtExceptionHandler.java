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

package de.usbi.android.util.error;

import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;

public abstract class UsbiUncaughtExceptionHandler implements
		UncaughtExceptionHandler {

	protected final UncaughtExceptionHandler defaultExceptionHandler;

	protected final Context context;

	public UsbiUncaughtExceptionHandler(
			UncaughtExceptionHandler defaultExceptionHandler, Context context) {
		this.defaultExceptionHandler = defaultExceptionHandler;
		this.context = context;

	}
	
	protected String getCheckSum(String x) {
		return x.length() + "";
	}

}