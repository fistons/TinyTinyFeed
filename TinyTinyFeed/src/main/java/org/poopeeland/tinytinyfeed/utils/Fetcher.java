package org.poopeeland.tinytinyfeed.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.rx2androidnetworking.Rx2AndroidNetworking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;
import org.poopeeland.tinytinyfeed.model.Article;
import org.poopeeland.tinytinyfeed.model.Category;
import org.poopeeland.tinytinyfeed.model.JsonWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

import static org.poopeeland.tinytinyfeed.TinyTinyFeedWidget.JSON_STORAGE_FILENAME_TEMPLATE;

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
            AndroidNetworking.initialize(context, getOkHttpClient());
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new FetchException(ex);
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
            builder.authenticator((route, response) ->
                    response.request().newBuilder()
                            .header("Authorization", Credentials.basic(httpAuthUser, httpAuthPassword))
                            .build());
        }

        Log.d(TAG, "Http client created");
        return builder.build();
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

        return Rx2AndroidNetworking.post(this.url)
                .addJSONObjectBody(jsonLogin)
                .build()
                .getJSONObjectObservable()
                .subscribeOn(Schedulers.io())
                .map(jsonObject -> {
                    if (jsonObject.getJSONObject("content").has("error")) {
                        throw new FetchException("Cant't login!");
                    } else {
                        return jsonObject.getJSONObject("content").getString("session_id");
                    }
                })
                .blockingFirst();
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

        Rx2AndroidNetworking.post(this.url)
                .addJSONObjectBody(jsonLogin)
                .build()
                .getJSONObjectObservable()
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public List<Category> fetchCategories() throws FetchException {
        Log.d(TAG, "Fetching categories");
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


        List<Category> categories = Rx2AndroidNetworking.post(this.url)
                .addJSONObjectBody(json)
                .build()
                .getJSONObjectObservable()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    List<Category> categories1 = new ArrayList<>();
                    JSONArray array = response.getJSONArray("content");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject c = array.getJSONObject(i);
                        categories1.add(JsonWrapper.fromJson(c.toString(), Category.class));
                    }
                    return categories1;
                })
                .blockingFirst();

        Log.d(TAG, "Categories fetched");
        logout(sessionId);

        return categories;
    }

    public List<Article> fetchFeeds(final int widgetId, final Set<String> categoryIds) throws FetchException {
        Log.d(TAG, "Fetching feeds for widget " + widgetId + " with categories");

        String session = login();

        List<ObservableSource<JSONObject>> observables = new ArrayList<>();
        for (String catId : categoryIds) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("sid", session);
                jsonObject.put("op", "getHeadlines");
                jsonObject.put("feed_id", catId);
                jsonObject.put("limit", this.numArticles);
                jsonObject.put("show_excerpt", "true");
                jsonObject.put("excerpt_length", this.excerptLength);
                jsonObject.put("force_update", "true");
                jsonObject.put("is_cat", "true");
                jsonObject.put("view_mode", onlyUnread ? "unread" : "all_articles");
            } catch (JSONException ex) {
                Log.e(TAG, "Json exception while creating the update article request", ex);
                throw new FetchException(ex);
            }

            observables.add(Rx2AndroidNetworking.post(this.url)
                    .addJSONObjectBody(jsonObject)
                    .build()
                    .getJSONObjectObservable()
                    .subscribeOn(Schedulers.io())
                    .doOnComplete(() -> Log.d(TAG, "Fetching " + catId + " done for widget #" + widgetId)));
        }

        final List<Article> articles = new ArrayList<>();
        Observable.concat(observables)
                .map(json -> {
                    List<Article> articles1 = new ArrayList<>();
                    JSONArray array = json.getJSONArray("content");
                    this.saveList(array, widgetId);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject c = array.getJSONObject(i);
                        articles1.add(JsonWrapper.fromJson(c.toString(), Article.class));
                    }
                    return articles1;
                })
                .doOnComplete(() -> Log.d(TAG, "Fetching done for widget #" + widgetId))
                .blockingSubscribe(articles::addAll);

        articles.sort((art1, art2) -> {
            if (art1.getUpdated() > art2.getUpdated()) {
                return -1;
            }

            if (art1.getUpdated() < art2.getUpdated()) {
                return 1;
            }

            return 0;
        });

        logout(session);

        return articles;
    }


    public void setArticleToRead(final Article article) throws FetchException {
        Log.d(TAG, String.format("Setting article %s set read...", article.getId()));

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
        Rx2AndroidNetworking.post(this.url)
                .addJSONObjectBody(jsonObject)
                .build()
                .getJSONObjectObservable()
                .subscribeOn(Schedulers.io())
                .blockingSubscribe();
        Log.d(TAG, String.format("Article %s set to read!", article.getId()));
        logout(session);
    }

    private void saveList(final JSONArray json, final int widgetId) {
        if (json == null) {
            Log.d(TAG, "List is empty, not saving it");
            return;
        }
        File fileName = new File(String.format(this.filenameTemplate, widgetId));

        Log.d(TAG, String.format("Saving the list to %s", fileName.getAbsolutePath()));
        try {
            FileOutputStream outputStream = new FileOutputStream(fileName);
            outputStream.write(json.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error while saving the last articles list", e);
        }
    }


    private void checkJsonResponse(final JSONObject response) throws CheckException, JSONException {
        if (response.getInt("status") != 0) {
            try {
                TtrssError reason = TtrssError.valueOf(response.getJSONObject("content").getString("error"));
                switch (reason) {
                    case LOGIN_ERROR:
                        Log.e(TAG, response.getJSONObject("content").getString("error"));
                        throw new CheckException(this.context.getText(R.string.badLogin).toString());
                    case CLIENT_PROTOCOL_EXCEPTION:
                    case UNREACHABLE_TTRSS:
                    case IO_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(this.context.getString(R.string.connectionError));
                    case SSL_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(this.context.getString(R.string.ssl_exception_message));
                    case HTTP_AUTH_REQUIRED:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(this.context.getString(R.string.connectionAuthError));
                    case UNSUPPORTED_ENCODING:
                    case JSON_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(this.context.getString(R.string.impossibleError), response.getJSONObject("content").getString("message")));
                    case API_DISABLED:
                        Log.e(TAG, "API disabled...");
                        throw new CheckException(this.context.getString(R.string.setupApiDisabled));
                    default:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(this.context.getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
                }
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, response.getJSONObject("content").getString("message"));
                throw new CheckException(String.format(this.context.getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
            }
        }
    }
}
