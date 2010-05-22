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
package org.sipdroid.media;

import org.sipdroid.net.SipdroidSocket;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;

import de.avm.android.fritzapp.sipua.ui.Sipdroid;

/** Audio launcher based on javax.sound  */
public class JAudioLauncher implements MediaLauncher
{  
   /** Event logger. */
   Log log=null;

   /** Sample rate [bytes] */
   int sample_rate=8000;
   /** Sample size [bytes] */
   int sample_size=1;
   /** Frame size [bytes] */
   int frame_size=160;
   /** Frame rate [frames per second] */
   int frame_rate=50; //=sample_rate/(frame_size/sample_size);
   boolean signed=false; 
   boolean big_endian=false;

   //String filename="audio.wav"; 

   /** Test tone */
   public static final String TONE="TONE";

   /** Test tone frequency [Hz] */
   public static int tone_freq=100;
   /** Test tone ampliture (from 0.0 to 1.0) */
   public static double tone_amp=1.0;

   /** Runtime media process */
   Process media_process=null;
   
   int dir; // duplex= 0, recv-only= -1, send-only= +1; 

   SipdroidSocket socket=null;
   RtpStreamSender sender=null;
   RtpStreamReceiver receiver=null;
   
   // change DTMF
   boolean useDTMF = false; // zero means not use outband DTMF
   
   /** Costructs the audio launcher */
   public JAudioLauncher(RtpStreamSender rtp_sender, RtpStreamReceiver rtp_receiver, Log logger)
   {  log=logger;
      sender=rtp_sender;
      receiver=rtp_receiver;
   }

   /** Costructs the audio launcher */
   public JAudioLauncher(int local_port, String remote_addr, int remote_port, int direction, String audiofile_in, String audiofile_out, int sample_rate, int sample_size, int frame_size, Log logger, int payload_type, int dtmf_pt)
   {  log=logger;
      frame_rate=sample_rate/frame_size;
      useDTMF = (dtmf_pt != 0);
      try
      {  socket=new SipdroidSocket(local_port);
         socket.setTrafficClass(160); // NB: Type of Service
         dir=direction;
         // sender
         if (dir>=0)
         {  printLog("new audio sender to "+remote_addr+":"+remote_port,LogLevel.MEDIUM);
            //audio_input=new AudioInput();
            sender=new RtpStreamSender(true,payload_type,frame_rate,frame_size,socket,remote_addr,remote_port);
            sender.setSyncAdj(2);
            sender.setDTMFpayloadType(dtmf_pt);
         }
         
         // receiver
         if (dir<=0)
         {  printLog("new audio receiver on "+local_port,LogLevel.MEDIUM);
            receiver=new RtpStreamReceiver(socket,payload_type);
         }
      }
      catch (Exception e) {  printException(e,LogLevel.HIGH);  }
   }

   /** Checks if audio engine works correctly */
   public boolean mediaOk(boolean wait) {
	   if(sender == null || receiver == null) {
		   return false;
	   }
	   for(int i=0; i < (wait ? 4 : 0); i++) {
		   if( (RtpStreamSender.RecordEngineState.STATE_NOTHING == sender.recordEngineInitialized()) ||
			   (RtpStreamReceiver.AudioEngineState.STATE_NOTHING == receiver.audioEngineInitialized())) {
			   try {
				   android.util.Log.d("JAudioLauncher", "waiting for audio engine initialization...");
				   Thread.sleep(250);
			   }
			   catch (InterruptedException e) {
			   }
		   }
		   else
			   break;
	   }
	   return (RtpStreamSender.RecordEngineState.STATE_INITIALIZED == sender.recordEngineInitialized() &&
			   RtpStreamReceiver.AudioEngineState.STATE_INITIALIZED == receiver.audioEngineInitialized());
   }
   
   /** Starts media application */
   public boolean startMedia()
   {
	   boolean ok = true;
	   printLog("starting java audio..",LogLevel.HIGH);

	   if (sender!=null) {
		   printLog("start sending",LogLevel.LOW);
		   sender.start();
	   }
	   else
		   ok = false;
	   
	   if (receiver!=null) {
		   printLog("start receiving",LogLevel.LOW);
		   receiver.start();
	   }
	   else {
		   if(sender != null)
			   sender.halt();
		   ok = false;
	   }
	   
	   if(!ok)
		   return false;
	   
	   return mediaOk(true);
   }
   
   /** Stops media application */
   public boolean stopMedia()
   {  printLog("halting java audio..",LogLevel.HIGH);    
      if (sender!=null)
      {  sender.halt(); sender=null;
         printLog("sender halted",LogLevel.LOW);
      }      
       if (receiver!=null)
      {  receiver.halt(); receiver=null;
         printLog("receiver halted",LogLevel.LOW);
      }      
      socket.close();
      return true;
   }

   public boolean muteMedia()
   {
	   if (sender != null)
		   return sender.mute();
	   return false;
   }
   
   public int speakerMedia(int mode)
   {
	   if (receiver != null)
		   return receiver.speaker(mode);
	   return 0;
   }
   
   //change DTMF
   /** Send outband DTMF packets **/
   public boolean sendDTMF(char c) {
	   if(!useDTMF) return false;
	   sender.sendDTMF(c);
	   return true;
   }

   // ****************************** Logs *****************************

   /** Adds a new string to the default Log */
   private void printLog(String str)
   {  printLog(str,LogLevel.HIGH);
   }

   /** Adds a new string to the default Log */
   private void printLog(String str, int level)
   {
	  if (Sipdroid.release) return;
	  if (log!=null) log.println("AudioLauncher: "+str,level+SipStack.LOG_LEVEL_UA);  
      if (level<=LogLevel.HIGH) System.out.println("AudioLauncher: "+str);
   }

   /** Adds the Exception message to the default Log */
   void printException(Exception e,int level)
   { 
	  if (Sipdroid.release) return;
	  if (log!=null) log.printException(e,level+SipStack.LOG_LEVEL_UA);
      if (level<=LogLevel.HIGH) e.printStackTrace();
   }

}
