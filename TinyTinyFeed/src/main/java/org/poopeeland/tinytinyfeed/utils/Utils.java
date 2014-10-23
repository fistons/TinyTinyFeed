package org.poopeeland.tinytinyfeed.utils;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;

import java.security.KeyStore;

/**
 * Collections of usefull methods
 * Created by eric on 22/10/14.
 */
public abstract class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static final HttpClient getNewHttpClient(SharedPreferences preferences, String httpUser, String httpPassword) {
        DefaultHttpClient client;
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf;
            if (preferences.getBoolean(TinyTinyFeedWidget.ALL_SLL_KEY, false)) {
                sf = new MySSLSocketFactory(trustStore);
            } else {
                sf = SSLSocketFactory.getSocketFactory();
            }

            if (preferences.getBoolean(TinyTinyFeedWidget.ALL_HOST_KEY, false)) {
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            }

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
            client = new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            Log.e(TAG, "Problem creating the ssl client", e);
            client = new DefaultHttpClient();
        }

        if (!httpUser.isEmpty()) {
            client.getCredentialsProvider().setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(httpUser, httpPassword));
        }

        return client;
    }

    public static void checkNetwork(ConnectivityManager connectivityManager) throws NoInternetException {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new NoInternetException();
        }
    }
}
