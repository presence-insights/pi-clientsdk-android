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

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class is package-protected so as not to be exposed to clients.
 */
final class Utils {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(Utils.class);
    // Android system properties
    static final String PROPERTY_HTTP_AGENT = "http.agent";
    /**
     * Name of the header sent by the server in a response to indicate a basic authentication challenge.
     */
    final static String AUTHENTICATION_CHALLENGE_HEADER = "WWW-Authenticate";
    // HTTP connection stuff
    static final String HTTP_HEADER_USER_AGENT = "User-Agent";
    static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    /**
     * Default resource bundle base name for localization.
     */
    static final String DEFAULT_LOCALIZATION_BASE_NAME = "com.ibm.caas.Messages";
    /**
     * The UTF-8 charset string.
     */
    static final String UTF_8 = "utf-8";

    static String localize(String messageId, Object... parameters) {
        return localize(Locale.getDefault(), DEFAULT_LOCALIZATION_BASE_NAME, messageId, parameters);
    }

    static String localize(Locale locale, String baseName, String messageId, Object... parameters) {
        //Log.v(LOG_TAG, String.format("locale=%s, baseName=%s, messageId=%s, parameters=%s", locale, baseName, messageId, Arrays.asList(parameters)));
        if (locale == null) {
            locale = Locale.getDefault();
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
            String source = bundle.getString(messageId);
            return MessageFormat.format(source, parameters);
        } catch (MissingResourceException e) {
            return "message '" + messageId + "' could not be localized (" + e.getMessage() + ")";
        }
    }

    static boolean isNullOrBlank(String source) {
        return (source == null) || "".equals(source.trim());
    }

    /**
     * Read a string content from a stream.
     * @param in the stream to read from.
     * @return the string extracted frm the stream.
     * @throws Exception if any error occurs.
     */
    static String readString(InputStream in) throws Exception {
        return new String(readBytes(in), "utf-8");
    }

    /**
     * Read bytes from an HTTP connection.
     * @param connection the connection to read from.
     * @return an array of the bytes read.
     * @throws Exception if any error occurs.
     */
    static byte[] readBytes(HttpURLConnection connection) throws Exception {
        return readBytes(new BufferedInputStream(connection.getInputStream()));
    }

    /**
     * Read a string content from a stream.
     * @param in the stream to read from.
     * @return the raw bytes extracted from the stream.
     * @throws Exception if any error occurs.
     */
    static byte[] readBytes(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        out.close();
        return out.toByteArray();
    }

    /**
     * Extract the body of an HTTP response as a string.
     * @param connection the connection to read from.
     * @return the string extracted from the connection's input stream.
     * @throws Exception if any error occurs.
     */
    static String readResponseBody(HttpURLConnection connection) throws Exception {
        return readString(new BufferedInputStream(connection.getInputStream()));
    }

    /**
     * Determine whether the response contains text data, based on the "Content-Type" header.
     * @param connection the connection from which the response is extracted.
     * @return <code>true</code> if the response contains text data, <code>false</code> otherwise.
     * @throws Exception if any eror occurs.
     */
    static boolean isTextResponseBody(HttpURLConnection connection) throws Exception {
        Map<String, List<String>> headers = connection.getHeaderFields();
        List<String> types = headers.get("Content-Type");
        if (types == null) {
            types = new ArrayList<String>();
        }
        String type = types.isEmpty() ? null : types.get(0);
        log.debug("response content type = [" + type + "]");
        return !((type == null) || (!type.contains("application/json") && !type.contains("text/html")));
    }

    /**
     * Extract the error of an HTTP response as a string.
     * @param connection the connection to read from.
     * @return the string extracted from the connection's error stream.
     * @throws Exception if any error occurs.
     */
    static String readErrorBody(HttpURLConnection connection) throws Exception {
        return readString(new BufferedInputStream(connection.getErrorStream()));
    }

    /**
     * Print the HTTP request headers to Logcat. This method is for tracing and debugging purposes only.
     * @param connection the HTTP connection from which to extract the headers.
     * @throws Exception if any error occurs.
     */
    static void logRequestHeaders(HttpURLConnection connection) throws Exception {
        StringBuilder sb = new StringBuilder();
        Map<String, List<String>> headers = connection.getRequestProperties();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
        }
        log.debug("request header:\n" + sb.toString());
    }

    /**
     * Print the HTTP reponse headers to Logcat. This method is for tracing and debugging purposes only.
     * @param connection the HTTP connection from which to extract the headers.
     * @throws Exception if any error occurs.
     */
    static void logResponseHeaders(HttpURLConnection connection) throws Exception {
        try {
            Map<String, List<String>> headers = connection.getHeaderFields();
            if (headers != null) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
                }
                log.debug("response header:\n" + sb.toString());
            } else {
                log.debug("no response headers");
            }
        } catch (Exception e) {
            log.debug("error logging response headers: ", e);
        }
    }

    /**
     * Determine whether the response contains a basic authentication challenge.
     * @param connection the connection from which the response is extracted.
     * @return <code>true</code> if the response contains an authentication challenge, <code>false</code> otherwise.
     */
    static boolean hasAuthenticationChallenge(HttpURLConnection connection) {
        Map<String, List<String>> headers = connection.getHeaderFields();
        return headers.get(AUTHENTICATION_CHALLENGE_HEADER) != null;
    }

    static String urlEncode(String source) {
        try {
            return URLEncoder.encode(source, UTF_8).replace("+", "%20");
        } catch (Exception e) {
            log.error("error encoding string '" + source + "' : ", e);
        }
        return source;
    }

    static String urlDeccode(String source) {
        try {
            return URLDecoder.decode(source, UTF_8).replace("+", "%20");
        } catch (Exception e) {
            log.error("error encoding string '" + source + "' : ", e);
        }
        return source;
    }

    private static void testJDK7(String someString) {
        // underscores in number literals
        long l = 1_000_000L;
        // diamond operator
        List<String> list = new ArrayList<>();
        // string switch
        switch (someString) {
            case "hello":
                break;
            case "bonjour":
                break;
            default:
                break;
        }
        // multiple catch
        try {
        } catch (Exception | Error e) {
        }
        // try_with_resource
        // this will only compile if compileSdkVersion and minSdkVersion are >= 19
        /*
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        } catch(Exception e) {
        }
        */
    }
}
