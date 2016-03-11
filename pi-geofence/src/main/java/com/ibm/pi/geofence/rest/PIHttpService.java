/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.pi.geofence.rest;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import org.apache.log4j.Logger;

import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Implementation of the service which communicates with the AE server to authenticate and perform RESTful requests.
 */
public class PIHttpService {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(PIHttpService.class);
    private CookieManager manager = new CookieManager();
    /**
     * Used to queue the http requests.
     */
    ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * Detects whether network and/or server are online and sends notifications accordingly.
     * This is used to determine when offline mode should be used.
     */
    NetworkConnectivityHandler connectivityHandler;
    /**
     * The base server URL, for instance <code>http[s]://hostname[:port]</code>.
     */
    String serverURL;
    /**
     * The tenantCode id.
     */
    String tenantCode;
    /**
     * The app id.
     */
    String orgCode;
    /**
     * User name.
     */
    String username;
    /**
     * User password.
     */
    String password;
    /**
     * Android application <code>Context</code> required to access location services.
     */
    Context androidContext = null;
    /**
     * Whether to allow untrusted certificates for HTTPS connections.
     */
    boolean allowUntrustedCertificates = false;
    /**
     * Validates any host name, used only when {@link #allowUntrustedCertificates} is <code>true</code>.
     */
    private static HostnameVerifier untrustedHostNameVerifier = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    /**
     * Validates any certificate chain, used only when {@link #allowUntrustedCertificates} is <code>true</code>.
     */
    private static TrustManager[] untrustedTrustManagers = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};
    /**
     * SSL socket factory that validates any certificate chain, used only when {@link #allowUntrustedCertificates} is <code>true</code>.
     */
    private static SSLSocketFactory untrustedSocketFactory = null;

    static {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, untrustedTrustManagers, new SecureRandom());
            untrustedSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize this service with the specified server URL, context root, tenantCode name and credentials.
     * @param serverURL the base server URL in the form <code>http[s]://hostname[:port]</code>.
     * @param username the user name for HTTP authentication.
     * @param password the password for HTTP authentication.
     */
    public PIHttpService(Context context, String serverURL, String tenantCode, String orgCode, String username, String password) {
        this.username = username;
        this.password = password;
        this.connectivityHandler = new NetworkConnectivityHandler(context, this, false);
        this.serverURL = serverURL;
        this.tenantCode = tenantCode;
        this.orgCode = orgCode;
    }


    /**
     * Get the base server URL.
     * @return a string representing the base server URL in the form <code>http[s]://hostname[:port]</code>.
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * Get the Android application <code>Context</code> required to access location services.
     */
    public Context getAndroidContext() {
        return androidContext;
    }

    /**
     * Get the tenantCode id.
     */
    public String getTenantCode() {
        return tenantCode;
    }

    /**
     * Get the app id.
     */
    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }


    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Execute the specified request against the server.
     * @param <T> the type of result returned from the server.
     * @param request the request to execute.
     */
    public <T> void executeRequest(final PIRequest<T> request) {
        log.debug("executeRequest()");
        if (!connectivityHandler.isEnabled() || connectivityHandler.isNetworkActive()) {
            executor.execute(new RequestTask<>(request));
        } else {
            connectivityHandler.persistRequest(request);
        }
    }

    /**
     * Determine whether untrusted certificates, such as self-signed certificates or certificates
     * whose CA is not in the Android's trusted list, are allowed for HTTPS connections.
     * @return <code>true</code> if untrusted certificates are alloowed, <code>false</code> otherwise.
     */
    public boolean isAllowUntrustedCertificates() {
        return allowUntrustedCertificates;
    }

    /**
     * Specify whether untrusted certificates, such as self-signed certificates or certificates
     * whose CA is not in the Android's trusted list, are allowed for HTTPS connections.
     * @param allowUntrustedCertificates <code>true</code> to allow untrusted certificates are alloowed, <code>false</code> to deny them.
     */
    public void setAllowUntrustedCertificates(boolean allowUntrustedCertificates) {
        this.allowUntrustedCertificates = allowUntrustedCertificates;
    }

    /**
     * Get the object which holds session identifiers.
     * This object can be used to perform furtther requests to the server with the same HTTP session.
     * @return A {@link CookieManager} instance.
     */
    public CookieManager getCookieManager() {
        return manager;
    }

    /**
     * Set the header for basic authentication onto the specified connection.
     * @param connection the connection used for autheitcation.
     * @throws Exception if any error occurs.
     */
    void setAuthHeader(HttpURLConnection connection) throws Exception {
        String credentials = username + ":" + password;
        String encoded = Base64.encodeToString(credentials.getBytes(Utils.UTF_8), Base64.NO_WRAP);
        connection.setRequestProperty("Authorization", "Basic " + encoded);
    }

    /**
     * Set the header for basic authentication onto the specified connection.
     * @param connection the connection used for autheitcation.
     * @throws Exception if any error occurs.
     */
    void setUserAgentHeader(HttpURLConnection connection) throws Exception {
        connection.setRequestProperty(Utils.HTTP_HEADER_USER_AGENT, System.getProperty(Utils.PROPERTY_HTTP_AGENT));
    }

    /**
     * Return the base URL for querying content.
     * @return the URL as a {@link Uri} object.
     * @throws Exception if any error occurs.
     */
    protected Uri getBaseQueryURI() throws Exception {
        URL url = new URL(getServerURL());
        int port = url.getPort();
        String portStr = (port < 0) ? "" : ":" + port;
        Uri.Builder builder = new Uri.Builder().scheme(url.getProtocol()).encodedAuthority(url.getHost() + portStr);
        return builder.build();
    }

    /**
     * Log the error response of an HTTP request.
     * @param connection the connection encapsulating the request and response.
     */
    void logErrorBody(HttpURLConnection connection) {
        try {
            String err = Utils.readErrorBody(connection);
            log.debug("error body = " + err);
        } catch (Exception ignore) {
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
        sb.append("serverURL=").append(serverURL);
        sb.append(']');
        return sb.toString();
    }

    /**
     * Handle whether to allow untrusted certificated for an https connection.
     * @param connection the connection to check.
     * @return the connection, eventually updated to allow untrusted certificatess.
     * @throws Exception if any error occurs.
     */
    HttpURLConnection handleConnection(HttpURLConnection connection) throws Exception {
        if (allowUntrustedCertificates && "https".equalsIgnoreCase(connection.getURL().getProtocol())) {
            ((HttpsURLConnection) connection).setHostnameVerifier(untrustedHostNameVerifier);
            ((HttpsURLConnection) connection).setSSLSocketFactory(untrustedSocketFactory);
        }
        return connection;
    }

    /**
     * A task wrapping an HTTP request for asynchronous submission and queuing.
     * @param <T> the type of the result of the request.
     */
    class RequestTask<T> implements Runnable {
        final PIRequest<T> request;

        RequestTask(final PIRequest<T> request) {
            this.request = request;
        }

        @Override
        public void run() {
            new RequestAsyncTask<>(PIHttpService.this, request).execute();
        }
    }
}
