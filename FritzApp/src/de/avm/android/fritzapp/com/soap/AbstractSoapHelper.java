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

package de.avm.android.fritzapp.com.soap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.util.Log;
import de.avm.android.fritzapp.GLOBAL;
import de.avm.android.fritzapp.com.ComSettingsChecker;
import de.avm.android.fritzapp.com.DataHub;
import de.usbi.android.util.error.exceptions.BaseException;
import de.usbi.android.util.net.LazySSLHttpClientFactory;

/* Lightweight helper class for SOAP communication.*/

public abstract class AbstractSoapHelper<RESULT> {

	public static final int HTTPS_PORT = 49443;
	private static final String USERNAME = "dslf-config";
	protected final Context context;

	/**
	 * Instantiates a new soap helper.
	 * 
	 * @param c
	 *            a valid context
	 */
	public AbstractSoapHelper(Context c) {
		this.context = c;
	}

	public abstract String getSoapMethod();

	public abstract RESULT getQualifiedResult();

	public String getNamespace() {
		return "urn:dslforum-org:service:X_AVM-DE_OnTel:1";
	}

	public String getControlURL() {
		return "/upnp/control/x_contact";
	}

	public String getSoapMethodParameter() {
		return "";
	}

	/**
	 * Fetches the soap body 
	 * 
	 * @return the soap body
	 */
	protected String getSoapBody() {
		DefaultHttpClient client = LazySSLHttpClientFactory
				.getClientWithDigestAuth(USERNAME, DataHub
						.getFritzboxPass(context), HTTPS_PORT);

		String ret = "";
		try {
			String soapEndpoint = "https://"
					+ DataHub.getFritzboxUrlWithoutProtocol(context) + ":"
					+ HTTPS_PORT + getControlURL();
			String requestBody = createRequestBody();
			String soapAction = getNamespace() + "#" + getSoapMethod();
			Log.v("SOAP-Endpoint", soapEndpoint);
			Log.v("SOAP-Action", soapAction);
			Log.v("RequestBody", requestBody);

			HttpPost post = new HttpPost(soapEndpoint);
			post.setEntity(new StringEntity(requestBody, "utf-8"));
			post.addHeader("User-Agent", "AVM FRITZ!App");
			post.addHeader("SOAPACTION", soapAction);
			post.addHeader("Content-Type", "text/xml; charset=\"utf-8\"");
			post.addHeader("Accept", "text/xml");
			post.addHeader("Connection", "close");

			HttpResponse resp = client.execute(post);
			InputStream input = resp.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					input));
			String str;
			while ((str = reader.readLine()) != null) {
				ret += str;
				Log.v("REPLY", str);
			}

		} catch (ClientProtocolException e) {
			// TODO
			throw new BaseException("Invalid Response from FRITZ!Box");
		} catch (IOException e) {
			handleConnectionProblem();
		}
		return ret;
	}

	/**
	 * Handle connection problem. General implementation
	 */
	protected void handleConnectionProblem()
	{
		GLOBAL.ShowDisconnectedHandler handler =
			GLOBAL.mShowDisconnectedHandler;
		if (handler != null)
			handler.postProblem(
					ComSettingsChecker.CONNECTION_PROBLEM.CONNECTION_CLOSED);
	}

	/**
	 * Create the soap request body.
	 * 
	 * @return the string
	 */
	private String createRequestBody() {
		String body = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
				+ "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
				+ "<s:Body>"
				+ "<u:"
				+ getSoapMethod()
				+ " xmlns:u=\""
				+ getNamespace()
				+ "\">"
				+ getSoapMethodParameter()
				+ "</u:"
				+ getSoapMethod() + ">" + "</s:Body></s:Envelope>";
		return body;
	}
}