package org.poopeeland.tinytinyfeed.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViewsService;

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
import org.poopeeland.tinytinyfeed.utils.MySSLSocketFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
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
    private final IBinder binder = new LocalBinder();
    private ConnectivityManager connMgr;
    private String session;
    private String url;
    private String password;
    private String user;
    private String numArticles;
    private boolean onlyUnread;
    private HttpClient client;
    private File lastListFile;
    private ListProvider listProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        this.lastListFile = new File(getApplicationContext().getFilesDir(), WidgetService.LIST_FILENAME);
        this.listProvider = new ListProvider(this);
        this.connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        SharedPreferences preferences = getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.url = preferences.getString(TinyTinyFeedWidget.URL_KEY, "");
        this.user = preferences.getString(TinyTinyFeedWidget.USER_KEY, "");
        this.password = preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, "");
        this.numArticles = preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "");
        this.onlyUnread = preferences.getBoolean(TinyTinyFeedWidget.ONLY_UNREAD_KEY, false);
        this.session = preferences.getString(TinyTinyFeedWidget.SESSION_KEY, null);
        String httpUser = preferences.getString(TinyTinyFeedWidget.HTTP_USER_KEY, "");
        String httpPassword = preferences.getString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, "");
        this.client = this.getNewHttpClient(httpUser, httpPassword);
        Log.d(TAG, "Preferences loaded");
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return this.listProvider;
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
        saveSessionId(this.session);
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
        this.checkNetwork();
        List<Article> list = new ArrayList<>();
        if (isNotLogged()) {
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
        this.checkNetwork();
        Log.d(TAG, String.format("Article %s set to read", article.getTitle()));
        if (isNotLogged()) {
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
     *
     * @return a List of Category
     * @throws RequiredInfoNotRegistred if the requiered info (login, password, url, etc) are not registred
     * @throws CheckException           if the json response is not correct
     * @throws JSONException            if there is a probleme with json parsing
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NoInternetException      if the is no internet connexion right now
     */
    public List<Category> loadCategories() throws InterruptedException, ExecutionException, CheckException, JSONException, RequiredInfoNotRegistred, NoInternetException {
        if (isNotLogged()) {
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

    /**
     * Try to subscribe to a feed
     *
     * @param url      the url of the feed
     * @param category the caterogy for the
     * @return From TTrss source code: <ul>
     * <li>-1 - Unknown error</li>
     * <li>0 - OK, Feed already exists</li>
     * <li>1 - OK, Feed added</li>
     * <li>2 - Invalid URL</li>
     * <li>3 - URL content is HTML, no feeds available</li>
     * <li>4 - URL content is HTML which contains multiple feeds.
     * Here you should call extractfeedurls in rpc-backend
     * to get all possible feeds.</li>
     * <li>5 - Couldn't download the URL content.</li>
     * </ul>
     * TODO: refactor this shit
     */
    public int subscribe(String url, Category category) {

        try {
            if (isNotLogged()) {
                login();
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("op", "subscribeToFeed");
            jsonObject.put("feed_url", url);
            jsonObject.put("category_id", category.getId());
            jsonObject.put("sid", session);
            RequestTask task = new RequestTask(this.client, this.url);
            task.execute(jsonObject);
            JSONObject response = task.get();
            checkJsonResponse(response);
            return response.getJSONObject("content").getJSONObject("status").getInt("code");
        } catch (InterruptedException | ExecutionException | JSONException | CheckException | RequiredInfoNotRegistred ex) {
            Log.e(TAG, String.format("Error while subscribing to a stream: %s", ex.getMessage()));
            return -1;
        }
    }


    private boolean isNotLogged() throws RequiredInfoNotRegistred, JSONException, ExecutionException, InterruptedException, CheckException {
        checkRequieredInfoRegistred();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", session);
        jsonObject.put("op", "isLoggedIn");

        RequestTask task = new RequestTask(this.client, this.url);
        task.execute(jsonObject);
        JSONObject response = task.get();
        checkJsonResponse(response);
        return !response.getJSONObject("content").getBoolean("status");
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
            Log.e(TAG, String.format("Error while saving the last articles list: %s", e.getLocalizedMessage()));
        }

    }

    /**
     * Check if all the requiered information has been filled
     *
     * @throws RequiredInfoNotRegistred if it is not the case
     */
    private void checkRequieredInfoRegistred() throws RequiredInfoNotRegistred {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        if (!preferences.getBoolean(TinyTinyFeedWidget.CHECKED, false)) {
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


    private void saveSessionId(String sessionId) {
        Log.d(TAG, String.format("Saving the sessions id %s", sessionId));
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TinyTinyFeedWidget.SESSION_KEY, sessionId);
        editor.apply();
    }

    private HttpClient getNewHttpClient(String httpUser, String httpPassword) {
        DefaultHttpClient client;
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

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

    public class LocalBinder extends Binder {
        public WidgetService getService() {
            return WidgetService.this;
        }
    }

}
