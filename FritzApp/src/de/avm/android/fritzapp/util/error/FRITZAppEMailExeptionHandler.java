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

import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;
import de.avm.android.fritzapp.R;
import de.usbi.android.util.error.EMailExceptionHandler;

/* ExceptionHandler for Fritz!App 
 * Opens an email with debug-information in the default email-application 
 */
public class FRITZAppEMailExeptionHandler extends EMailExceptionHandler {

	/**
	 * Instantiates a new Fritz!App e mail exception handler.
	 * 
	 * @param defaultExceptionHandler
	 *            the default exception handler
	 * @param context
	 *            the context
	 */
	public FRITZAppEMailExeptionHandler(
			UncaughtExceptionHandler defaultExceptionHandler, Context context) {
		super(defaultExceptionHandler, context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.usbi.android.util.error.EMailExceptionHandler#getEMailAdress()
	 */
	@Override
	protected String getEMailAdress() {
		return context.getString(R.string.error_mail_address);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.usbi.android.util.error.EMailExceptionHandler#getEMailSubject()
	 */
	@Override
	protected String getEMailSubject() {
		return context.getString(R.string.error_mail_subject);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.usbi.android.util.error.EMailExceptionHandler#getEMailIntrodutionText
	 * ()
	 */
	@Override
	protected String getEMailIntrodutionText() {
		return context.getString(R.string.error_mail_introduction);
	}

}