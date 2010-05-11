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

package de.avm.android.fritzapp.sipua.ui;

import java.util.concurrent.LinkedBlockingQueue;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.SystemClock;

/**
 * Background Task to send DTMF info over connection and to play
 * DTMF tones locally
 */
public class SendDtmfAsyncTask extends AsyncTask<Integer, Integer, Integer>
{
	/**
	 * Enqueue DTMF to send
	 * @param digit
	 * @param dtmfTone
	 * @throws InterruptedException 
	 */
	public void put(char digit, int dtmfTone)
	{
		try 
		{
			mQueue.put(new Dtmf(digit, dtmfTone));
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Cancel all pending DTMF
	 */
	public void cancel()
	{
		synchronized(mIsClosing)
		{
			mQueue.clear();
			if (mIsClosing)
			{
				try { mQueue.put(new Dtmf((char)0, 0)); }
				catch (InterruptedException e) { }
			}
		}
	}
	
	/**
	 * Stop async task after sending what is already in the queue
	 */
	public void close()
	{
		synchronized(mIsClosing) { mIsClosing = true; }
		try
		{
			mQueue.put(new Dtmf((char)0, 0));
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	private class Dtmf
	{
		public char Digit;
		public int Tone;
		
		public Dtmf(char digit, int tone)
		{
			Digit = digit;
			Tone = tone;
		}
	}
	
	private LinkedBlockingQueue<Dtmf> mQueue = new LinkedBlockingQueue<Dtmf>();
	private Boolean mIsClosing = false;

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Integer doInBackground(Integer... parms)
	{
		ToneGenerator toneGenerator = null;
		try
		{
			toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC,
					(int)(ToneGenerator.MAX_VOLUME * Sipdroid.getEarGain()));
			
			while (true)
			{
				// done?
				synchronized(mIsClosing)
				{
					if (mIsClosing) break;
				}
				Dtmf dtmf = null;
				try { dtmf = mQueue.take(); }
            	catch(InterruptedException e) {}
				synchronized(mIsClosing)
				{
					if (mIsClosing) break;
				}
				if ((dtmf == null) || (dtmf.Digit == 0)) break;

				// start playing tone
				if (toneGenerator != null)
					toneGenerator.startTone(dtmf.Tone);
				
				// send to connection
                long time = SystemClock.elapsedRealtime();
                Receiver.engine(Receiver.mContext).info(dtmf.Digit, 250);

				// stop playing tone
                time = 250 - (SystemClock.elapsedRealtime() - time);
                if ((toneGenerator != null) && (time > 0))
                {
                	try { Thread.sleep(time); }
                	catch(InterruptedException e) {}
                	finally
                	{
    					toneGenerator.stopTone();
                	}
                }
                Thread.sleep(250);
			}
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		finally
		{
			if (toneGenerator != null) toneGenerator.release();
		}
		
		return 0;
	}
}
