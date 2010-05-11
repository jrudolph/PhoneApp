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

package de.avm.android.fritzapp.com;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.com.discovery.FritzBoxDiscovery;
import de.avm.android.fritzapp.com.soap.GetPhonebookList;
import de.avm.android.fritzapp.util.error.DataMisformatException;
import de.usbi.android.util.error.ExceptionHandler;

/*
 * Testing the current status of connection
 */
public class ComSettingsChecker {

	private static final String TAG = "ComSettingsChecker";
	
	/**
	 *	no TR-064 interfaces available
	 */
	public static final int TR064_NONE = 0;
	/**
	 *	readonly call log,
	 *	readonly phonebook,
	 *	WLAN associated devices information  
	 */
	public static final int TR064_BASIC = 1;
	/**
	 * TR064_BASIC,
	 * configure VoIP devices settings 
	 */
	public static final int TR064_VOIPCONF = 2;
	/**
	 * TR064_VOIPCONF
	 */
	public static final int TR064_MOSTRECENT = TR064_VOIPCONF; 

	private static final int MIN_VERSION_MAJOR = 4;
	private static final int MIN_VERSION_MINOR = 80;
	private static final int TIMEOUT = 750;

	
	protected static String mLocationIP = "";
	protected static JasonBoxinfo mJasonBoxinfo = null;
	protected static int mTr064Level = TR064_NONE;
	
	/**
	 * Gets IP address of box found
	 * @return IP or domain name address
	 */
	public static String getLocationIP()
	{
		return mLocationIP;
	}
	
	/**
	 * Get jason info of box found 
	 * @return JasonBoxinfo instance
	 */
	public static JasonBoxinfo getJasonBoxinfo()
	{
		return mJasonBoxinfo;
	}

	/**
	 * @return version of TR-064 support (see TR064_*)
	 */
	public static int getTr064Level()
	{
		return mTr064Level;
	}
	
	public static enum CONNECTION_PROBLEM {
		WLAN_OFF, WLAN_DISCONNECT, FRITZBOX_MISSING, FRITZBOX_REJECTED, FRITZBOX_VERSION, NO_PROBLEM, CONNECTION_CLOSED
	}

	/**
	 * Check connection on Startup.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return a ConnectionProblem or NO_PROBLEM if everything fine
	 */
	public static CONNECTION_PROBLEM checkStartUpConnection(Context c) {
		if (!isWLANAngeschaltet(c)) {
			clearBoxInfo();
			return CONNECTION_PROBLEM.WLAN_OFF;
		} else if (!isWifiAvailable(c)) {
			clearBoxInfo();
			return CONNECTION_PROBLEM.WLAN_DISCONNECT;
		} else if (!isFritzBoxReachable(c)) {
			clearBoxInfo();
			return CONNECTION_PROBLEM.FRITZBOX_MISSING;
		} else if (!isVersionCompatibel(c)) {
			clearBoxInfo();
			return CONNECTION_PROBLEM.FRITZBOX_VERSION;
		} else if ((mTr064Level != TR064_NONE) && !isConnectionPossible(c)) {
			return CONNECTION_PROBLEM.FRITZBOX_REJECTED;
		}
		return CONNECTION_PROBLEM.NO_PROBLEM;
	}

	/**
	 * Checks if WLAN is on.
	 * 
	 * @param c
	 *            a valid context            
	 * 
	 * @return if WLAN is on
	 */
	protected static boolean isWLANAngeschaltet(Context c) {
		if (GLOBAL.DEBUG_NO_COM_CHECK)
			return true;
		String connectivity_context = Context.WIFI_SERVICE;
		final WifiManager wifi = (WifiManager) c
				.getSystemService(connectivity_context);
		return wifi.isWifiEnabled();
	}

	/**
	 * Checks if wifi is available.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return true, if wifi available
	 */
	protected static boolean isWifiAvailable(Context c) {
		if (GLOBAL.DEBUG_NO_COM_CHECK)
			return true;
		String connectivity_context = Context.WIFI_SERVICE;
		final WifiManager wifi = (WifiManager) c
				.getSystemService(connectivity_context);
		return (wifi.getConnectionInfo() != null);
	}

	/**
	 * Checks if is FRITZ!Box can be reached via the network.
	 * TODO try to Discover via SSDP first?
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return true, if FRITZ!Box is reachable
	 */
	protected static boolean isFritzBoxReachable(Context c)
	{
 		// Adresse aus den Einstellungen versuchen
		if (checkFritzboxAdress(DataHub.getFritzboxUrl(c)))
			return true;

		// Only discover if not set manually in preferences
		if (DataHub.isFritzboxUrlDefaulted(c))
		{
			try
			{
				// upnp discovery versuchen
				HashMap<String, Bundle> discover = FritzBoxDiscovery.discover(c);
				if (!discover.isEmpty()) {
					Bundle infoBundle = discover.get(discover.keySet().toArray()[0]);
					String discoveredIP = infoBundle
							.getString(FritzBoxDiscovery.BUNDEL_LOCATION);
					URL x = new URL(discoveredIP);
					discoveredIP = x.getHost();
					if (discoveredIP.length() > 0) {
						//wir übernehmen die neue adresse
						DataHub.setFritzboxUrl("http://" + discoveredIP, c);
						return true;
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Check HTTP-result for fritzbox' url
	 * 
	 * @param adress
	 *            the url to connect to
	 * 
	 * @return true, if a http request to the box respones a code 200  
	 */
	protected static boolean checkFritzboxAdress(String adress)
	{
		try
		{
			URL url = new URL("http", new URL(adress).getHost(),
					"/cgi-bin/system_status");
			HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
			urlc.setRequestProperty("Connection", "close");
			urlc.setConnectTimeout(TIMEOUT);
			urlc.connect();
			if (urlc.getResponseCode() == 200) {
				return true;
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Checks if the version of the FRITZ!Box is compatibel.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return true, if version is compatibel
	 */
	protected static boolean isVersionCompatibel(Context c)
	{
		String location = DataHub.getFritzboxUrl(c);
		JasonBoxinfo boxInfo = null;
		class StringWrapper
		{
			public String value = "";
		}
		final StringWrapper locationIP = new StringWrapper();

		if (location.length() > 0)
		{
			DefaultHttpClient client = null;
			try
			{
				// Info über Box holen
				client = new DefaultHttpClient();
				client.addRequestInterceptor(new HttpRequestInterceptor()
				{
					public void process(HttpRequest request, HttpContext context)
							throws HttpException, IOException
					{
						// get remote IP address
				        ManagedClientConnection conn = (ManagedClientConnection)context
		        				.getAttribute(ExecutionContext.HTTP_CONNECTION);
				        if (conn != null)
				        	locationIP.value = conn.getRemoteAddress().getHostAddress();
					}
				});
				HttpResponse httpResponse = client
						.execute(new HttpGet(JasonBoxinfo.createUri(location)));
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
					boxInfo = new JasonBoxinfo(EntityUtils
							.toString(httpResponse.getEntity()));
			}
			catch(Exception exp)
			{
				exp.printStackTrace();
			}
			finally
			{
				if (client != null) client.getConnectionManager().shutdown();
			}
		}

		if (boxInfo != null)
		{
			// check for minimal version
			try
			{
				ExceptionHandler.addDebugInfo("Firmware", boxInfo.getVersion());
				Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+).*$")
						.matcher(boxInfo.getVersion());
				if (m.find())
				{
					int majorVersion = Integer.parseInt(m.group(2));
					if ((majorVersion < MIN_VERSION_MAJOR) ||
						((majorVersion == MIN_VERSION_MAJOR) &&
						 (Integer.parseInt(m.group(3)) < MIN_VERSION_MINOR)))
					{
						Log.d(TAG, "FRITZ!Box firmware too old: " + boxInfo.getVersion());
						boxInfo = null;
					}
				}
				else
				{
					Log.w(TAG, "Invalid FRITZ!Box firmware version: " + boxInfo.getVersion());
					boxInfo = null;
				}
			}
			catch(Exception exp)
			{
				Log.w(TAG, "Invalid FRITZ!Box firmware version: " + boxInfo.getVersion());
				exp.printStackTrace();
				boxInfo = null; // invalid content?
			}
		}		
		
		if (boxInfo != null)
		{
			// check for TR-064 features needed
			int tr064Level = TR064_NONE;
			try
			{
				tr064Level = Tr064Boxinfo.createInstance(location)
						.getTr064Level();
			}
			catch(Exception exp)
			{
				exp.printStackTrace();
			}

			mLocationIP = locationIP.value;
			mJasonBoxinfo = boxInfo;
			mTr064Level = tr064Level;
			return true;
		}
		return false;
	}

	/**
	 * Checks if aqualified connection to the SOAP-Endpoint is possible.
	 * Only needed if TR-064 will be used.
	 * 
	 * @param c
	 *            a valid context
	 * 
	 * @return true, if connection is possible
	 */
	protected static boolean isConnectionPossible(Context c)
	{
		try
		{
			Log.d(TAG, "isConnectionPossible()");
			new GetPhonebookList(c).getQualifiedResult();
		}
		catch (DataMisformatException exp)
		{
			// valid SOAP response with unexpected content
			// guessing interfaces not fully implemented
			// could be invalid password too
			mTr064Level = TR064_NONE;
			Log.d(TAG, "isConnectionPossible: DataMisformatException - disable using TR-064");
			exp.printStackTrace();
		}
		catch (Exception exp)
		{
			// invalid or no SOAP response 
			Log.d(TAG, "isConnectionPossible: failed with exception");
			exp.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 *	Clears all box info data
	 */
	protected static void clearBoxInfo()
	{
		mLocationIP = "";
		mJasonBoxinfo = null;
		mTr064Level = TR064_NONE;
	}
}