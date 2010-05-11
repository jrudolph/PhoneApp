/*
 * ============================================================================
 *                 The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 2010 AVM GmbH <info@avm.de>
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

/* BASED on UPNP by <sbbi.net>. Modified by usefulbits GmbH <usbi.de> */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import de.avm.android.fritzapp.GLOBAL;

/**
 * Class to discover an UPNP device on the network.</br> A multicast socket will
 * be created to discover devices, the binding port for this socket is set to
 * 1901, if this is causing a problem you can use the
 * net.sbbi.upnp.Discovery.bindPort system property to specify another port. The
 * discovery methods only accept matching device description and broadcast
 * message response IP to avoid a security flaw with the protocol. If you are
 * not happy with such behaviour you can set the net.sbbi.upnp.ddos.matchip
 * system property to false to avoid this check.
 * 
 * @author <a href="mailto:superbonbon@sbbi.net">SuperBonBon</a>
 * @version 1.0
 */
public class FritzBoxDiscovery {

	public static final String BUNDEL_FIRMWARE = "firmware";
	public static final String BUNDEL_LOCATION = "location";

	public final static String ROOT_DEVICES = "upnp:rootdevice";
	public final static String ALL_DEVICES = "ssdp:all";

	public static final int DEFAULT_MX = 3;
	public static final int DEFAULT_TTL = 4;
	public static final int DEFAULT_TIMEOUT = 1000;
	public static final String DEFAULT_SEARCH = ALL_DEVICES;
	public static final int DEFAULT_SSDP_SEARCH_PORT = 1901;

	public final static String SSDP_IP = "239.255.255.250";
	public final static int SSDP_PORT = 1900;

	/**
	 * Devices discovering on all network interfaces with a given root device to
	 * search.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return an array of UPNP Root device that matches the search or null if
	 *         nothing found with the default timeout. Null does NOT means that
	 *         no UPNP device is available on the network. It only means that
	 *         for this given timeout no devices responded or that effectively
	 *         no devices are available at all.
	 * 
	 * @throws IOException
	 *             if some IOException occurs during discovering
	 */
	public static HashMap<String, Bundle> discover(Context c)
			throws IOException {
		String searchTarget = "urn:dslforum-org:device:InternetGatewayDevice:1";
		return discoverDevices(DEFAULT_TIMEOUT, DEFAULT_TTL, DEFAULT_MX,
				searchTarget, c);
	}

	/**
	 * Discover devices.
	 * 
	 * @param timeOut
	 *            the time out
	 * @param ttl
	 *            the time to live
	 * @param mx
	 *            the mx field
	 * @param searchTarget
	 *            the search target
	 * @param c
	 *            a valid context
	 * 
	 * @return the hash map< string, bundle>
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static HashMap<String, Bundle> discoverDevices(int timeOut,
			int ttl, int mx, String searchTarget, Context c) throws IOException {

		NetworkInterface ni = null;
		final HashMap<String, Bundle> devices = new HashMap<String, Bundle>();

		FritzBoxDiscoveryResultsHandler handler = new FritzBoxDiscoveryResultsHandler() {
			public void discoveredDevice(String usn, String udn, String nt,
					String maxAge, URL location, String server) {
				synchronized (devices) {
					if (!devices.containsKey(usn)) {
						Bundle b = new Bundle();
						b.putString(BUNDEL_LOCATION, location.toString());
						Log.v("server", server);
						b.putString(BUNDEL_FIRMWARE, server);
						devices.put(usn, b);
					}
				}
			}
		};

		FritzBoxDiscoveryListener.getInstance().registerResultsHandler(handler,
				searchTarget);
		if (GLOBAL.DEBUG_NO_COM_CHECK) {
			for (Enumeration<NetworkInterface> e = NetworkInterface
					.getNetworkInterfaces(); e.hasMoreElements();) {
				NetworkInterface intf = e.nextElement();
				for (Enumeration<InetAddress> adrs = intf.getInetAddresses(); adrs
						.hasMoreElements();) {
					InetAddress adr = adrs.nextElement();
					if (adr instanceof Inet4Address && !adr.isLoopbackAddress()) {
						sendSearchMessage(adr, ttl, mx, searchTarget);
					}
				}
			}
		} else {

			WifiManager wifiManager = (WifiManager) c
					.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiinfo = wifiManager.getConnectionInfo();
			int intaddr = wifiinfo.getIpAddress();
			if (intaddr != 0) {
				byte[] byteaddr = null;
				byteaddr = new byte[] { (byte) (intaddr & 0xff),
						(byte) (intaddr >> 8 & 0xff),
						(byte) (intaddr >> 16 & 0xff),
						(byte) (intaddr >> 24 & 0xff) };
				ni = NetworkInterface.getByInetAddress(InetAddress
						.getByAddress(byteaddr));

				for (Enumeration<InetAddress> adrs = ni.getInetAddresses(); adrs
						.hasMoreElements();) {
					InetAddress adr = adrs.nextElement();
					if (adr instanceof Inet4Address && !adr.isLoopbackAddress()) {
						sendSearchMessage(adr, ttl, mx, searchTarget);
					}
				}
			}
		}

		try {
			Thread.sleep(timeOut);
		} catch (InterruptedException ex) {
			// don't care
		}
		FritzBoxDiscoveryListener.getInstance().unRegisterResultsHandler(
				handler, searchTarget);
		return devices;
	}

	/**
	 * Sends an SSDP search message on the network.
	 * 
	 * @param src
	 *            the sender ip
	 * @param ttl
	 *            the time to live
	 * @param mx
	 *            the mx field
	 * @param searchTarget
	 *            the search target
	 * 
	 * @throws IOException
	 *             if some IO errors occurs during search
	 */
	public static void sendSearchMessage(InetAddress src, int ttl, int mx,
			String searchTarget) throws IOException {

		int bindPort = DEFAULT_SSDP_SEARCH_PORT;
		String port = System.getProperty("net.sbbi.upnp.Discovery.bindPort");
		if (port != null) {
			bindPort = Integer.parseInt(port);
		}
		InetSocketAddress adr = new InetSocketAddress(InetAddress
				.getByName(FritzBoxDiscovery.SSDP_IP),
				FritzBoxDiscovery.SSDP_PORT);

		java.net.MulticastSocket skt = new java.net.MulticastSocket(null);
		skt.bind(new InetSocketAddress(src, bindPort));
		skt.setTimeToLive(ttl);
		StringBuffer packet = new StringBuffer();
		packet.append("M-SEARCH * HTTP/1.1\r\n");
		packet.append("HOST: 239.255.255.250:1900\r\n");
		packet.append("MAN: \"ssdp:discover\"\r\n");
		packet.append("MX: ").append(mx).append("\r\n");
		packet.append("ST: ").append(searchTarget).append("\r\n")
				.append("\r\n");
		String toSend = packet.toString();
		byte[] pk = toSend.getBytes();
		skt.send(new DatagramPacket(pk, pk.length, adr));
		skt.disconnect();
		skt.close();
	}

}