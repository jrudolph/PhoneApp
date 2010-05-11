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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * SAX-Handler for FRITZ!Box TR-064 description
 */
public class SAXTr064DescHandler extends DefaultHandler
{
	private static final String DEVICE_TYPE_IGD = "urn:dslforum-org:device:InternetGatewayDevice:1";
	private static final String DEVICE_TYPE_LAN = "urn:dslforum-org:device:LANDevice:1";
	private static final String SERVICE_TYPE_ONTEL = "urn:dslforum-org:service:X_AVM-DE_OnTel:1";
	private static final String SERVICE_TYPE_WLANCONF = "urn:dslforum-org:service:WLANConfiguration:1";
	private static final String SERVICE_TYPE_VOIP = "urn:dslforum-org:service:X_VoIP:1";
	
	private static final String PATH_IGD = "/root/device";
	private static final String PATH_ONTEL = "/root/device/servicelist/service";
	private static final String PATH_VOIP = "/root/device/servicelist/service";
	private static final String PATH_LAN = "/root/device/devicelist/device";
	private static final String PATH_WLANCONF = "/root/device/devicelist/device/servicelist/service";

	private String mCurrentPath = "";
	private String mCurrentScpdUrl = "";

	private int mInIgd = 0;
	private int mInLan = 0;
	private int mInWlan = 0;
	private int mInOnTel = 0;
	private int mInVoIP = 0;
	private int mInScpdUrl = 0;

	private String mOnTelPath = "";
	private String mWlanConfPath = "";
	private String mVoIPPath = "";
	
	public String getOnTelPath()
	{
		return mOnTelPath;
	}
	
	public String getWlanConfPath()
	{
		return mWlanConfPath;
	}
	
	public String getVoIPPath()
	{
		return mVoIPPath;
	}
	
	/**
	 * Instantiates a new sAX FRITZ!Box TR-064 description handler.
	 */
	public SAXTr064DescHandler()
	{
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument()
		throws SAXException
	{
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	@Override
	public void endDocument()
		throws SAXException
	{
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException
	{
		mCurrentPath += "/" + localName;
		if (localName.equalsIgnoreCase("scpdurl"))
		{
			mInScpdUrl = mCurrentPath.length();
			mCurrentScpdUrl = "";
		}
	}

	/**
	 * on closing tags like: </tag>.
	 * 
	 * @param namespaceURI
	 *            the namespace uri
	 * @param localName
	 *            the local name
	 * @param qName
	 *            the q name
	 * 
	 * @throws SAXException
	 *             the SAX exception
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException
	{
		if (mCurrentPath.endsWith(localName))
		{
			if (localName.equalsIgnoreCase("service"))
			{
				if (mInOnTel == mCurrentPath.length())
					mOnTelPath = mCurrentScpdUrl;
				else if (mInWlan == mCurrentPath.length())
					mWlanConfPath = mCurrentScpdUrl;
				else if (mInVoIP == mCurrentPath.length())
					mVoIPPath = mCurrentScpdUrl;
			}
			mCurrentPath = mCurrentPath.substring(0, mCurrentPath.length() - localName.length() - 1);
		}
		int len = mCurrentPath.length();
		if (len < mInIgd) mInIgd = 0;
		if (len < mInLan) mInLan = 0;
		if (len < mInWlan) mInWlan = 0;
		if (len < mInOnTel) mInOnTel = 0;
		if (len < mInVoIP) mInVoIP = 0;
		if (len < mInScpdUrl) mInScpdUrl = 0;
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>.
	 * 
	 * @param ch
	 *            the ch
	 * @param start
	 *            the start
	 * @param length
	 *            the length
	 */
	@Override
	public void characters(char ch[], int start, int length)
	{
		String str = new String(ch, start, length);
		if ((str == null) || (str.trim().length() <= 0)) return;
		
		if (mInScpdUrl > 0)
		{
			mCurrentScpdUrl = str;
		}
		else if ((mInIgd == 0) &&
				 mCurrentPath.equalsIgnoreCase(PATH_IGD + "/" + "devicetype") &&
				 str.equals(DEVICE_TYPE_IGD))
		{
			mInIgd = PATH_IGD.length();
		}
		else if ((mInIgd > 0) && (mInLan == 0) &&
				 mCurrentPath.equalsIgnoreCase(PATH_LAN + "/" + "devicetype") &&
				 str.equals(DEVICE_TYPE_LAN))
		{
			mInLan = PATH_LAN.length();
		}
		else if ((mInIgd > 0) && (mInOnTel == 0) &&
				 mCurrentPath.equalsIgnoreCase(PATH_ONTEL + "/" + "servicetype") &&
				 str.equals(SERVICE_TYPE_ONTEL))
		{
			mInOnTel = PATH_ONTEL.length();
		}
		else if ((mInIgd > 0) && (mInVoIP == 0) &&
				 mCurrentPath.equalsIgnoreCase(PATH_VOIP + "/" + "servicetype") &&
				 str.equals(SERVICE_TYPE_VOIP))
		{
			mInVoIP = PATH_VOIP.length();
		}
		else if ((mInIgd > 0) && (mInLan > 0) && (mInWlan == 0) &&
				 mCurrentPath.equalsIgnoreCase(PATH_WLANCONF + "/" + "servicetype") &&
				 str.equals(SERVICE_TYPE_WLANCONF))
		{
			mInWlan = PATH_WLANCONF.length();
		}
	}
}
