package de.avm.android.fritzapp.gui;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.util.CallRouteExceptions;
import de.avm.android.fritzapp.util.CallRouteExceptionsAdapter;
import de.avm.android.fritzapp.util.PhoneNumberHelper;

/* GUI for the settings of call route exceptions */
public class SettingsRouteExceptionsActivity extends ListActivity
{
	private CallRouteExceptions mCallRouteExceptions = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// frame
		LinearLayout frameView = (LinearLayout) View.inflate(this,
				R.layout.settings, null);

		// list
		ListView listView = new ListView(this);
		listView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		listView.setSelector(R.drawable.list_selector_background);
		listView.setId(android.R.id.list);
		frameView.addView(listView, 1);
		setContentView(frameView);

		setTitle(R.string.settings_routeexceptions);
		registerForContextMenu(getListView());
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mDialogTargetPosition = position;
				showDialog(MENU_EDIT);
			}
		});

		// load list
		mCallRouteExceptions = new CallRouteExceptions(this);
		updateListAdapter();
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		if (mCallRouteExceptions != null)
			mCallRouteExceptions.save();
	}

	private void updateListAdapter()
	{
		mCallRouteExceptions.setOnChangedListener(null);
		if (mCallRouteExceptions.size() > 0)
		{
			CallRouteExceptionsAdapter adapter = new CallRouteExceptionsAdapter(
					this, mCallRouteExceptions);
			 adapter.sort(String.CASE_INSENSITIVE_ORDER);
			setListAdapter(adapter);
		}
		else setListAdapter(null);
		mCallRouteExceptions.setOnChangedListener(new CallRouteExceptions.OnChangedListener()
		{
			public void onChanged()
			{
				 updateListAdapter();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.routeexceptions_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.findItem(R.id.Clear).setEnabled(mCallRouteExceptions.size() > 0);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
			case R.id.Add:
				showDialog(R.id.Add);
				return true;
	
			case R.id.Clear:
				showDialog(R.id.Clear);
				return true;
		}
		return false;
	}

	private static final int MENU_EDIT = 1;
	private static final int MENU_REMOVE = 2;
	private int mDialogTargetPosition = -1;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);

		if (view == getListView())
		{
			try
			{
				menu.setHeaderTitle((String)getListView()
						.getItemAtPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position));
				menu.add(0, MENU_EDIT, Menu.NONE, R.string.settings_editrouteexception);
				menu.add(0, MENU_REMOVE, Menu.NONE, R.string.settings_removerouteexception);
			}
			catch (Exception exp) { }
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_EDIT:
				try
				{
					AdapterView.AdapterContextMenuInfo menuInfo =
						(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
					mDialogTargetPosition = menuInfo.position;
					showDialog(MENU_EDIT);
					return true;
				}
				catch (Exception exp) {}
				break;
	
			case MENU_REMOVE:
				try
				{
					AdapterView.AdapterContextMenuInfo menuInfo =
						(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
					mCallRouteExceptions.remove(menuInfo.position);
					return true;
				}
				catch (Exception exp) {}
				break;
		}

		return super.onContextItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			// remove all exceptions
			case R.id.Clear:
			{
				return TextDialog.create(this,
						getString(R.string.settings_routeexceptions),
						getString(R.string.ask_removeall),
						TextDialog.DEFAULT_EDIT_ICON).setPositiveButton(
						R.string.yes, new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialogInterface,
									int id)
							{
								mCallRouteExceptions.clear();
								removeDialog(R.id.Clear);
							}
						}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialogInterface, int id)
							{
								removeDialog(R.id.Clear);
							}
						}).create();
			}
	
				// add new exception
			case R.id.Add: 
			{
				return TextDialog.createEdit(this,
						getString(R.string.settings_addrouteexception), "",
						InputType.TYPE_CLASS_PHONE)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialogInterface, int id)
							{
								String pattern = PhoneNumberHelper.stripSeparators(
										((TextView)(((Dialog)dialogInterface)
										.findViewById(R.id.message))).getText()
										.toString());
								if (pattern.length() > 0)
									mCallRouteExceptions.add(pattern);
								removeDialog(R.id.Add);
							}
						}).setNegativeButton(android.R.string.cancel,
									new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialogInterface, int id)
							{
								removeDialog(R.id.Add);
							}
						}).create();
			}
	
			case MENU_EDIT:
			{
				if ((mDialogTargetPosition < 0)
						|| (mCallRouteExceptions.size() <= mDialogTargetPosition))
					break;
	
				return TextDialog.createEdit(this,
						getString(R.string.settings_editrouteexception),
						mCallRouteExceptions.get(mDialogTargetPosition),
						InputType.TYPE_CLASS_PHONE)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialogInterface, int id)
							{
								String pattern = PhoneNumberHelper.stripSeparators(
										((TextView)(((Dialog)dialogInterface)
										.findViewById(R.id.message))).getText()
										.toString());
								if (!pattern.equals(mCallRouteExceptions
										.get(mDialogTargetPosition)))
								{
									if (pattern.length() > 0)
										mCallRouteExceptions.set(mDialogTargetPosition,
												pattern);
									else
										mCallRouteExceptions
												.remove(mDialogTargetPosition);
								}
								removeDialog(MENU_EDIT);
							}
						}).setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialogInterface, int id)
							{
								removeDialog(MENU_EDIT);
							}
						}).create();
			}
		}
		return null;
	}
	
	/**
	 * Preparations on settings to do on app's start
	 * @param context context for reading and writing the settings
	 * @param firstRun true for first run after install
	 */
	public static void prepareSettings(Context context, boolean firstRun)
	{
		if (firstRun) CallRouteExceptions.saveDefault(context);
	}
}
