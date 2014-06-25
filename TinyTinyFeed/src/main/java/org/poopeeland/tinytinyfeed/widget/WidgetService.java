package org.poopeeland.tinytinyfeed.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViewsService;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.Article;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.RequestTask;
import org.poopeeland.tinytinyfeed.SetupActivity;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;
import org.poopeeland.tinytinyfeed.exceptions.UrlSuffixException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by eric on 11/05/14.
 */
public class WidgetService extends RemoteViewsService {

    public static final String ACTIVITY_FLAG = "Activity";
    private static final String TAG = "WidgetService";
    protected IBinder binder = new LocalBinder();
    private ConnectivityManager connMgr;
    private String session;
    private String url;
    private String password;
    private String user;
    private String numArticles;
    private boolean onlyUnread;
    private DefaultHttpClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new ListProvider(this));
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        this.start();

        if (intent.getExtras().containsKey(ACTIVITY_FLAG)) {
            return binder;
        } else {
            return super.onBind(intent);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbinded");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Don't! stop! me! noooooow...");
        super.onDestroy();
    }

    /**
     * Log into the TTRss server
     *
     * @throws org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred if some required information has not been registred
     * @throws org.json.JSONException                                          (should not happen)
     * @throws java.util.concurrent.ExecutionException
     * @throws InterruptedException
     * @throws org.poopeeland.tinytinyfeed.exceptions.CheckException           When something wring happened with the server
     */
    private void login() throws RequiredInfoNotRegistred, JSONException, ExecutionException, InterruptedException, CheckException {

        checkRequieredInfoRegistred();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("password", password);
        jsonObject.put("op", "login");

        RequestTask task = new RequestTask(this.client, this.url);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
        this.session = response.getJSONObject("content").getString("session_id");
    }

    public List<Article> updateFeeds() throws RequiredInfoNotRegistred, CheckException, JSONException, ExecutionException, InterruptedException, NoInternetException {
        try {
            this.checkNetwork();
        } catch (NoInternetException ex) {
            Log.d(TAG, "No internet right now, load the last list");
            return this.loadLastList();
        }
        List<Article> list = new ArrayList();
        if (!isLogged()) {
            login();
        }

        String feedId;
        if (onlyUnread) {
            Log.d(TAG, "Retrieve only unread articles");
            feedId = "-3";
        } else {
            Log.d(TAG, "Retrieve all articles");
            feedId = "-4";
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", session);
        jsonObject.put("op", "getHeadlines");
        jsonObject.put("feed_id", feedId);
        jsonObject.put("limit", this.numArticles);
        jsonObject.put("show_excerpt", "true");

        RequestTask task = new RequestTask(this.client, this.url);
        task.execute(jsonObject);
        JSONObject response = task.get();

        checkJsonResponse(response);

        for (int i = 0; i < response.getJSONArray("content").length(); i++) {
            list.add(new Article(response.getJSONArray("content").getJSONObject(i)));
        }

        this.saveList(response.getJSONArray("content"));

        return list;
    }

    public void setArticleToRead(Article article) throws CheckException, ExecutionException, InterruptedException, JSONException, RequiredInfoNotRegistred, NoInternetException {
        this.checkNetwork();
        Log.d(TAG, String.format("Article %s set to read", article.getTitle()));
        if (!isLogged()) {
            login();
        }
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("sid", session);
        jsonObject.put("op", "updateArticle");
        jsonObject.put("article_ids", article.getId());
        jsonObject.put("mode", "0");
        jsonObject.put("field", "2");

        RequestTask task = new RequestTask(this.client, this.url);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
    }

    public void checkSetup(String url, String httpUser, String httpPassword, String user, String password) throws MalformedURLException, UrlSuffixException, JSONException, ExecutionException, InterruptedException, CheckException, NoInternetException {
        this.checkNetwork();
        if (!url.endsWith(SetupActivity.URL_SUFFIX)) {
            throw new UrlSuffixException();
        }
        new URL(url); // To check if the URL is a real one
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("password", password);
        jsonObject.put("op", "login");
        DefaultHttpClient client = new DefaultHttpClient();
        if (!httpUser.isEmpty()) {
            client.getCredentialsProvider().setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(httpUser, httpPassword));
        }
        RequestTask task = new RequestTask(client, url);
        task.execute(jsonObject);
        JSONObject response = task.get();
        if (response.getInt("status") != 0) {
            try {
                TtrssError reason = TtrssError.valueOf(response.getJSONObject("content").getString("error"));
                switch (reason) {
                    case LOGIN_ERROR:
                        Log.e(TAG, response.getJSONObject("content").getString("error"));
                        throw new CheckException(getText(R.string.badLogin).toString());
                    case CLIENT_PROTOCOL_EXCEPTION:
                    case UNREACHABLE_TTRSS:
                    case IO_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(getString(R.string.connectionError));
                    case HTTP_AUTH_REQUIERED:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(getString(R.string.connectionAuthError));
                    case UNSUPPORTED_ENCODING:
                    case JSON_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(getString(R.string.impossibleError), response.getJSONObject("content").getString("message")));
                    default:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
                }
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, response.getJSONObject("content").getString("message"));
                throw new CheckException(String.format(getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
            }
        }

    }


    private boolean isLogged() throws RequiredInfoNotRegistred, JSONException, ExecutionException, InterruptedException, CheckException {
        checkRequieredInfoRegistred();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", session);
        jsonObject.put("op", "isLoggedIn");

        RequestTask task = new RequestTask(this.client, this.url);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
        return response.getJSONObject("content").getBoolean("status");
    }

    private void checkJsonResponse(JSONObject response) throws CheckException, JSONException {
        if (response.getInt("status") != 0) {
            try {
                TtrssError reason = TtrssError.valueOf(response.getJSONObject("content").getString("error"));
                switch (reason) {
                    case LOGIN_ERROR:
                        Log.e(TAG, response.getJSONObject("content").getString("error"));
                        throw new CheckException(getText(R.string.badLogin).toString());
                    case CLIENT_PROTOCOL_EXCEPTION:
                    case UNREACHABLE_TTRSS:
                    case IO_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(getString(R.string.connectionError));
                    case HTTP_AUTH_REQUIERED:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(getString(R.string.connectionAuthError));
                    case UNSUPPORTED_ENCODING:
                    case JSON_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(getString(R.string.impossibleError), response.getJSONObject("content").getString("message")));
                    default:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
                }
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, response.getJSONObject("content").getString("message"));
                throw new CheckException(String.format(getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
            }
        }
    }

    /**
     * Save the articles in the preferences, so it can be retrevied when an update
     *
     * @param json the articles to save
     */
    private void saveList(JSONArray json) {
        Log.d(TAG, "Saving the list");
        SharedPreferences.Editor editor = getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TinyTinyFeedWidget.LAST_LIST_KEY, json.toString());
        editor.commit();
    }

    /**
     * Load the last saved list from the preference (pretty ugly, maybe I should store it elsewhere?)
     *
     * @return the last list of articles
     * @throws JSONException
     */
    private List<Article> loadLastList() throws JSONException {
        SharedPreferences preferences = getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE);
        String json = preferences.getString(TinyTinyFeedWidget.LAST_LIST_KEY, "");
        if (json.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        JSONArray response = new JSONArray(json);
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            articles.add(new Article(response.getJSONObject(i)));
        }
        return articles;
    }

    /**
     * Check if all the requiered information has been filled
     *
     * @throws RequiredInfoNotRegistred if it is not the case
     */
    private void checkRequieredInfoRegistred() throws RequiredInfoNotRegistred {
        SharedPreferences preferences = getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (!(preferences.contains(TinyTinyFeedWidget.URL_KEY) &&
                preferences.contains(TinyTinyFeedWidget.USER_KEY) &&
                preferences.contains(TinyTinyFeedWidget.PASSWORD_KEY) &&
                preferences.contains(TinyTinyFeedWidget.NUM_ARTICLE_KEY))) {
            throw new RequiredInfoNotRegistred();
        }
    }

    /**
     * Check if a data connection is available
     *
     * @return true if we can use the use the data connection, false otherwise
     */
    private void checkNetwork() throws NoInternetException {
        NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new NoInternetException();
        }
    }

    /**
     * Prepare the service
     */
    private void start() {
        this.connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        SharedPreferences preferences = getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE);
        this.url = preferences.getString(TinyTinyFeedWidget.URL_KEY, "");
        this.user = preferences.getString(TinyTinyFeedWidget.USER_KEY, "");
        this.password = preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, "");
        this.numArticles = preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "");
        this.onlyUnread = preferences.getBoolean(TinyTinyFeedWidget.ONLY_UNREAD_KEY, false);
        String httpUser = preferences.getString(TinyTinyFeedWidget.HTTP_USER_KEY, "");
        String httpPassword = preferences.getString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, "");
        this.client = new DefaultHttpClient();
        if (!httpUser.isEmpty()) {
            this.client.getCredentialsProvider().setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(httpUser, httpPassword));
        }
        Log.d(TAG, "Preferences loaded");

    }


    public class LocalBinder extends Binder {
        public WidgetService getService() {
            return WidgetService.this;
        }
    }

}
