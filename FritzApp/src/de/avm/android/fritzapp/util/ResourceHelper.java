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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.com.ComSettingsChecker.CONNECTION_PROBLEM;

/* HelperCalss for access of ressources (strings etc) */
public class ResourceHelper {

	/**
	 * Gets the icon for an activity class. (launcher icon)
	 * 
	 * @param clazz
	 *            the class
	 * @param c
	 *            a valid context
	 * 
	 * @return the icon for class
	 */
	public static Drawable getIconForClass(Class<?> clazz, boolean launcherIcon,
			Context c)
	{
		int id = getIdentifierOfTyp(
				(launcherIcon) ? "_metaicon" : "_icon", "drawable", clazz, c);
		if (id != 0) {
			return c.getResources().getDrawable(id);
		} else {
			return c.getResources().getDrawable((launcherIcon) ?
					R.drawable.features_metaicon : R.drawable.features_icon);
		}
	}

	/**
	 * Gets the label for an activityclass. (launchertext)
	 * 
	 * @param clazz
	 *            the clazz
	 * @param c
	 *            a valid context
	 * 
	 * @return the label for class
	 */
	public static String getLabelForClass(Class<?> clazz, Context c) {
		int id = getIdentifierOfTyp("_label", "string", clazz, c);
		if (id != 0) {
			return c.getResources().getString(id);
		} else {
			return "MISSING LABEL";
		}
	}

	/**
	 * Gets the text for a connection problem.
	 * 
	 * @param p
	 *            the problem
	 * @param c
	 *            a valid context
	 * 
	 * @return the text for connection problem
	 */
	public static String getTextForConnectionProblem(CONNECTION_PROBLEM p,
			Context c) {
		int id = c.getResources().getIdentifier(
				"problem_" + p.toString().toLowerCase(), "string", GLOBAL.BASE_PACKAGE);
		if (id != 0) {
			return c.getResources().getString(id);
		} else {
			return "CONNECTION_PROBLEM";
		}
	}

	/**
	 * Gets the identifier of a specific type for a class.
	 * 
	 * @param type
	 *            the type
	 * @param resTyp
	 *            the res typ 
	 * @param clazz
	 *            the clazz
	 * @param c
	 *            a valid context
	 * 
	 * @return the identifier of typ
	 */
	protected static int getIdentifierOfTyp(String type, String resTyp,
			Class<?> clazz, Context c) {
		String idName = clazz.getSimpleName().replace("Activity", "");
		idName = idName.toLowerCase() + type;
		int id = c.getResources().getIdentifier(idName, resTyp, GLOBAL.BASE_PACKAGE);
		Log.v(idName, "" + id);
		return id;
	}
}