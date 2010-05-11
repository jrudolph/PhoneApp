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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class ExceptionHandler {

	public static String APP_VERSION = "unknown";
	public static String APP_PACKAGE = "unknown";
	public static String PHONE_MODEL = "unknown";
	public static String ANDROID_VERSION = "unknown";
	protected static String DEBUG_INFOS = "";

	public static String TAG = "de.usbi.android.util.error.ExceptionHandler";

	/**
	 * Den aktuellen Thread registrieren. Damit werden alle unbehandelten Exception gefangen und 
	 * vom übergebenen UsbiUncaughtExceptionHandler behandelt.
	 * 
	 * @param context
	 * @param exceptionHandler
	 */
	public static void register(final Context context,
			final Class<? extends UsbiUncaughtExceptionHandler> exceptionHandler) {
		Log.i(TAG, "Registering default exceptions handler");

		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
			APP_VERSION = info.versionName;
			APP_PACKAGE = info.packageName;
			PHONE_MODEL = android.os.Build.MODEL;
			ANDROID_VERSION = android.os.Build.VERSION.RELEASE;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		addDebugInfo("APP_PACKAGE", APP_PACKAGE);
		addDebugInfo("APP_VERSION", APP_VERSION);
		addDebugInfo("ANDROID_VERSION", ANDROID_VERSION);
		addDebugInfo("PHONE_MODEL", PHONE_MODEL);

		new Thread() {
			@Override
			public void run() {
				UncaughtExceptionHandler currentHandler = Thread
						.getDefaultUncaughtExceptionHandler();
				if (currentHandler != null) {
					Log.d(TAG, "current handler class="
							+ currentHandler.getClass().getName());
				}
				// don't register again if already registered
				if (!(currentHandler instanceof UsbiUncaughtExceptionHandler)) {
					// Register default exceptions handler
					try {
						UsbiUncaughtExceptionHandler newInstance = exceptionHandler
								.getDeclaredConstructor(
										UncaughtExceptionHandler.class,
										Context.class).newInstance(
										currentHandler, context);
						Thread.setDefaultUncaughtExceptionHandler(newInstance);
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}.start();
	}

	/**
	 * Fügt einen Eintrag in die Debug Informationen ein, der später zusammen mit den
	 * Exception Informationen weiter verarbeitet wird.
	 * 
	 * @param key
	 * @param value
	 */
	public static void addDebugInfo(String key, String value) {
		DEBUG_INFOS += key + ((value != null && value.length() > 0) ? ": " + value : "") + "\n";
	}
}