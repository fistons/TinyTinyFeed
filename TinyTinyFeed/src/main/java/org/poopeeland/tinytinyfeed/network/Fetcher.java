package org.poopeeland.tinytinyfeed.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.network.exceptions.ApiDisabledException;
import org.poopeeland.tinytinyfeed.network.exceptions.BadCredentialException;
import org.poopeeland.tinytinyfeed.network.exceptions.FetchException;
import org.poopeeland.tinytinyfeed.network.exceptions.GeneralHttpException;
import org.poopeeland.tinytinyfeed.network.exceptions.HttpAuthException;
import org.poopeeland.tinytinyfeed.network.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.network.exceptions.SslException;
import org.poopeeland.tinytinyfeed.models.Article;
import org.poopeeland.tinytinyfeed.models.Category;
import org.poopeeland.tinytinyfeed.models.JsonWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget.JSON_STORAGE_FILENAME_TEMPLATE;

/**
 * Fetch the datas.
 * Created by eric on 28/05/17.
 */

public class Fetcher {

    private static final String TAG = Fetcher.class.getSimpleName();
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
            new X509TrustManager() {
                @Override
                @SuppressLint("TrustAllX509TrustManager")
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    // Do nothing because everybody is beautiful
                }

                @Override
                @SuppressLint("TrustAllX509TrustManager")
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    // Do nothing because everything is awesome
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };

    private final Context context;

    private final OkHttpClient httpClient;

    private final String url;
    private final String user;
    private final String password;
    private final String httpAuthUser;
    private final String httpAuthPassword;
    private final String numArticles;
    private final String excerptLength;
    private final String filenameTemplate;

    private final boolean allowAllSslKey;
    private final boolean allowAllSslHost;
    private final boolean onlyUnread;

    public Fetcher(final SharedPreferences preferences, final Context context) throws FetchException {
        this.context = context;
        this.url = preferences.getString(TinyTinyFeedWidget.URL_KEY, "") + "/api/";
        this.user = preferences.getString(TinyTinyFeedWidget.USER_KEY, "");
        this.password = preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, "");
        this.allowAllSslKey = preferences.getBoolean(TinyTinyFeedWidget.ALL_SLL_KEY, false);
        this.allowAllSslHost = preferences.getBoolean(TinyTinyFeedWidget.ALL_HOST_KEY, false);
        this.httpAuthUser = preferences.getString(TinyTinyFeedWidget.HTTP_USER_KEY, "");
        this.httpAuthPassword = preferences.getString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, "");
        this.numArticles = preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "");
        this.onlyUnread = preferences.getBoolean(TinyTinyFeedWidget.ONLY_UNREAD_KEY, false);
        this.excerptLength = preferences.getString(TinyTinyFeedWidget.EXCERPT_LENGTH_KEY, context.getText(R.string.preference_excerpt_lenght_default_value).toString());
        this.filenameTemplate = context.getApplicationContext().getFilesDir() + File.separator + JSON_STORAGE_FILENAME_TEMPLATE;

        try {
            this.httpClient = getOkHttpClient();
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new FetchException(ex);
        }
    }

    private static JSONObject parseResponse(final Response response) throws FetchException {
        checkHttpResponse(response);
        try {
            ResponseBody body = response.body();
            if (body == null) {
                throw new FetchException("HTTP Response body is null!");
            }
            return checkJsonResponse(new JSONObject(body.string()));
        } catch (IOException | JSONException e) {
            throw new FetchException(e);
        }
    }

    private void checkIfNetworkAvailable() throws NoInternetException {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            throw new NoInternetException();
        }
    }

    private Response call(final Request request) throws FetchException {
        Call call = this.httpClient.newCall(request);
        try {
            return call.execute();
        } catch (SSLHandshakeException | SSLPeerUnverifiedException ex) {
            throw new SslException();
        } catch (UnknownHostException ex) {
            throw new GeneralHttpException();
        } catch (IOException e) {
            throw new FetchException(e);
        }
    }

    private OkHttpClient getOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        Log.d(TAG, "Creating http client");

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());

        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (this.allowAllSslKey) {
            Log.d(TAG, "Allowing all SSL certificates");
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS[0]);
        }
        if (this.allowAllSslHost) {
            Log.d(TAG, "Allowing all SSL hostname");
            builder.hostnameVerifier((hostname, session) -> true);
        }

        if (!this.httpAuthUser.isEmpty()) {
            Log.d(TAG, "Using HTTP basic auth with user " + this.httpAuthUser);

            builder.authenticator((route, response) -> {
                String credentials = Credentials.basic(httpAuthUser, httpAuthPassword);
                if (credentials.equals(response.request().header("Authorization"))) {
                    return null; // If we already failed with these credentials, don't retry.
                }
                return response.request().newBuilder()
                        .header("Authorization", credentials)
                        .build();
            });
        }

        Log.d(TAG, "Http client created");
        return builder.build();
    }

    private Request prepareRequest(final JSONObject json) {
        return new Request.Builder()
                .url(this.url)
                .post(RequestBody.create(MediaType.parse("application/json"), json.toString()))
                .build();
    }

    private String login() throws FetchException {
        Log.d(TAG, "Login to api using " + this.user);
        final JSONObject jsonLogin = new JSONObject();
        try {
            jsonLogin.put("user", this.user);
            jsonLogin.put("password", this.password);
            jsonLogin.put("op", "login");
        } catch (JSONException ex) {
            throw new FetchException(ex);
        }

        try {
            Request request = prepareRequest(jsonLogin);
            Response response = call(request);
            JSONObject jsonResponse = parseResponse(response);
            return jsonResponse.getJSONObject("content").getString("session_id");
        } catch (JSONException e) {
            throw new FetchException("Cant't login! " + e.getMessage(), e);
        }
    }

    private void logout(final String sessionId) throws FetchException {
        Log.d(TAG, "Logout session " + sessionId);
        final JSONObject jsonLogin = new JSONObject();
        try {
            jsonLogin.put("sid", sessionId);
            jsonLogin.put("op", "logout");
        } catch (JSONException ex) {
            throw new FetchException(ex);
        }

        Request request = prepareRequest(jsonLogin);
        Response response = call(request);
        parseResponse(response);

    }

    public void testConnection() throws FetchException {
        Log.d(TAG, "Testing connection...");
        checkIfNetworkAvailable();
        String session = login();
        logout(session);
        Log.d(TAG, "Connection ok!");
    }

    public List<Category> fetchCategories() throws FetchException {
        Log.d(TAG, "Fetching categories");
        checkIfNetworkAvailable();
        final String sessionId = login();

        final JSONObject json = new JSONObject();
        try {
            json.put("sid", sessionId);
            json.put("op", "getCategories");
            json.put("enable_nested", false);
            json.put("include_empty", true);
        } catch (JSONException ex) {
            throw new FetchException(ex);
        }

        List<Category> categories = new ArrayList<>();
        try {
            Request request = prepareRequest(json);
            Response response = call(request);
            JSONObject jsonResponse = parseResponse(response);
            JSONArray array = jsonResponse.getJSONArray("content");
            for (int i = 0; i < array.length(); i++) {
                JSONObject c = array.getJSONObject(i);
                categories.add(JsonWrapper.fromJson(c.toString(), Category.class));
            }
        } catch (JSONException e) {
            throw new FetchException("Error while fetching categories", e);
        } finally {
            logout(sessionId);
        }
        Log.d(TAG, "Categories fetched");

        return categories;
    }

    public List<Article> fetchFeeds(final int widgetId, final Set<String> categoryIds) throws FetchException {
        Log.d(TAG, "Fetching feeds for widget " + widgetId + " with categories");
        checkIfNetworkAvailable();
        String session = login();
        final List<Article> articles = new ArrayList<>();
        for (String catId : categoryIds) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("sid", session);
                jsonObject.put("op", "getHeadlines");
                jsonObject.put("feed_id", catId);
                jsonObject.put("limit", this.numArticles);
                jsonObject.put("show_excerpt", "true");
                jsonObject.put("excerpt_length", this.excerptLength);
                jsonObject.put("force_update", "false"); // TODO Add as on option
                jsonObject.put("is_cat", "true");
                jsonObject.put("view_mode", onlyUnread ? "unread" : "all_articles");
            } catch (JSONException ex) {
                Log.e(TAG, "Json exception while creating the update article request", ex);
                throw new FetchException(ex);
            }

            Log.d(TAG, "Fetching cat. " + catId + " for widget #" + widgetId + "...");
            try {
                Request request = prepareRequest(jsonObject);
                Response response = call(request);
                JSONObject jsonResponse = parseResponse(response);
                JSONArray array = jsonResponse.getJSONArray("content");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject c = array.getJSONObject(i);
                    articles.add(JsonWrapper.fromJson(c.toString(), Article.class));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error while fetching cat. " + catId + " for widget #" + widgetId + " done!", e);
            }
            Log.d(TAG, "Fetching cat. " + catId + " for widget #" + widgetId + " done!");
        }
        Log.d(TAG, "Fetching done for widget #" + widgetId + " " + articles.size() + " articles fetched");

        Collections.sort(articles);
        List<Article> subList = articles.subList(0, Math.min(Integer.parseInt(numArticles), articles.size()));
        logout(session);

        return saveList(subList, widgetId);
    }

    public void setArticleToRead(final Article article) throws FetchException {
        Log.d(TAG, String.format("Setting article %s set read...", article.getId()));
        checkIfNetworkAvailable();
        String session = login();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sid", session);
            jsonObject.put("op", "updateArticle");
            jsonObject.put("article_ids", article.getId());
            jsonObject.put("mode", "0");
            jsonObject.put("field", "2");
        } catch (JSONException ex) {
            Log.e(TAG, "Json exception while creating the update article request", ex);
            throw new FetchException(ex);
        }

        try {
            Request request = prepareRequest(jsonObject);
            Response response = call(request);
            parseResponse(response);
        } finally {
            logout(session);
        }
        Log.d(TAG, String.format("Article %s set to read!", article.getId()));

    }

    private List<Article> saveList(final List<Article> articles, final int widgetId) {
        File fileName = new File(String.format(this.filenameTemplate, widgetId));
        Log.d(TAG, String.format("Saving the list to %s", fileName.getAbsolutePath()));
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(fileName))) {
            outputStream.writeObject(new ArrayList<>(articles));
        } catch (Exception e) {
            Log.e(TAG, "Error while saving the last articles list", e);
        }

        return articles;
    }

    private static Response checkHttpResponse(final Response response) throws FetchException {
        if (!response.isSuccessful()) {
            Log.e(TAG, "Http error: " + response.code());
            switch (response.code()) {
                case 401:
                case 403:
                    throw new HttpAuthException();
                default:
                    throw new GeneralHttpException();
            }
        }
        return response;
    }

    private static JSONObject checkJsonResponse(final JSONObject response) throws FetchException {
        try {
            if (response.getInt("status") != 0) {
                String reason = response.getJSONObject("content").getString("error").toUpperCase();
                switch (reason) {
                    case "LOGIN_ERROR":
                        Log.e(TAG, reason);
                        throw new BadCredentialException();
                    case "API_DISABLED":
                        Log.e(TAG, "API disabled...");
                        throw new ApiDisabledException();
                    default:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new FetchException("Unknown error while checking JSON response");
                }
            }
        } catch (JSONException ex) {
            throw new FetchException("Impossible to parse JSON response");
        }
        return response;
    }
}
