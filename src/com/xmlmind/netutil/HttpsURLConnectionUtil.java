/*
 *    Qizx/open 4.1
 *
 * This code is part of the Qizx application components
 * Copyright (C) 2004-2010 Axyana Software -- All rights reserved.
 *
 * The contents of this file are subject to the Mozilla Public License 
 *  Version 1.1 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 * The Initial Developer of the Original Code is Xavier Franc - Axyana Software.
 *
 */
/*
 * Copyright (c) 2008 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.netutil;

import java.util.Date;
import java.text.DateFormat;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

/**
 * A collection of utilities (static methods) related to {@link
 * HttpsURLConnection}.
 */
public final class HttpsURLConnectionUtil {
    private HttpsURLConnectionUtil() {}

    // -----------------------------------------------------------------------

    private static final SSLSocketFactory STRICT_SSL_SOCKET_FACTORY =
        (SSLSocketFactory) SSLSocketFactory.getDefault();

    private static final X509TrustManager createTrustManager(KeyStore ks) 
        throws KeyStoreException, NoSuchAlgorithmException {
        TrustManagerFactory factory =
            TrustManagerFactory.getInstance("SunX509");
        factory.init(ks);

        TrustManager[] managers = factory.getTrustManagers();
        for (int i = 0; i < managers.length; ++i) {
            if (managers[i] instanceof X509TrustManager) {
                return (X509TrustManager) managers[i];
            }
        }
        return null;
    }

    private static final String toString(X509Certificate[] chain) {
        StringBuffer buffer = new StringBuffer();
        toString(chain, buffer);
        return buffer.toString();
    }

    private static final void toString(X509Certificate[] chain,
                                       StringBuffer buffer) {
        for (int i = 0; i < chain.length; ++i) {
            if (i > 0)
                buffer.append("---\n");
            toString(chain[i], buffer);
        }
    }

    private static final String toString(X509Certificate cert) {
        StringBuffer buffer = new StringBuffer();
        toString(cert, buffer);
        return buffer.toString();
    }

    private static final void toString(X509Certificate cert,
                                       StringBuffer buffer)
    {
        Principal subject = cert.getSubjectDN();
        Principal issuer = cert.getIssuerDN();
        Date issued = cert.getNotBefore();
        Date expires = cert.getNotAfter();

        buffer.append("Issued to: ");
        buffer.append(subject.toString());
        buffer.append('\n');

        buffer.append("Issued by: ");
        buffer.append(issuer.toString());
        buffer.append('\n');

        buffer.append("Issued on: ");
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        buffer.append(dateFormat.format(issued));
        buffer.append('\n');

        buffer.append("Expires on: ");
        buffer.append(dateFormat.format(expires));
        buffer.append('\n');
    }

    private static final class TrustManagerImpl implements X509TrustManager {
        // See http://java.sun.com/javase/6/docs/
        //             technotes/guides/security/jsse/JSSERefGuide.html

        private X509TrustManager delegate;

        public TrustManagerImpl() 
            throws KeyStoreException, NoSuchAlgorithmException {
            // null ==> Use <java-home>/lib/security/cacerts
            delegate = createTrustManager(null);
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType)
            throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType)
            throws CertificateException {
            /*
            try {
                delegate.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                System.err.println("Accepting invalid server certificate\n" + 
                                   HttpsURLConnectionUtil.toString(chain));
            }
            */
        }

        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }

    private static SSLSocketFactory LAX_SSL_SOCKET_FACTORY = 
        STRICT_SSL_SOCKET_FACTORY;
    static {
        try {
            TrustManager[] managers = new TrustManager[] {
                new TrustManagerImpl()
            };
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, managers, null);

            LAX_SSL_SOCKET_FACTORY = context.getSocketFactory();
        } catch (Exception shouldNotHappen) {
            shouldNotHappen.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------

    private static final HostnameVerifier STRICT_HOSTNAME_VERIFIER = 
        new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return false;
            }
        };

    private static final HostnameVerifier LAX_HOSTNAME_VERIFIER = 
        new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

    // -----------------------------------------------------------------------

    /**
     * Specifies whether validation of the certificates of HTTPS servers is
     * turned on.
     * <p>When the validation is turned off, 
     * there is no validation <em>at all</em>.
     * For example, the following cases are <em>not</em> rejected:
     * <ul>
     * <li>Self-signed certificate.
     * <li>Expired certificate.
     * <li>Certificate where the CN of the subject does not match the hostname
     * of the server.
     * </ul>
     * 
     * @see #getValidateServer
     */
    public static void setValidateServer(boolean validate) {
        if (validate) {
            HttpsURLConnection.setDefaultSSLSocketFactory(
                STRICT_SSL_SOCKET_FACTORY);
            HttpsURLConnection.setDefaultHostnameVerifier(
                STRICT_HOSTNAME_VERIFIER);
        } else {
            HttpsURLConnection.setDefaultSSLSocketFactory(
                LAX_SSL_SOCKET_FACTORY);
            HttpsURLConnection.setDefaultHostnameVerifier(
                LAX_HOSTNAME_VERIFIER);
        }
    }

    /**
     * Returns <code>true</code> if the validation of the certificates of
     * HTTPS servers is turned on; <code>false</code> otherwise.
     * 
     * @see #setValidateServer
     */
    public static boolean getValidateServer() {
        return (HttpsURLConnection.getDefaultSSLSocketFactory() == 
                STRICT_SSL_SOCKET_FACTORY);
    }
}
