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
import org.poopeeland.tinytinyfeed.Category;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.RequestTask;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Central service managing the connections to the server
 * <p/>
 * Created by eric on 11/05/14.
 */
public class WidgetService extends RemoteViewsService {

    public static final String ACTIVITY_FLAG = "Activity";
    private static final String TAG = WidgetService.class.getSimpleName();
    private final String listFileName = "listArticles.json";
    private final String listCatName = "listCat.json";
    protected IBinder binder = new LocalBinder();
    private ConnectivityManager connMgr;
    private String session;
    private String url;
    private String password;
    private String user;
    private String numArticles;
    private boolean onlyUnread;
    private DefaultHttpClient client;
    private File lastListFile;
    private File catListFile;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        this.lastListFile = new File(getApplicationContext().getFilesDir(), this.listFileName);
        this.catListFile = new File(getApplicationContext().getFilesDir(), this.listCatName);
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new ListProvider(this));
    }

    @Override
    public IBinder onBind(Intent intent) {
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
     * @throws org.poopeeland.tinytinyfeed.exceptions.CheckException           When something wrong happened with the server
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

    /**
     * Refresh the feeds et return the list of articles
     *
     * @return the list of last articles
     * @throws RequiredInfoNotRegistred if the requiered info (login, password, url, etc) are not registred
     * @throws CheckException           if the json response is not correct
     * @throws JSONException            if there is a probleme with json parsing
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NoInternetException      if the is no internet connexion right now
     */
    public List<Article> updateFeeds() throws RequiredInfoNotRegistred, CheckException, JSONException, ExecutionException, InterruptedException, NoInternetException {
        this.start();
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
        this.saveList(response.getJSONArray("content"));

        for (int i = 0; i < response.getJSONArray("content").length(); i++) {
            list.add(new Article(response.getJSONArray("content").getJSONObject(i)));
        }


        return list;
    }

    /**
     * Set the article as read on the server
     *
     * @param article the article to set as read
     * @throws RequiredInfoNotRegistred if the requiered info (login, password, url, etc) are not registred
     * @throws CheckException           if the json response is not correct
     * @throws JSONException            if there is a probleme with json parsing
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NoInternetException      if the is no internet connexion right now
     */
    public void setArticleToRead(Article article) throws CheckException, ExecutionException, InterruptedException, JSONException, RequiredInfoNotRegistred, NoInternetException {
        this.start();
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

    /**
     * Load all the category from TTRss Server
     * @return a List of Category
     * @throws RequiredInfoNotRegistred if the requiered info (login, password, url, etc) are not registred
     * @throws CheckException           if the json response is not correct
     * @throws JSONException            if there is a probleme with json parsing
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NoInternetException      if the is no internet connexion right now
     */
    public List<Category> loadCategories() throws InterruptedException, ExecutionException, CheckException, JSONException, RequiredInfoNotRegistred, NoInternetException {
        this.start();
        if (!isLogged()) {
            login();
        }
        this.checkNetwork();
        List<Category> categories = new ArrayList<>();
        Log.d(TAG, "Retrieve the list of category");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", session);
        jsonObject.put("op", "getCategories");
        jsonObject.put("unread_only", false);
        jsonObject.put("enable_nested", false);
        jsonObject.put("include_empty", true);

        RequestTask task = new RequestTask(this.client, this.url);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
        Log.d(TAG, response.toString());

        for (int i = 0; i < response.getJSONArray("content").length(); i++) {
            categories.add(new Category(response.getJSONArray("content").getJSONObject(i)));
        }

        return categories;
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
        if (json == null) {
            Log.d(TAG, "List is empty, not saving it");
            return;
        }

        Log.d(TAG, "Saving the list");
        try {
            FileOutputStream outputStream = new FileOutputStream(this.lastListFile);
            outputStream.write(json.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.d(TAG, String.format("Error while saving the last articles list: %s", e.getLocalizedMessage()));
        }

    }

    /**
     * Load the last saved list from the preference in a file
     *
     * @return the last list of articles
     * @throws JSONException
     */
    private List<Article> loadLastList() throws JSONException {
        if (!this.lastListFile.isFile()) {
            return Collections.EMPTY_LIST;
        }


        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader fis = new BufferedReader(new FileReader(this.lastListFile));
            String buffer;
            while ((buffer = fis.readLine()) != null) {
                sb.append(buffer);
            }
            fis.readLine();
            fis.close();
        } catch (IOException ex) {
            Log.d(TAG, String.format("Error while reading the last article list: %s", ex.getLocalizedMessage()));
        }

        if (sb.toString().isEmpty()) {
            return Collections.EMPTY_LIST;
        }


        JSONArray response = new JSONArray(sb.toString());
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
     */
    private void checkNetwork() throws NoInternetException {
        NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new NoInternetException();
        }
    }

    /**
     * Prepare the service
     * TODO: Ugly, need to change that
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
