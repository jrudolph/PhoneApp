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
import java.io.StringBufferInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Wrapper for jason_boxinfo.xml
 */
@SuppressWarnings("deprecation")  // StringBufferInputStream wird benötigt
public class JasonBoxinfo
{
	private static final String FILEPATH = "/jason_boxinfo.xml";
	private static final String NAMESPACE = "http://jason.avm.de/updatecheck/";
	
	private Node mBoxInfoNode = null;
	
	/**
	 * @param host box' host address
	 * @return URI to info file on host
	 * @throws URISyntaxException host is invalid
	 */
	public static URI createUri(String host)
		throws  URISyntaxException
	{
		return new URI("http", new URI(host).getHost(), FILEPATH, "");
	}
	
	/**
	 * @return box' product name
	 */
	public String getName()
	{
		return getTextNode("name");
	}
	
	/**
	 * @return box' version
	 */
	public String getVersion()
	{
		String version = getTextNode("version");
		String revision = getTextNode("revision");
		
		int pos = version.indexOf('-');
		if ((pos < 0) || (revision.length() == 0) || version.endsWith(revision))
			return version;
		return version.substring(0, pos + 1) + revision;
	}
	
	/**
	 * @return box' lab name if any
	 */
	public String getLab()
	{
		return getTextNode("lab");
	}
	
	/**
	 * Creates instance from XML string
	 * @param xml XML string
	 * @throws InvalidParameterException Error parsing XML string
	 * @throws FactoryConfigurationError Error creating XML parser
	 * @throws ParserConfigurationException Error creating XML parser
	 */
	public JasonBoxinfo(String xml)
		throws InvalidParameterException, FactoryConfigurationError, ParserConfigurationException
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new StringBufferInputStream(xml));

			NodeList nodes = document.getElementsByTagNameNS(NAMESPACE, "BoxInfo");
			if (nodes.getLength() == 1)
				mBoxInfoNode = nodes.item(0);
			else
				throw new InvalidParameterException();
		}
		catch (SAXException e)
		{
			throw new InvalidParameterException();
		}
		catch (IOException e)
		{
			throw new InvalidParameterException();
		}
	}

	/**
	 * Retrieves first text part of elements content
	 * @param tag name of element node
	 * @return text part
	 */
	private String getTextNode(String tag)
	{
		NodeList nodes = mBoxInfoNode.getChildNodes();
		for (int ii = 0; ii < nodes.getLength(); ii++)
		{
			Node node = nodes.item(ii);
			if ((node.getNodeType() == Node.ELEMENT_NODE) &&
				NAMESPACE.equals(node.getNamespaceURI()) &&
				tag.equalsIgnoreCase(node.getLocalName()))
			{
				// first text value
				NodeList childNodes = node.getChildNodes();
				for (int jj = 0; jj < childNodes.getLength(); jj++)
				{
					Node childNode = childNodes.item(jj);
					if (childNode.getNodeType() == Node.TEXT_NODE)
						return childNode.getNodeValue();
				}
				break;
			}
		}
		
		return "";
	}
}
