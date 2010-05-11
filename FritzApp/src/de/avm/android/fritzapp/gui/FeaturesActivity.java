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
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.sipua.UserAgent;
import de.avm.android.fritzapp.sipua.ui.Receiver;
import de.avm.android.fritzapp.util.ResourceHelper;

/*
 * Main GUI. SHows the status and Launcher-Icons
 */
public class FeaturesActivity extends Activity
{
	private Runnable mStatusChangedHandler = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.features);
		
		final ListView listView = (ListView)findViewById(R.id.DashBoard);
		listView.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
				{
					View item = listView.getSelectedView();
					if (item != null)
						return item.performClick();
				}
				return false;
			}
		});
		
		mStatusChangedHandler = new Runnable()
		{
			public void run()
			{
				// fill according to connection status
				listView.setAdapter(new StartButtonAdapter());
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		// show in call screen if call is active
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE) Receiver.moveTop();
		GLOBAL.mStatus.addStatusChangedHandler(mStatusChangedHandler);
		mStatusChangedHandler.run();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		GLOBAL.mStatus.removeStatusChangedHandler(mStatusChangedHandler);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    getMenuInflater().inflate(R.menu.features_menu, menu);
		return true;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.About:
				FRITZApp.showAbout(this);
				break;
				
			case R.id.Help:
				FRITZApp.showHelp(this);
				break;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch(keyCode)
		{
			case KeyEvent.KEYCODE_BACK:
				// if we have been left with HOME button and later on returned
				// using task switcher, we will not get back to main
				// activity from here by defaulted handling!
	            startActivity(new Intent(this, FRITZApp.class));
	            finish();
	            return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Für das Grid der Launcherbuttons auf dem Einstiegsscreen
	 * 
	 * @author am
	 * 
	 */
	protected class StartButtonAdapter extends BaseAdapter
	{
		private LinkedList<Class<? extends Activity>> mActivities =
				new LinkedList<Class<? extends Activity>>();

		// Einträge im Startgrid in der gewünschten Reihenfolge
		/**
		 * Instantiates a new start button adapter.
		 */
		@SuppressWarnings("unchecked")
		public StartButtonAdapter()
		{
			// all
			mActivities.add(CallLogActivity.class);			
			mActivities.add(PhoneBookActivity.class);			
			mActivities.add(WlanTesterActivity.class);			
			mActivities.add(OpenWebActivity.class);			
			mActivities.add(SettingsActivity.class);
			
			// remove not available ones
			for (int activityItem = mActivities.size() - 1; activityItem >= 0;
					activityItem--)
			{
				// can we ask the activity if it could be shown?
				Class[] ifs = mActivities.get(activityItem).getInterfaces();
				for (int interfaceItem = 0; interfaceItem < ifs.length;
						interfaceItem++)
				{
					if (OfflineActivity.class.equals(ifs[interfaceItem]))
					{
						try
						{
							// ask the activity, remove if the answer is no
							Method method = mActivities.get(activityItem)
									.getMethod("canShow", (Class[])null);
							if (!(Boolean)method.invoke(null, (Object[])null))
								mActivities.remove(activityItem);
						}
						catch(Exception exp)
						{
							mActivities.remove(activityItem);
							exp.printStackTrace();
						}
					}
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getCount()
		 */
		public int getCount() {
			return mActivities.size();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItem(int)
		 */
		public Object getItem(int pos)
		{
			return mActivities.get(pos);
		}

		/* (non-Javadoc)
		 * @see android.widget.BaseAdapter#hasStableIds()
		 */
		@Override
		public boolean hasStableIds() {
			return true;
		}

		/*
		 * baut den View für einen Launcherbutton zusammen
		 */
		/* (non-Javadoc)
		 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent)
		{
			RelativeLayout activityLauncher = (convertView == null) ?
					(RelativeLayout) View.inflate(FeaturesActivity.this,
							R.layout.t_launcherlistitem, null) :
					(RelativeLayout)convertView;
			Class<? extends Activity> currActivityClass = mActivities.get(position);
			Drawable icon = ResourceHelper.getIconForClass(currActivityClass, false,
					FeaturesActivity.this);
			String label = ResourceHelper.getLabelForClass(currActivityClass,
					FeaturesActivity.this);
			ImageView launchIcon = (ImageView) activityLauncher
					.findViewById(R.id.launcher_button);
			TextView text = (TextView) activityLauncher
					.findViewById(R.id.launcher_text);
			launchIcon.setImageDrawable(icon);
			text.setText(label, TextView.BufferType.NORMAL);
			activityLauncher.setOnClickListener(new OnClickStartActivityIntent(
					currActivityClass));
			
			// disable view, if nothing to invoke
			Class[] ifs = currActivityClass.getInterfaces();
			for (int interfaceItem = 0; interfaceItem < ifs.length;
					interfaceItem++)
			{
				if (OfflineActivity.class.equals(ifs[interfaceItem]))
				{
					try
					{
						// ask the activity, disable if no intent to invoke
						Method method = currActivityClass
								.getMethod("showIntent", new Class[] {Context.class});
						if ((Intent)method.invoke(null,
								new Object[] {activityLauncher.getContext()}) == null)
							activityLauncher.setEnabled(false);
					}
					catch(Exception exp)
					{
						activityLauncher.setEnabled(false);
						exp.printStackTrace();
					}
				}
			}

			return activityLauncher;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return position;
		}
	}
}