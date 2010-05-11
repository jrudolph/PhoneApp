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

package de.usbi.android.util.net;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/*
 * This is a Factory for very Lazy SSL Connections, mainly needed for usage of
 * self singed SSL certificates.
 * It accepts all hosts and all kind of certificates, so all authentification
 * parts of SSL are not in place anymore. The encryption part is still fully
 * functional!
 * It supports also a Secure Digest Authentification (e.g. Challenge-Respone).
 * 
 * Thank you Google for not using the standard Apache HTTP Commons and don't commenting it ;-D
 * 
 * This pages helped us, while figuring out the problems in this area:
 * + http://stackoverflow.com/questions/995514/https-connection-android
 * + http://blog.ippon.fr/2008/10/20/certificats-auto-signe-et-communication-ssl-en-java
 * (http://translate.google.de/translate?js=y&prev=_t&hl=de&ie=UTF-8&u=http%3A%2F%2Fblog.ippon.fr%2F2008%2F10%2F20%2Fcertificats-auto-signe-et-communication-ssl-en-java&sl=fr&tl=de)
 * + http://www.java-forum.org/netzwerkprogrammierung/89641-socket-ssl-mode-bringen.html#post566725 
 */
public class LazySSLHttpClientFactory {

	private static final int DEFAULT_HTTPS_PORT = 443;

	/**
	 * Erstellt einen LazySSLClient, mit integrierter Digest Authentifizierung.
	 * Als SSL Port wird der übergebene Port verwendet. Dies ist nur nötig, wenn
	 * der SLL Port nicht DEFAULT_HTTPS_PORT ist.
	 */
	public static DefaultHttpClient getClientWithDigestAuth(String username,
			String password, int nonDefaultHttpsPort) {

		// Client which is configured and later returned.
		DefaultHttpClient client = getClient(nonDefaultHttpsPort);

		// Choose Digest Auth
		client.getParams().setParameter("http.auth.scheme-pref",
				AuthPolicy.DIGEST);

		// CredentialsProvider
		UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
				username, password);
		client.setCredentialsProvider(new FixedCredentialsProvider(cred));

		return client;
	}

	/**
	 * Erstellt einen LazySSLClient, mit integrierter Digest Authentifizierung.
	 * Als SSL Port wird der übergebene Port verwendet.
	 */
	public static DefaultHttpClient getClientWithDigestAuth(String username,
			String password) {
		return getClientWithDigestAuth(username, password, DEFAULT_HTTPS_PORT);
	}

	/**
	 * Erstellt einen LazySSLClient, für den übergebenen HTTPS Port.
	 */
	public static DefaultHttpClient getClient(int nonDefaultHttpsPort) {

		// SchemaRegistry
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		try {
			LazySSLSocketFactory factory = new LazySSLSocketFactory();
			factory.setHostnameVerifier(LazyHostnameVeryfier);

			schemeRegistry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", factory,
					nonDefaultHttpsPort));

		} catch (Exception e) {
			// TODO ?!
			e.printStackTrace();
		}

		// HttpParams
		HttpParams params = new BasicHttpParams();

		// ConnectionManager
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(
				params, schemeRegistry);

		// Client which is configured and later returned.
		DefaultHttpClient client = new DefaultHttpClient(cm, params);

		return client;
	}

	/**
	 * Erstellt einen LazySSLClient für den Port DEFAULT_HTTPS_PORT
	 */
	public static DefaultHttpClient getClient() {
		return getClient(DEFAULT_HTTPS_PORT);
	}

	/**
	 * Hostname Verifier which accepts all hostnames. This is needed for
	 * self-signed certificates.<br/>
	 * Verify-Methods will never throw a SSLEception, e.g. verifying everything.
	 */
	private static final X509HostnameVerifier LazyHostnameVeryfier = new X509HostnameVerifier() {

		public void verify(String host, String[] cns, String[] subjectAlts)
				throws SSLException {
			// nothing to do here
		}

		public void verify(String host, X509Certificate cert)
				throws SSLException {
			// nothing to do here
		}

		public void verify(String host, SSLSocket ssl) throws IOException {
			// nothing to do here
		}

		public boolean verify(String host, SSLSession session) {
			return true;
		}
	};

	/**
	 * TrustManager which trusts all Servers and Clients. This is needed for
	 * self-signed certificates.<br/>
	 * Check-Methods will never throw a CertificateException.
	 */
	private static final X509TrustManager LazyTrustManager = new X509TrustManager() {

		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[] {};
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// nothing to do here
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// nothing to do here
		}
	};

	/**
	 * Credentials Provides which returns, despite of the AuthScope, always the
	 * same, fixed, given credentials.
	 */
	private static class FixedCredentialsProvider implements
			CredentialsProvider {

		private final UsernamePasswordCredentials credentials;

		public FixedCredentialsProvider(
				UsernamePasswordCredentials fixedCredentials) {
			this.credentials = fixedCredentials;
		}

		public void setCredentials(AuthScope authscope, Credentials credentials) {
			// nothing to do here
		}

		public Credentials getCredentials(AuthScope authscope) {
			return this.credentials;
		}

		public void clear() {
			// nothing to do here
		}
	};

	private static class LazySSLSocketFactory extends
			org.apache.http.conn.ssl.SSLSocketFactory {

		private javax.net.ssl.SSLSocketFactory FACTORY = HttpsURLConnection
				.getDefaultSSLSocketFactory();

		public LazySSLSocketFactory() throws KeyManagementException,
				NoSuchAlgorithmException, UnrecoverableKeyException,
				KeyStoreException {

			// Call super-Constructor without KeyStore, because we've no Keys.
			super(null);

			SSLContext context = SSLContext.getInstance("TLS");
			TrustManager[] trustManagers = new TrustManager[] { LazyTrustManager };
			context.init(null, trustManagers, new SecureRandom());
			FACTORY = context.getSocketFactory();
		}

		public Socket createSocket(Socket socket, String host, int port,
				boolean autoClose) throws IOException, UnknownHostException {
			// Important: Use FACTORY here, and not super!
			return FACTORY.createSocket(socket, host, port, autoClose);
		}

		public Socket createSocket() throws IOException {
			// Important: Use FACTORY here, and not super!
			return FACTORY.createSocket();
		}
	}
}