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

package de.avm.android.fritzapp.util.error;

import de.usbi.android.util.error.exceptions.BaseException;

/* Exception thrown when data received from FRITZ!Box is not in the format we expected. */
public class DataMisformatException extends BaseException {

	/**
	 * Instantiates a new data misformat exception.
	 * 
	 * @param string
	 *            the string
	 */
	public DataMisformatException(String string) {
		super(string);
	}

	/**
	 * Instantiates a new data misformat exception.
	 * 
	 * @param string
	 *            the string
	 * @param e
	 *            the e
	 */
	public DataMisformatException(String string, Exception e) {
		super(string, e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1324993075772323917L;

}