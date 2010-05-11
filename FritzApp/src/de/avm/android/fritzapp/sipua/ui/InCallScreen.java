/*
 * Copyright (C) 2010 AVM GmbH <info@avm.de>
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.avm.android.fritzapp.sipua.ui;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.net.SipdroidSocket;
import de.avm.android.fritzapp.R;
import de.avm.android.fritzapp.gui.Dialpad;
import de.avm.android.fritzapp.gui.SlidingDrawer;
import de.avm.android.fritzapp.sipua.UserAgent;
import de.avm.android.fritzapp.sipua.phone.Call;
import de.avm.android.fritzapp.sipua.phone.CallerInfo;
import de.avm.android.fritzapp.sipua.phone.CallerInfoAsyncQuery;
import de.avm.android.fritzapp.sipua.phone.Connection;
import de.avm.android.fritzapp.sipua.phone.ContactsAsyncHelper;
import de.avm.android.fritzapp.sipua.phone.Phone;
import de.avm.android.fritzapp.sipua.phone.PhoneUtils;


import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class InCallScreen extends CallScreen
		implements CallerInfoAsyncQuery.OnQueryCompleteListener
{
	final static String TAG = "InCallScreen";
	
	final int MSG_ANSWER = 1;
	final int MSG_ANSWER_SPEAKER = 2;
	final int MSG_BACK = 3;
	final int MSG_TICK = 4;
	
	final int SCREEN_OFF_TIMEOUT = 12000;
	
	Phone ccPhone;
	int oldtimeout;
    private Chronometer mElapsedTime;
    private TextView mActionLabel;
    private ImageView mActionImage;
    private TextView mName;
    private TextView mNumber;
    private Button mCallNow;
    private Button mHangup;
    private ImageButton mSlideDialpad;
    private ImageButton mReject;
    private ImageButton mSpeaker;
	private Dialpad mDialpad = null;
	private SendDtmfAsyncTask mDtmfSender = null;

    // Track the state for the caller info.
    private ContactsAsyncHelper.ImageTracker mPhotoTracker;

	void screenOff(boolean off) {
        ContentResolver cr = getContentResolver();
        
        if (off) {
        	if (oldtimeout == 0) {
        		oldtimeout = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000);
	        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_OFF_TIMEOUT);
        	}
        } else {
        	if (oldtimeout == 0 && Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000) == SCREEN_OFF_TIMEOUT)
        		oldtimeout = 60000;
        	if (oldtimeout != 0) {
	        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, oldtimeout);
        		oldtimeout = 0;
        	}
        }
	}
	
	@Override
	public void onPause() {
		super.onPause();
    	if (!Sipdroid.release) Log.i("SipUA:","on pause");
    	switch (Receiver.call_state) {
    	case UserAgent.UA_STATE_INCOMING_CALL:
    		Receiver.moveTop();
    		break;
    	case UserAgent.UA_STATE_IDLE:
    		displayMainCallStatus();
			mHandler.sendEmptyMessageDelayed(MSG_BACK, 2000);
    		break;
    	}
		if (socket != null) {
			socket.close();
			socket = null;
		}
		screenOff(false);
		if (mElapsedTime != null) mElapsedTime.stop();
	}

	void moveBack() {
		if (Receiver.ccConn != null && !Receiver.ccConn.isIncoming()&&
			!Sipdroid.mBackToMainActivity) {
			// after an outgoing call don't fall back to the contact
			// or call log because it is too easy to dial accidentally from there
			startActivity(Receiver.createHomeIntent());
		}
		moveTaskToBack(true);
	}
	
	SipdroidSocket socket;
	Context mContext = this;
	int speakermode;
	long speakervalid;

	@Override
	public void onResume() {
		super.onResume();
    	if (!Sipdroid.release) Log.i("SipUA:","on resume");
		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCOMING_CALL:
			if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Sipdroid.PREF_AUTOON, false) &&
					!mKeyguardManager.inKeyguardRestrictedInputMode())
				mHandler.sendEmptyMessageDelayed(MSG_ANSWER, 1000);
			else if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Sipdroid.PREF_AUTOONDEMAND, false) &&
					PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Sipdroid.PREF_AUTOONDEMAND, false))
				mHandler.sendEmptyMessageDelayed(MSG_ANSWER_SPEAKER, 10000);
			break;
		case UserAgent.UA_STATE_INCALL:
			if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) &&
				(mDialpad != null))
			{
				mDialpad.close();
			}
		    screenOff(true);
			break;
		case UserAgent.UA_STATE_IDLE:
			if (!mHandler.hasMessages(MSG_BACK))
				moveBack();
			break;
		}
		if ((Receiver.call_state != UserAgent.UA_STATE_INCALL) &&
			(mDialpad != null))
		{
			mDialpad.close();
		}
		displayMainCallStatus();
        mHandler.sendEmptyMessage(MSG_TICK);
	}
	
    Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		case MSG_ANSWER:
        		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
        			answer();
        		break;
    		case MSG_ANSWER_SPEAKER:
        		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
        			answer();
    				Receiver.engine(mContext).speaker(AudioManager.MODE_NORMAL);
        		}
        		break;
    		case MSG_BACK:
    			moveBack();
    			break;
    		case MSG_TICK:
    			// TODO update online time display
    			break;
    		}
    	}
    };

    @Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.incall);
		
        mElapsedTime = (Chronometer)findViewById(R.id.Duration);
        mActionLabel = (TextView)findViewById(R.id.ActionLabel);
        mActionImage = (ImageView)findViewById(R.id.ActionImage);
        mName = (TextView)findViewById(R.id.Name);
        mNumber = (TextView)findViewById(R.id.Number);

        // Dialpad
		mDialpad = (Dialpad)findViewById(R.id.dtmf_dialer);
		mDialpad.setInitiallyOpen(false);
		mDialpad.setHint(R.string.dtmf_hint);
		mSlideDialpad = ((ImageButton)findViewById(R.id.SlideDialpad));
		mDialpad.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener()
		{
			public void onDrawerClosed()
			{
				mSlideDialpad.setImageResource(R.drawable.btn_dialpadopen);
				SendDtmfAsyncTask sender = mDtmfSender;
				if (sender != null)
				{
					mDtmfSender = null;
					sender.close();
				}
			}
		});
		mDialpad.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener()
		{
			public void onDrawerOpened()
			{
				mDialpad.setText("");
				mSlideDialpad.setImageResource(R.drawable.btn_dialpadclose);
				if (mDtmfSender == null)
				{
					mDtmfSender = (SendDtmfAsyncTask)new SendDtmfAsyncTask()
							.execute((Integer[])null);
				}
			}
		});
		mDialpad.setOnDtmfDigitListener(new Dialpad.OnDtmfDigitListener()
		{
			public void onDtmfDigit(char digit, int dtmfTone)
			{
				SendDtmfAsyncTask sender = mDtmfSender;
				if (sender != null) sender.put(digit, dtmfTone);
			}
		});
        
		// Footer Buttons
		mSlideDialpad.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				mDialpad.animateToggle();
			}
		});
        mCallNow = (Button)findViewById(R.id.CallNow);
        mCallNow.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				answer();
			}
		});
        mHangup = (Button)findViewById(R.id.Hangup);
        mHangup.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				SendDtmfAsyncTask sender = mDtmfSender;
				if (sender != null) sender.cancel();
				Receiver.engine(InCallScreen.this).rejectcall();
			}
		});
        mReject = (ImageButton)findViewById(R.id.Reject);
        mReject.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				reject();      
			}
		});
        mSpeaker = (ImageButton)findViewById(R.id.Speaker);
        mSpeaker.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Receiver.engine(InCallScreen.this)
						.speaker((RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL) ?
								AudioManager.MODE_IN_CALL : AudioManager.MODE_NORMAL);
                updateSpeakerButton();
			}
		});
        
        displayOnHoldCallStatus(ccPhone, null);
        displayOngoingCallStatus(ccPhone, null);
        
        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        mPhotoTracker = new ContactsAsyncHelper.ImageTracker();
    }
		
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		if (Receiver.call_state == UserAgent.UA_STATE_INCALL || Receiver.call_state == UserAgent.UA_STATE_HOLD) {
//			menu.findItem(HOLD_MENU_ITEM).setVisible(true);
			menu.findItem(MUTE_MENU_ITEM).setVisible(true);
//			menu.findItem(TRANSFER_MENU_ITEM).setVisible(true);
		} else {
//			menu.findItem(HOLD_MENU_ITEM).setVisible(false);
			menu.findItem(MUTE_MENU_ITEM).setVisible(false);
//			menu.findItem(TRANSFER_MENU_ITEM).setVisible(false);
		}
		
		return result;
	}

	public void reject() {
		if (Receiver.ccCall != null) {
			Receiver.stopRingtone();
			Receiver.ccCall.setState(Call.State.DISCONNECTED);
			displayMainCallStatus();
			SendDtmfAsyncTask sender = mDtmfSender;
			if (sender != null) sender.cancel();
			if (mDialpad != null) mDialpad.close();
		}
        (new Thread() {
			public void run() {
        		Receiver.engine(mContext).rejectcall();
			}
		}).start();   	
    }
	
	public void answer() {
        (new Thread() {
			public void run() {
        		Receiver.engine(mContext).answercall();
			}
		}).start();   
		if (Receiver.ccCall != null) {
			Receiver.stopRingtone();
			Receiver.ccCall.setState(Call.State.ACTIVE);
			Receiver.ccCall.base = SystemClock.elapsedRealtime();
			displayMainCallStatus();
			if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) &&
					(mDialpad != null))
				mDialpad.close();
		}
	}
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_CALL:
        	switch (Receiver.call_state) {
        	case UserAgent.UA_STATE_INCOMING_CALL:
        		answer();
        		break;
        	case UserAgent.UA_STATE_INCALL:
        	case UserAgent.UA_STATE_HOLD:
       			Receiver.engine(this).togglehold();
       			break;
        	}
            // consume KEYCODE_CALL so PhoneWindow doesn't do anything with it
            return true;

        case KeyEvent.KEYCODE_BACK:
        	if ((mDialpad != null) && mDialpad.isOpened())
        	{
				SendDtmfAsyncTask sender = mDtmfSender;
				if (sender != null) sender.cancel();
        		mDialpad.animateClose();
        	}
        	else if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
        	{
        		reject();
        	}
            return true;

        case KeyEvent.KEYCODE_CAMERA:
            // Disable the CAMERA button while in-call since it's too
            // easy to press accidentally.
        	return true;
        	
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
        		Receiver.stopRingtone();
        		return true;
        	}
        	RtpStreamReceiver.adjust(keyCode);
        	return true;
        }
        return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
        //case KeyEvent.KEYCODE_VOLUME_DOWN:
        //case KeyEvent.KEYCODE_VOLUME_UP:
        //	return true;
		}
		Receiver.pstn_time = 0;
		return false;
	}
	
	/**
     * Updates the main block of caller info
     * according to call state
	 */
	private void displayMainCallStatus()
	{
		if (Receiver.ccCall == null) return;

        Call.State state = Receiver.ccCall.getState(); 
        switch (state)
        {
            case DIALING:
            case ALERTING:
        		mActionLabel.setVisibility(View.VISIBLE);
        		mActionLabel.setText(getString(R.string.callstate_dialing));
        		mElapsedTime.setVisibility(View.GONE);
                mCallNow.setVisibility(View.GONE);
                mHangup.setVisibility(View.VISIBLE);
                mReject.setVisibility(View.GONE);
                updateSpeakerButton();
                mSpeaker.setVisibility(View.VISIBLE);
                mSlideDialpad.setVisibility(View.GONE);
                if (mDialpad.isOpened()) mDialpad.close();
                break;

            case INCOMING:
            case WAITING:
        		mActionLabel.setVisibility(View.VISIBLE);
        		mActionLabel.setText(getString(R.string.callstate_incoming));
        		mElapsedTime.setVisibility(View.GONE);
                mCallNow.setVisibility(View.VISIBLE);
                mHangup.setVisibility(View.GONE);
                mReject.setVisibility(View.VISIBLE);
                mSpeaker.setVisibility(View.GONE);
                mSlideDialpad.setVisibility(View.GONE);
                if (mDialpad.isOpened()) mDialpad.close();
                break;

        	case ACTIVE:
        		mActionLabel.setVisibility(View.GONE);
        		mElapsedTime.setVisibility(View.VISIBLE);
                mElapsedTime.setBase(Receiver.ccCall.base);
                mElapsedTime.start();
                mCallNow.setVisibility(View.GONE);
                mHangup.setVisibility(View.VISIBLE);
                mReject.setVisibility(View.GONE);
                updateSpeakerButton();
                mSpeaker.setVisibility(View.VISIBLE);
                mSlideDialpad.setVisibility(View.VISIBLE);
                break;

            case HOLDING:
        		mActionLabel.setVisibility(View.VISIBLE);
        		mActionLabel.setText(getString(R.string.callstate_onhold));
        		mElapsedTime.setVisibility(View.GONE);
                mCallNow.setVisibility(View.GONE);
                mHangup.setVisibility(View.VISIBLE);
                mReject.setVisibility(View.GONE);
                updateSpeakerButton();
                mSpeaker.setVisibility(View.VISIBLE);
                mSlideDialpad.setVisibility(View.GONE);
                if (mDialpad.isOpened()) mDialpad.close();
                break;

        	case DISCONNECTED:
                mElapsedTime.stop();
                if (mElapsedTime.getVisibility() == View.GONE)
                	mActionLabel.setText(getString(R.string.callstate_hangup));
                mCallNow.setVisibility(View.GONE);
                mHangup.setVisibility(View.GONE);
                mReject.setVisibility(View.GONE);
                mSpeaker.setVisibility(View.GONE);
                mSlideDialpad.setVisibility(View.GONE);
                if (mDialpad.isOpened()) mDialpad.close();
                break;

            case IDLE:
                // The "main CallCard" should never display an idle call!
                Log.w(TAG, "displayMainCallStatus: IDLE call!");
        		mActionLabel.setVisibility(View.GONE);
        		mElapsedTime.setVisibility(View.GONE);
                mCallNow.setVisibility(View.GONE);
                mHangup.setVisibility(View.GONE);
                mReject.setVisibility(View.GONE);
                mSpeaker.setVisibility(View.GONE);
                mSlideDialpad.setVisibility(View.GONE);
                if (mDialpad.isOpened()) mDialpad.close();
                break;

            default:
                Log.w(TAG, "displayMainCallStatus: unexpected call state: " + state);
                break;
        }

        // Update onscreen info for a regular call (which presumably
        // has only one connection.)
        Connection conn = Receiver.ccCall.getEarliestConnection();

        boolean isPrivateNumber = false; // TODO: need isPrivate() API

        if (conn == null)
        {
        	Log.d(TAG, "displayMainCallStatus: connection is null, using default values.");
            // if the connection is null, we run through the behaviour
            // we had in the past, which breaks down into trivial steps
            // with the current implementation of getCallerInfo and
            // updateDisplayForPerson.
            updateDisplayForPerson(null, isPrivateNumber, false, Receiver.ccCall);
        }
        else
        {
            Log.d(TAG, "  - CONN: " + conn + ", state = " + conn.getState());

            // make sure that we only make a new query when the current
            // callerinfo differs from what we've been requested to display.
            boolean runQuery = true;
            Object o = conn.getUserData();
            if (o instanceof PhoneUtils.CallerInfoToken)
                runQuery = mPhotoTracker.isDifferentImageRequest(
                        ((PhoneUtils.CallerInfoToken) o).currentInfo);
            else
                runQuery = mPhotoTracker.isDifferentImageRequest(conn);

            if (runQuery)
            {
            	Log.d(TAG, "- displayMainCallStatus: starting CallerInfo query...");
                PhoneUtils.CallerInfoToken info =
                        PhoneUtils.startGetCallerInfo(this, conn, this, Receiver.ccCall);
                updateDisplayForPerson(info.currentInfo, isPrivateNumber, !info.isFinal, Receiver.ccCall);
            }
            else
            {
                // No need to fire off a new query.  We do still need
                // to update the display, though (since we might have
                // previously been in the "conference call" state.)
            	Log.d(TAG, "- displayMainCallStatus: using data we already have...");
                if (o instanceof CallerInfo)
                {
                    CallerInfo ci = (CallerInfo) o;
                    Log.d(TAG, "   ==> Got CallerInfo; updating display: ci = " + ci);
                    updateDisplayForPerson(ci, false, false, Receiver.ccCall);
                }
                else if (o instanceof PhoneUtils.CallerInfoToken)
                {
                    CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                    Log.d(TAG, "   ==> Got CallerInfoToken; updating display: ci = " + ci);
                    updateDisplayForPerson(ci, false, true, Receiver.ccCall);
                }
                else
                {
                    Log.w(TAG, "displayMainCallStatus: runQuery was false, "
                          + "but we didn't have a cached CallerInfo object!  o = " + o);
                    // TODO: any easy way to recover here (given that
                    // the CallCard is probably displaying stale info
                    // right now?)  Maybe force the CallCard into the
                    // "Unknown" state?
                }
            }
        }

        // In some states we override the "photo" ImageView to be an
        // indication of the current state, rather than displaying the
        // regular photo as set above.
        updateActionImageForCallState(Receiver.ccCall);
	}

	private void updateSpeakerButton()
	{
		mSpeaker.setImageResource((RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL) ?
				R.drawable.btn_speakeroff : R.drawable.btn_speaker);
	}
	
	/**
	 * Sets ActionImage according to call state
	 * currently we override the photo in every state!!
	 * 
	 * @param call
	 */
	private void updateActionImageForCallState(Call call)
	{
        Call.State state = call.getState();
        switch (state)
        {
	        case IDLE: break;
	    	
	        case ACTIVE:
	        case HOLDING:
	        	mActionImage.setImageResource(R.drawable.callstate_active);
                break;

	        case INCOMING:
	        case WAITING:
	        	mActionImage.setImageResource(R.drawable.callstate_incoming);
                break;

	        case DISCONNECTED:
	        	mActionImage.setImageResource(R.drawable.callstate_hangup);
                break;

            case DIALING:
            case ALERTING:
            	mActionImage.setImageResource(R.drawable.callstate_dialing);
                break;

            default:
                Log.w(TAG, "updatePhotoForCallState: unexpected call state: " + state);
                mActionImage.setVisibility(View.GONE);
                return;
        }
    	mActionImage.setVisibility(View.VISIBLE);
	}

	/**
     * Updates the name and number label fields
     * according to call state
     *
     * If the current call is a conference call, use
     * updateDisplayForConference() instead.
     * 
	 * @param info
	 * @param isPrivateNumber
	 * @param isTemporary
	 * @param call
	 */
	private void updateDisplayForPerson(CallerInfo info,
            boolean isPrivateNumber, boolean isTemporary, Call call)
	{
        String name;
        String displayNumber = null;
        String label = null;

        if (info != null)
        {
            if (TextUtils.isEmpty(info.name))
            {
            	name = (TextUtils.isEmpty(info.phoneNumber)) ?
            			getString(R.string.unknown) : info.phoneNumber;
            }
            else
            {
                name = info.name;
                displayNumber = info.phoneNumber;
                label = info.phoneLabel;
            }
        }
        else name = getString(R.string.unknown);
        mName.setText(name);
        mName.setVisibility(View.VISIBLE);

        if (displayNumber != null)
        {
            if (label != null)
            	displayNumber = label + " " + displayNumber;
            mNumber.setText(displayNumber);
            mNumber.setVisibility(View.VISIBLE);
        }
        else  mNumber.setVisibility(View.GONE);
	}

	/**
     * Updates the "Ongoing call" box in the "other call" info area
     * (ie. the stuff in the otherCallOngoingInfo block)
     * based on the specified Call.
     * Or, clear out the "ongoing call" box if the specified call
     * is null or idle.
	 * 
	 * @param phone
	 * @param call
	 */
	private void displayOngoingCallStatus(Phone phone, Call call)
    {
		// TODO implement hold and incoming on busy
	}

    /**
     * Updates the "on hold" box in the "other call" info area
     * (ie. the stuff in the otherCallOnHoldInfo block)
     * based on the specified Call.
     * Or, clear out the "on hold" box if the specified call
     * is null or idle.
     * 
     * @param phone
     * @param call
     */
    private void displayOnHoldCallStatus(Phone phone, Call call)
    {
		// TODO implement hold and incoming on busy
	}

	/* (non-Javadoc)
	 * @see de.avm.android.fritzapp.sipua.phone.CallerInfoAsyncQuery.OnQueryCompleteListener#onQueryComplete(int, java.lang.Object, de.avm.android.fritzapp.sipua.phone.CallerInfo)
	 */
	@Override
	public void onQueryComplete(int token, Object cookie, CallerInfo ci)
	{
        if (cookie instanceof Call)
        {
            // grab the call object and update the display for an individual call,
            // as well as the successive call to update image via call state.
            // If the object is a textview instead, we update it as we need to.
            Log.d(TAG, "callerinfo query complete, updating ui from displayMainCallStatus()");
            Call call = (Call) cookie;
            updateDisplayForPerson(ci, false, false, call);
            updateActionImageForCallState(call);

        }
        else if (cookie instanceof TextView)
        {
            Log.d(TAG, "callerinfo query complete, updating ui from ongoing or onhold");
            ((TextView)cookie).setText(PhoneUtils.getCompactNameFromCallerInfo(ci, (Context)this));
        }
	}
}
