/*
 * ============================================================================
 *                 The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 2002 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must  retain the above copyright  notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following  acknowledgment: "This product includes software
 *    developed by SuperBonBon Industries (http://www.sbbi.net/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "UPNPLib" and "SuperBonBon Industries" must not be
 *    used to endorse or promote products derived from this software without
 *    prior written permission. For written permission, please contact
 *    info@sbbi.net.
 *
 * 5. Products  derived from this software may not be called 
 *    "SuperBonBon Industries", nor may "SBBI" appear in their name, 
 *    without prior written permission of SuperBonBon Industries.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT,INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software  consists of voluntary contributions made by many individuals
 * on behalf of SuperBonBon Industries. For more information on 
 * SuperBonBon Industries, please see <http://www.sbbi.net/>.
 */
package de.avm.android.fritzapp.com.discovery;

/* BASED on UPNP by <sbbi.net>. Modified by AVM GmbH <info@avm.de> */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * This class can be used to listen for UPNP devices responses when a search message is sent by a control point
 * ( using the net.sbbi.upnp.Discovery.sendSearchMessage() method )
 * @author <a href="mailto:superbonbon@sbbi.net">SuperBonBon</a>
 * @version 1.0
 */

public class FritzBoxDiscoveryListener implements Runnable {
  
  private static boolean MATCH_IP = true;
  
  private static final int DEFAULT_TIMEOUT = 250;
 
  private Map registeredHandlers = new HashMap();
  
  private final Object REGISTRATION_PROCESS = new Object();
  
  private final static FritzBoxDiscoveryListener singleton = new FritzBoxDiscoveryListener();
  
  private boolean inService = false;
  private boolean daemon = true;
  
  private java.net.MulticastSocket skt;
  private DatagramPacket input;
  
  /**
	 * Instantiates a new fritz box discovery listener.
	 */
  private FritzBoxDiscoveryListener() {
  }
  
  /**
	 * Gets the single instance of FritzBoxDiscoveryListener.
	 * 
	 * @return single instance of FritzBoxDiscoveryListener
	 */
  public final static FritzBoxDiscoveryListener getInstance() {
    return singleton;
  }
  
  /**
	 * Sets the listener as a daemon thread.
	 * 
	 * @param daemon
	 *            daemon thread
	 */
  public void setDaemon( boolean daemon ) {
    this.daemon = daemon;
  }
  
  /**
	 * Registers an SSDP response message handler.
	 * 
	 * @param resultsHandler
	 *            the SSDP response message handler
	 * @param searchTarget
	 *            the search target
	 * 
	 * @throws IOException
	 *             if some errors occurs during SSDP search response messages
	 *             listener thread startup
	 */
  public void registerResultsHandler( FritzBoxDiscoveryResultsHandler resultsHandler, String searchTarget ) throws IOException {
    synchronized( REGISTRATION_PROCESS ) {
      if ( !inService ) startDevicesListenerThread();
      Set handlers = (Set)registeredHandlers.get( searchTarget );
      if ( handlers == null ) {
        handlers = new HashSet();
        registeredHandlers.put( searchTarget, handlers );
      }
      handlers.add( resultsHandler );
    }
  }

  /**
	 * Unregisters an SSDP response message handler.
	 * 
	 * @param resultsHandler
	 *            the SSDP response message handler
	 * @param searchTarget
	 *            the search target
	 */
  public void unRegisterResultsHandler( FritzBoxDiscoveryResultsHandler resultsHandler, String searchTarget ) {
    synchronized( REGISTRATION_PROCESS ) {
      Set handlers = (Set)registeredHandlers.get( searchTarget );
      if ( handlers != null ) {
        handlers.remove( resultsHandler );
        if ( handlers.size() == 0 ) {
          registeredHandlers.remove( searchTarget );
        }
      }
      if ( registeredHandlers.size() == 0 ) {
        stopDevicesListenerThread();
      }
    }
  }
  
  /**
	 * Start devices listener thread.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
  private void startDevicesListenerThread() throws IOException {
    synchronized( singleton ) {
      if ( !inService ) {
        
        this.startMultiCastSocket();
        Thread deamon = new Thread( this, "FritzBoxDiscoveryListener daemon" );
        deamon.setDaemon( daemon );
        deamon.start();
        while ( !inService ) {
          // wait for the thread to be started let's wait a few ms
          try {
            Thread.sleep( 2 );
          } catch( InterruptedException ex ) {
            // don t care
          }
        }
      }
    }
  }
  
  /**
	 * Stop devices listener thread.
	 */
  private void stopDevicesListenerThread() {
    synchronized( singleton ) {
      inService = false;
    }
  }
  
  /**
	 * Start multi cast socket.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
  private void startMultiCastSocket() throws IOException {
    int bindPort = FritzBoxDiscovery.DEFAULT_SSDP_SEARCH_PORT;
    String port = System.getProperty( "net.sbbi.upnp.Discovery.bindPort" );
    if ( port != null ) {
      bindPort = Integer.parseInt( port );
    }

    skt = new java.net.MulticastSocket( null );
    skt.bind( new InetSocketAddress( InetAddress.getByName( "0.0.0.0" ), bindPort ) );
    skt.setTimeToLive( FritzBoxDiscovery.DEFAULT_TTL );
    skt.setSoTimeout( DEFAULT_TIMEOUT );
    skt.joinGroup( InetAddress.getByName( FritzBoxDiscovery.SSDP_IP ) );

    byte[] buf = new byte[2048];
    input = new DatagramPacket( buf, buf.length );

  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    if ( !Thread.currentThread().getName().equals( "FritzBoxDiscoveryListener daemon" ) ) {
      throw new RuntimeException( "No right to call this method" );
    }
    inService = true;
    while ( inService ) {
      try {
        listenBroadCast();
      } catch ( SocketTimeoutException ex ) {
        // ignoring
      } catch ( IOException ioEx ) {
          //Log.v("Error", "Problem", ex );
      } catch( Exception ex ) {
        //Log.v("Error", "Fatal Error during UPNP FritzBoxDiscoveryListener messages listening thread, thread will exit", ex );
        inService = false;
      }
    }
    
    try {
      skt.leaveGroup( InetAddress.getByName( FritzBoxDiscovery.SSDP_IP ) );
      skt.close();
    } catch ( Exception ex ) {
      // ignoring
    }
  }

  /**
	 * Listen for broad cast.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
  private void listenBroadCast() throws IOException {
    
    skt.receive( input );
    InetAddress from = input.getAddress();
    String received = new String( input.getData(), input.getOffset(), input.getLength() );
    DiscoverHttpResponse msg = null;
    try {
      msg = new DiscoverHttpResponse( received );
    } catch (IllegalArgumentException ex ) {
      return;
    }
    String header = msg.getHeader();
    if ( header != null && header.startsWith( "HTTP/1.1 200 OK" ) && msg.getHTTPHeaderField( "st" ) != null ) {
      // probably a search repsonse !
      String deviceDescrLoc = msg.getHTTPHeaderField( "location" );
      if( deviceDescrLoc == null || deviceDescrLoc.trim().length() == 0 ) {
        //TODO if ( log.isDebugEnabled() ) log.debug( "Skipping SSDP message, missing HTTP header 'location' field" );
        return;
      }
      URL loc = new URL( deviceDescrLoc );
      if ( MATCH_IP ) {
        InetAddress locHost = InetAddress.getByName( loc.getHost() );
        if ( !from.equals( locHost ) ) {
        	//TODO  log.warn( "Discovery message sender IP " + from +
            //       " does not match device description IP " + locHost + 
            //       " skipping device, set the net.sbbi.upnp.ddos.matchip system property" +
            //       " to false to avoid this check" );
         return;
        }
      }
    //TODO if ( log.isDebugEnabled() ) log.debug( "Processing " + deviceDescrLoc + " device description location" );
      String st = msg.getHTTPHeaderField( "st" );
      if( st == null || st.trim().length() == 0 ) {
    	//TODO if ( log.isDebugEnabled() ) log.debug( "Skipping SSDP message, missing HTTP header 'st' field" );
        return;
      }
      String usn = msg.getHTTPHeaderField( "usn" );
      if( usn == null || usn.trim().length() == 0 ) {
    	//TODO if ( log.isDebugEnabled() ) log.debug( "Skipping SSDP message, missing HTTP header 'usn' field" );
        return;
      }
      String maxAge = msg.getHTTPFieldElement( "Cache-Control", "max-age" );
      if( maxAge == null || maxAge.trim().length() == 0 ) {
    	//TODO if ( log.isDebugEnabled() ) log.debug( "Skipping SSDP message, missing HTTP header 'max-age' field" );
        return;
      }
      String server = msg.getHTTPHeaderField( "server" );
      if( server == null || server.trim().length() == 0 ) {
    	//TODO if ( log.isDebugEnabled() ) log.debug( "Skipping SSDP message, missing HTTP header 'server' field" );
        return;
      }

      String udn = usn;
      int index = udn.indexOf( "::" );
      if ( index != -1 ) udn = udn.substring( 0, index );
      synchronized( REGISTRATION_PROCESS ) {
        Set handlers = (Set)registeredHandlers.get( st );
        if ( handlers != null ) {
          for ( Iterator i = handlers.iterator(); i.hasNext(); ) {
            FritzBoxDiscoveryResultsHandler handler = (FritzBoxDiscoveryResultsHandler)i.next();
            handler.discoveredDevice( usn, udn, st, maxAge, loc, server );
          }
        }
      }
    } else {
    	//TODO if ( log.isDebugEnabled() ) log.debug( "Skipping uncompliant HTTP message " + received );
    }
  }
}
