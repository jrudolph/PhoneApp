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

import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

/**
 * Handler to update the status display in
 * title bars
 */
public class StatusDisplayHandler implements Runnable
{
	private Context mContext;
	private TextView mStatusBox = null;
	private TextView mStatusSip = null;
	
	public StatusDisplayHandler(Activity parent)
	{
		mContext = parent;
		View statusDisplay = parent.findViewById(R.id.Status);
		if (statusDisplay != null)
		{
			statusDisplay.setVisibility(View.VISIBLE);
			mStatusBox = (TextView)statusDisplay.findViewById(R.id.StatusBox);
			mStatusSip = (TextView)statusDisplay.findViewById(R.id.StatusSip);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		int backgroundBox = R.drawable.state_box_disabled;
		int backgroundSip = R.drawable.state_sip_disabled;
		int boxTextAppearance = R.style.StatusDisplayTextDisabledAppearance;
		int sipTextAppearance = R.style.StatusDisplayTextDisabledAppearance;
		
		if (GLOBAL.mStatus != null)
		{
			if (GLOBAL.mStatus.isConnected())
			{
				backgroundBox = R.drawable.state_box_available;
				boxTextAppearance = R.style.StatusDisplayTextAppearance;
				switch(GLOBAL.mStatus.getSip())
				{
					case ComStatus.SIP_IDLE:
						backgroundSip = R.drawable.state_sip_idle;
						sipTextAppearance = R.style.StatusDisplayTextAppearance;
						break;
					
					case ComStatus.SIP_AWAY:
						backgroundSip = R.drawable.state_sip_away;
						sipTextAppearance = R.style.StatusDisplayTextAppearance;
						break;
	
					case ComStatus.SIP_AVAILABLE:
						backgroundSip = R.drawable.state_sip_available;
						sipTextAppearance = R.style.StatusDisplayTextAppearance;
						break;
				}
			}
			else if (GLOBAL.mStatus.getConn() == ComStatus.CONN_AWAY)
			{
				backgroundBox = R.drawable.state_box_away;
				boxTextAppearance = R.style.StatusDisplayTextAppearance;
			}
		}

		if (mStatusBox != null)
		{
			mStatusBox.setBackgroundResource(backgroundBox);
			mStatusBox.setTextAppearance(mContext, boxTextAppearance);
		}
		if (mStatusSip != null)
		{
			mStatusSip.setBackgroundResource(backgroundSip);
			mStatusSip.setTextAppearance(mContext, sipTextAppearance);
		}
	}
}
