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
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.avm.android.fritzapp.util.error.DataMisformatException;
import de.usbi.android.util.error.exceptions.BaseException;

/**
 * Wrapper for jason_boxinfo.xml
 */
public class Tr064Boxinfo
{
	private static final int PORT = 49000;
	private static final String FILEPATH_TR64 = "/tr64desc.xml";
	
	private int mTr064Level = ComSettingsChecker.TR064_MOSTRECENT;
	
	/**
	 * Creates instance from info retrieved from box
	 * @param host box' host address
	 * @return URI to info file on host
	 * @throws URISyntaxException host is invalid
	 * @throws BaseException
	 * @throws DataMisformatException
	 */
	public static Tr064Boxinfo createInstance(String host)
		throws  URISyntaxException, BaseException, DataMisformatException
	{
		return new Tr064Boxinfo(new URI(host).getHost());
	}

	public int getTr064Level()
	{
		return mTr064Level;
	}
	
	/**
	 * Creates instance from XML string
	 * @param host box' host address
	 * @throws URISyntaxException host or an uri in a decription files is invalid
	 * @throws BaseException
	 * @throws FactoryConfigurationError Error creating XML parser
	 * @throws DataMisformatException
	 */
	private Tr064Boxinfo(String host)
		throws URISyntaxException, BaseException, DataMisformatException
	{
		try
		{
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			XMLReader reader = parser.getXMLReader();
			SAXTr064DescHandler tr64Handler = new SAXTr064DescHandler();
			reader.setContentHandler(tr64Handler);
			URI uri = new URI("http", "", host, PORT, FILEPATH_TR64, "", "");
			reader.parse(new InputSource(uri.toString()));
			
			// OnTel1
			if (tr64Handler.getOnTelPath().length() > 0)
			{
				try
				{
					reader = parser.getXMLReader();
					SAXOnTelScpdHandler onTelHandler = new SAXOnTelScpdHandler(); 
					reader.setContentHandler(onTelHandler);
					uri = new URI("http", "", host, PORT, tr64Handler.getOnTelPath(), "", "");
					reader.parse(new InputSource(uri.toString()));
					mergeLevel(onTelHandler.getTr064Level());
				}
				catch(Exception exp)
				{
					exp.printStackTrace();
					mergeLevel(SAXOnTelScpdHandler.NOT_AVAILABLE_LEVEL);
				}
			}
			else mergeLevel(SAXOnTelScpdHandler.NOT_AVAILABLE_LEVEL);
			
			// WLANConfiguration1
			if (tr64Handler.getWlanConfPath().length() > 0)
			{
				try
				{
					reader = parser.getXMLReader();
					SAXWlanConfScpdHandler wlanConfHandler = new SAXWlanConfScpdHandler(); 
					reader.setContentHandler(wlanConfHandler);
					uri = new URI("http", "", host, PORT, tr64Handler.getWlanConfPath(), "", "");
					reader.parse(new InputSource(uri.toString()));
					mergeLevel(wlanConfHandler.getTr064Level());
				}
				catch(Exception exp)
				{
					exp.printStackTrace();
					mergeLevel(SAXWlanConfScpdHandler.NOT_AVAILABLE_LEVEL);
				}
			}
			else mergeLevel(SAXOnTelScpdHandler.NOT_AVAILABLE_LEVEL);
			
			// VoIP1
			if (tr64Handler.getVoIPPath().length() > 0)
			{
				try
				{
					reader = parser.getXMLReader();
					SAXVoIPConfScpdHandler voipConfHandler = new SAXVoIPConfScpdHandler(); 
					reader.setContentHandler(voipConfHandler);
					uri = new URI("http", "", host, PORT, tr64Handler.getVoIPPath(), "", "");
					reader.parse(new InputSource(uri.toString()));
					mergeLevel(voipConfHandler.getTr064Level());
				}
				catch(Exception exp)
				{
					exp.printStackTrace();
					mergeLevel(SAXVoIPConfScpdHandler.NOT_AVAILABLE_LEVEL);
				}
			}
			else mergeLevel(SAXVoIPConfScpdHandler.NOT_AVAILABLE_LEVEL);
		}
		catch (FactoryConfigurationError exp)
		{
			throw new BaseException("Unhandled configuration Exception", exp);
		}
		catch (ParserConfigurationException exp)
		{
			throw new BaseException("Unhandled configuration Exception", exp);
		}
		catch (SAXException exp)
		{
			throw new DataMisformatException("Invalid Interface Description Data", exp);
		}
		catch (IOException exp)
		{
			throw new DataMisformatException("Invalid Interface Description Data", exp);
		}
	}
	
	private int mergeLevel(int level)
	{
		if (mTr064Level > level) mTr064Level = level;
		return mTr064Level;
	}
}
