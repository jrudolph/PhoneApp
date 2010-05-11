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

import de.avm.android.fritzapp.R;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

/**
 *	Wrapper for dialpad
 *	1st version to get code out of consuming activity
 *
 *	keyboard input hat to be relayed to this instance through interface
 * 	View.OnKeyListener
 * 
 * 	parent must have layout/dtmf_twelve_key_dialer in it's layout
 */
public class Dialpad extends SlidingDrawer implements View.OnClickListener,
		View.OnLongClickListener, View.OnKeyListener
{
    /**
     * Callback invoked when a digit has been added.
     */
    public static interface OnDtmfDigitListener
    {
        /**
         * @param digit 
         * @param dtmfTone as android.media.ToneGenerator.TONE_DTMF_*
         */
        public void onDtmfDigit(char digit, int dtmfTone);
    }

	private static final String PREF_REDIAL = "redial";

	private OnDtmfDigitListener mOnDtmfDigitListener = null;
	
	private boolean mInitiallyOpen = false;
	private String mInitialText = null;
	private int mHintId = 0;
	
	public Dialpad(Context context, AttributeSet attrs)
	{
        this(context, attrs, 0);
	}
	
	public Dialpad(Context context, AttributeSet attrs, int defStyle)
	{
        super(context, attrs, defStyle);
	}

    @Override
    protected void onFinishInflate()
    {
    	super.onFinishInflate();

        View parent = getContent();
	    mDigits = (EditText)parent.findViewById(R.id.digits);
	    if (mHintId > 0) mDigits.setHint(mHintId);
	    if (mInitialText != null) mDigits.setText(mInitialText);

	    // keyboard input
	    parent.setOnKeyListener(this);
	    
	    // handle button clicks
	    parent.findViewById(R.id.digits_back).setOnClickListener(this);
	    parent.findViewById(R.id.digits_back).setOnLongClickListener(this);
        for (int viewId : mButtonIds)
        	parent.findViewById(viewId).setOnClickListener(this);
        
        // has to be opened?
        if (mInitiallyOpen) open();
    }

    public void setInitiallyOpen(boolean open)
    {
    	mInitiallyOpen = open;
		if (open && (getContent() != null) && !isOpened())
    	{
    		// init already done and not set properly
			open();
    	}
    }
    
	/**
	 * Gets current text
	 * @return
	 */
	public String getText()
	{
		return mDigits.getText().toString();
	}

	/**
	 * Sets current text
	 * @return
	 */
	public void setText(String text)
	{
		mInitialText = text;
		if (mDigits != null) mDigits.setText(text);
	}

	/**
	 * Sets hint for text input field
	 * @return
	 */
	public void setHint(int resid)
	{
		mHintId = resid;
        if (mDigits != null) mDigits.setHint(resid);
	}
	
	/**
	 * Saves text as redial number
	 * @param context
	 * @param text
	 */
	public static void saveAsRedial(Context context, String text)
	{
		if (text.length() > 0)
		{
			Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
			edit.putString(PREF_REDIAL, text);
			edit.commit();
		}
	}
	
	/**
	 * Saves edit field content as redial number
	 * @param context
	 */
	public void saveTextAsRedial(Context context)
	{
		saveAsRedial(context, getText());
		mDigits.setText("");
	}

	/**
	 * Restores redial number to edit field
	 * @param context
	 */
	public void loadRedial(Context context)
	{
		String redial = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(PREF_REDIAL, "");
		if (redial.length() > 0)
		    mDigits.setText(redial);
	}

	/**
	 * Adds listener for listening to text changes
	 * @param textWatcher
	 */
	public void addTextChangedListener(TextWatcher textWatcher)
	{
		mDigits.addTextChangedListener(textWatcher);
	}
	
	/**
	 * Removes listener for listening to text changes
	 * @param textWatcher
	 */
	public void removeTextChangedListener(TextWatcher textWatcher)
	{
		mDigits.removeTextChangedListener(textWatcher);
	}

    /**
     * Sets the listener that receives a notification when the drawer becomes open.
     *
     * @param onDrawerOpenListener The listener to be notified when the drawer is opened.
     */
    public void setOnDtmfDigitListener(OnDtmfDigitListener onDtmfDigitListener)
    {
        mOnDtmfDigitListener = onDtmfDigitListener;
    }

	private static int mButtonIds[] = new int[]
  	{
  		R.id.one, R.id.two, R.id.three, R.id.four, R.id.five,
  		R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.zero,
  		R.id.pound, R.id.star
  	};

  	private static char mCharacters[] = new char[]
  	{
  		'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '#', '*'
  	};
  	
  	private static int mDtmfTones[] = new int[]
  	{
  		ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_2,
  		ToneGenerator.TONE_DTMF_3, ToneGenerator.TONE_DTMF_4,
  		ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_6,
  		ToneGenerator.TONE_DTMF_7, ToneGenerator.TONE_DTMF_8,
  		ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_0,
  		ToneGenerator.TONE_DTMF_P, ToneGenerator.TONE_DTMF_S
  	};

	EditText mDigits = null;
	
	@Override
	public void onClick(View v)
	{
		if (isOpened())
		{
	        int viewId = v.getId();
	        if (viewId == R.id.digits_back)
	        {
		    	removeLastDigit();
	        }
	        else
	        {
	    		for(int ii = 0; ii < mButtonIds.length; ++ii)
	    		{
	    			if (mButtonIds[ii] == viewId)
	    			{
	    	        	appendDigit(mCharacters[ii]);
	    	        	break;
	    			}
	    		}
	        }
		}
	}

	@Override
	public boolean onLongClick(View v)
	{
		if (isOpened())
		{
	        int viewId = v.getId();
	        if (viewId == R.id.digits_back)
	        {
				mDigits.setText("");
				return true;
	        }
		}
		return false;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event)
	{
		if (isOpened())
		{
	        char digit = event.getNumber();
			for(int ii = 0; ii < mCharacters.length; ++ii)
			{
				if (mCharacters[ii] == digit)
				{
    	        	appendDigit(digit);
    	        	break;
				}
			}
		}
		
		return false;
	}
	
	private void appendDigit(char digit)
	{
        mDigits.getText().append(digit);
        
        if (mOnDtmfDigitListener != null)
        {
    		for(int ii = 0; ii < mCharacters.length; ++ii)
    		{
    			if (mCharacters[ii] == digit)
    			{
    				mOnDtmfDigitListener.onDtmfDigit(digit, mDtmfTones[ii]);
    				break;
    			}
    		}
        }
	}
	
	private void removeLastDigit()
	{
		Editable text = mDigits.getText();
		if (text != null)
		{
			int len = text.length();
			if (len > 0) text.delete(len - 1, len);
		}
	}
}
