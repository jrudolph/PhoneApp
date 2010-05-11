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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public abstract class EMailExceptionHandler extends UsbiUncaughtExceptionHandler {

	private static final String TAG = "UNHANDLED_EXCEPTION";

	public EMailExceptionHandler(
			UncaughtExceptionHandler defaultExceptionHandler, Context context) {
		super(defaultExceptionHandler, context);
	}

	/**
	 * Behandelt die ungefangene Exception.
	 */
	public void uncaughtException(Thread t, Throwable e) {

		// StackTrace holen
		final Writer stackTraceWriter = new StringWriter();
		final PrintWriter stackTracePrintWriter = new PrintWriter(
				stackTraceWriter);
		e.printStackTrace(stackTracePrintWriter);

		// Mail Test bauen
		final StringBuilder mailBody = new StringBuilder();
		mailBody.append(getEMailIntrodutionText());
		mailBody.append("\n\n\n" + ExceptionHandler.DEBUG_INFOS + "\n\n\n");
		mailBody.append(stackTraceWriter.toString());

		Log.d(TAG, stackTraceWriter.toString());

		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.setType("message/rfc822");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] { getEMailAdress() });
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				getEMailSubject());
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, mailBody
				.toString());
		context.startActivity(emailIntent);

		// call original handler
		defaultExceptionHandler.uncaughtException(t, e);
	}

	protected abstract String getEMailAdress();

	protected abstract String getEMailIntrodutionText();

	protected abstract String getEMailSubject();
}