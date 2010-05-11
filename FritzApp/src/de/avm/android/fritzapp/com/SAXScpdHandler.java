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
 * SAX-Handler base class for FRITZ!Box SCPD description
 */
public abstract class SAXScpdHandler extends DefaultHandler
{
	/**
	 * Checks interface level after paring
	 * @return
	 */
	public abstract int getTr064Level();

	public SAXScpdHandler()
	{
	}

	/**
	 * Array of action names to check for.
	 * Has to be filled in derived classes before parsing.
	 */
	protected String[] mActions = null;
	/**
	 * Array of availability flags for actions.
	 * Could be read after parsing.
	 */
	protected boolean[] mAvalability = null;
	
	private final String ACTION_PATH = "/scpd/actionlist/action/name";
	private String mCurrentPath = "";
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument()
		throws SAXException
	{
		mAvalability = new boolean[mActions.length];
		for(int ii = 0; ii < mAvalability.length; ii++) mAvalability[ii] = false;
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
			mCurrentPath = mCurrentPath.substring(0, mCurrentPath.length() - localName.length() - 1);
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
		if ((str == null) || (str.length() <= 0)) return;
		
		if (mCurrentPath.equalsIgnoreCase(ACTION_PATH))
		{
			for(int ii = 0; ii < mActions.length; ii++)
			{
				if (mActions[ii].equalsIgnoreCase(str))
				{
					mAvalability[ii] = true;
					break;
				}
			}
		}
	}
}
