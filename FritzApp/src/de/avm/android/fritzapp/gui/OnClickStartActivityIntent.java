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

package de.avm.android.fritzapp.gui;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import de.usbi.android.util.adapter.OnClickStartActivity;

/*
 * Special OnClickHandler for the launcherbuttons.
 * For activities with OfflineActivity interface its gets the Intent to start from
 * the activity (dynamically calls static method).
 * For all other activities, the activity is started itself 
 */
public class OnClickStartActivityIntent extends OnClickStartActivity
{
	/**
	 * Instantiates a new on click start activity.
	 * 
	 * @param activity
	 *            the activity
	 */
	public OnClickStartActivityIntent(
			Class<? extends Activity> activity)
	{
		super(activity);
	}

	/* (non-Javadoc)
	 * @see de.usbi.android.util.adapter.OnClickStartActivity#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v)
	{
		// can we ask the activity if it could be shown?
		Class[] ifs = activity.getInterfaces();
		for (int i = 0; i < ifs.length; i++)
		{
			if (OfflineActivity.class.equals(ifs[i]))
			{
				try
				{
					// ask the activity, go for it depending on answer
					Method method = activity.getMethod("showIntent", new Class[] {Context.class});
					Intent intent = (Intent)method.invoke(null, new Object[] {v.getContext()}); 
					if (intent != null) v.getContext().startActivity(intent);
				}
				catch(Exception exp)
				{
					exp.printStackTrace();
				}
				return;
			}
		}

		// can't ask the activity, so go for it
		super.onClick(v);
	}
}
