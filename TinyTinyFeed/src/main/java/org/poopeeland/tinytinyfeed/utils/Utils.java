package org.poopeeland.tinytinyfeed.utils;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;

import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.HttpConnectionException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Collections of usefull methods
 * Created by eric on 22/10/14.
 */
public abstract class Utils {

    private static final String TAG = Utils.class.getSimpleName();
    private static final String URL_SUFFIX = "/api/";
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            // Placeholder
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            // Placeholder
        }
    }};


    private static String createBasicAuth(final String user, final String password) {
        String userPassword = String.format("%s:%s", user, password);
        return Base64.encodeToString(userPassword.getBytes(), Base64.DEFAULT);
    }

    public static HttpURLConnection getHttpURLConnection(final SharedPreferences preferences) throws HttpConnectionException {
        boolean needHttpAuth = !preferences.getString(TinyTinyFeedWidget.HTTP_USER_KEY, "").trim().isEmpty();
        String urlString = preferences.getString(TinyTinyFeedWidget.URL_KEY, "") + URL_SUFFIX;

        HttpURLConnection connection;
        if (preferences.getBoolean(TinyTinyFeedWidget.ALL_HOST_KEY, false)) {
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            Log.d(TAG, "All hostname allowed");
        }

        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            if (preferences.getBoolean(TinyTinyFeedWidget.ALL_SLL_KEY, false)) {
                sc.init(null, TRUST_ALL_CERTS, null);
                Log.d(TAG, "Trust all certs");
            } else {
                sc.init(null, null, null);
                Log.d(TAG, "Does not trust all certs");
            }
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new HttpConnectionException(ex);
        }


        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("charset", "utf-8");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            if (needHttpAuth) {
                String user = preferences.getString(TinyTinyFeedWidget.HTTP_USER_KEY, "");
                String password = preferences.getString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, "");
                connection.setRequestProperty("Authorization", String.format("Basic %s", createBasicAuth(user, password)));
                Log.d(TAG, String.format("Http basic auth with user %s", user));
            }
            return connection;
        } catch (IOException ex) {
            throw new HttpConnectionException(ex);
        }
    }


    public static void checkNetwork(final ConnectivityManager connectivityManager) throws NoInternetException {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new NoInternetException();
        }
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ex) {
            // Ignore
            Log.e(TAG, ex.getMessage());
        }
    }
}
