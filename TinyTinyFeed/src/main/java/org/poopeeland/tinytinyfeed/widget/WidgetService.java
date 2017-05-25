package org.poopeeland.tinytinyfeed.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.RequestTask;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistered;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;
import org.poopeeland.tinytinyfeed.model.Article;
import org.poopeeland.tinytinyfeed.model.JsonWrapper;
import org.poopeeland.tinytinyfeed.utils.HttpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Central service managing the connections to the server
 * Created by eric on 11/05/14.
 */
public class WidgetService extends RemoteViewsService {

    public static final String ACTIVITY_FLAG = "Activity";
    public static final String LIST_FILENAME = "listArticles.json";
    private static final String TAG = WidgetService.class.getSimpleName();
    private static final String ARTICLE_ALL = "-4";
    private static final String ARTICLE_ONLY_UNREAD = "-3";
    private final IBinder binder = new LocalBinder();

    private ConnectivityManager connMgr;
    private SharedPreferences preferences;
    private ListProvider listProvider;
    private File lastListFile;
    private String password;
    private String user;
    private String numArticles;
    private boolean onlyUnread;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        this.lastListFile = new File(getApplicationContext().getFilesDir(), WidgetService.LIST_FILENAME);
        this.listProvider = new ListProvider(this);
        this.connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d(TAG, "Preferences loaded");
    }

    private void refreshParams() {
        this.user = preferences.getString(TinyTinyFeedWidget.USER_KEY, "");
        this.password = preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, "");
        this.numArticles = preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "");
        this.onlyUnread = preferences.getBoolean(TinyTinyFeedWidget.ONLY_UNREAD_KEY, false);
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return this.listProvider;
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.refreshParams();
        Log.d(TAG, "onBind");
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
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
    }

    /**
     * Log into the TTRss server
     *
     * @throws RequiredInfoNotRegistered if some required information has not been registred
     * @throws org.json.JSONException                                          (should not happen)
     * @throws java.util.concurrent.ExecutionException
     * @throws InterruptedException
     * @throws org.poopeeland.tinytinyfeed.exceptions.CheckException           When something wrong happened with the server
     */
    private String login() throws RequiredInfoNotRegistered, JSONException, ExecutionException, InterruptedException, CheckException {
        Log.d(TAG, "Login");
        checkRequieredInfoRegistred();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("password", password);
        jsonObject.put("op", "login");

        RequestTask task = new RequestTask(preferences);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
        return response.getJSONObject("content").getString("session_id");
    }

    private void logout(String session) throws JSONException, ExecutionException, InterruptedException, CheckException {
        Log.d(TAG, "Logout");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("op", "logout");
        jsonObject.put("sid", session);
        RequestTask task = new RequestTask(preferences);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
    }


    /**
     * Refresh the feeds et return the list of articles
     *
     * @return the list of last articles
     * @throws RequiredInfoNotRegistered if the requiered info (login, password, url, etc) are not registred
     * @throws CheckException           if the json response is not correct
     * @throws JSONException            if there is a probleme with json parsing
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NoInternetException      if the is no internet connexion right now
     */
    public List<Article> updateFeeds() throws RequiredInfoNotRegistered, CheckException, JSONException, ExecutionException, InterruptedException, NoInternetException {
        Log.d(TAG, "updateFeeds");

        HttpUtils.checkNetwork(this.connMgr);
        List<Article> list = new ArrayList<>();

        String session = login();

        String feedId = this.onlyUnread ? ARTICLE_ONLY_UNREAD : ARTICLE_ALL;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", session);
        jsonObject.put("op", "getHeadlines");
        jsonObject.put("feed_id", feedId);
        jsonObject.put("limit", this.numArticles);
        jsonObject.put("show_excerpt", "true");
        jsonObject.put("excerpt_length", preferences.getString(TinyTinyFeedWidget.EXCERPT_LENGTH_KEY, getText(R.string.preference_excerpt_lenght_default_value).toString()));
        jsonObject.put("force_update", "true");

        RequestTask task = new RequestTask(preferences);
        task.execute(jsonObject);
        JSONObject response = task.get();

        logout(session);

        checkJsonResponse(response);
        this.saveList(response.getJSONArray("content"));

        for (int i = 0; i < response.getJSONArray("content").length(); i++) {
            list.add(JsonWrapper.articleFromJson(response.getJSONArray("content").getJSONObject(i).toString()));
        }

        return list;
    }

    /**
     * Set the article as read on the server
     *
     * @param article the article to set as read
     * @throws RequiredInfoNotRegistered if the requiered info (login, password, url, etc) are not registred
     * @throws CheckException           if the json response is not correct
     * @throws JSONException            if there is a probleme with json parsing
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NoInternetException      if the is no internet connexion right now
     */
    public void setArticleToRead(final Article article) throws CheckException, ExecutionException, InterruptedException, JSONException, RequiredInfoNotRegistered, NoInternetException {
        Log.d(TAG, String.format("Article %s set to read", article.getId()));
        HttpUtils.checkNetwork(this.connMgr);

        String session = login();
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("sid", session);
        jsonObject.put("op", "updateArticle");
        jsonObject.put("article_ids", article.getId());
        jsonObject.put("mode", "0");
        jsonObject.put("field", "2");

        RequestTask task = new RequestTask(preferences);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
        logout(session);
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
                    case SSL_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(getString(R.string.ssl_exception_message));
                    case HTTP_AUTH_REQUIRED:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(getString(R.string.connectionAuthError));
                    case UNSUPPORTED_ENCODING:
                    case JSON_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(getString(R.string.impossibleError), response.getJSONObject("content").getString("message")));
                    case API_DISABLED:
                        Log.e(TAG, "API disabled...");
                        throw new CheckException(getString(R.string.setupApiDisabled));
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
        if (json == null) {
            Log.d(TAG, "List is empty, not saving it");
            return;
        }

        Log.d(TAG, String.format("Saving the list to %s", this.lastListFile.getAbsolutePath()));
        try {
            FileOutputStream outputStream = new FileOutputStream(this.lastListFile);
            outputStream.write(json.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error while saving the last articles list", e);
        }

    }

    /**
     * Check if all the requiered information has been filled
     *
     * @throws RequiredInfoNotRegistered if it is not the case
     */
    private void checkRequieredInfoRegistred() throws RequiredInfoNotRegistered {
        if (!preferences.getBoolean(TinyTinyFeedWidget.CHECKED, false)) {
            throw new RequiredInfoNotRegistered();
        }
    }

    public class LocalBinder extends Binder {
        WidgetService getService() {
            return WidgetService.this;
        }
    }

}

