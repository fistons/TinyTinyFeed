package org.poopeeland.tinytinyfeed.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.Article;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.RequestTask;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Created by eric on 11/05/14.
 */
public class ListProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "ListProvider";
    private final Context context;
    private final int appWidgetId;
    private List<Article> articleList = new ArrayList();
    private ConnectivityManager connMgr;
    private String session;
    private String url;
    private String password;
    private String user;
    private String numArticles;
    private HttpClient client;

    public ListProvider(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        start();
    }

    @Override
    public void onDataSetChanged() {
        if (checkNetwork()) {
            try {
                Log.d(TAG, "Refresh the articles list");
                List<Article> tempList = this.updateFeeds();
                articleList.clear();
                articleList = tempList;
            } catch (RequiredInfoNotRegistred ex) {
                Log.e(TAG, "Some informations are missing");
            } catch (CheckException e) {
                Log.e(TAG, e.getMessage());
            } catch (InterruptedException | ExecutionException | JSONException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        } else {
            Log.w(TAG, "No Internet right now");
        }
    }

    @Override
    public void onDestroy() {
        this.articleList.clear();
    }

    @Override
    public int getCount() {
        return articleList.size();
    }


    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.article_layout);
        Article listItem = articleList.get(position);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(listItem.getUrl()));

        String feedNameAndDate = String.format("%s - %s", listItem.getFeeTitle(), listItem.getDate());
        remoteView.setTextViewText(R.id.title, listItem.getTitle());
        remoteView.setTextViewText(R.id.feedNameAndDate, feedNameAndDate);
        remoteView.setTextViewText(R.id.resume, listItem.getContent());

        Intent fillInIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(listItem.getUrl()));
        remoteView.setOnClickFillInIntent(R.id.articleLayout, fillInIntent);

        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * Log into the TTRss server
     *
     * @throws RequiredInfoNotRegistred if some required information has not been registred
     * @throws JSONException            (should not happen)
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws CheckException           When something wring happened with the server
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

    private List<Article> updateFeeds() throws RequiredInfoNotRegistred, CheckException, JSONException, ExecutionException, InterruptedException {
        List<Article> list = new ArrayList();

        if (!isLogged()) {
            login();
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sid", session);
        jsonObject.put("op", "getHeadlines");
        jsonObject.put("feed_id", "-4");
        jsonObject.put("limit", this.numArticles);
        jsonObject.put("show_excerpt", "true");

        RequestTask task = new RequestTask(this.client, this.url);
        task.execute(jsonObject);
        JSONObject response = task.get();

        checkJsonResponse(response);

        for (int i = 0; i < response.getJSONArray("content").length(); i++) {
            list.add(new Article(response.getJSONArray("content").getJSONObject(i)));
        }


        return list;
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
                        throw new CheckException(context.getText(R.string.badLogin).toString());
                    case CLIENT_PROTOCOL_EXCEPTION:
                    case UNREACHABLE_TTRSS:
                    case IO_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(context.getString(R.string.connectionError));
                    case UNSUPPORTED_ENCODING:
                    case JSON_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(context.getString(R.string.impossibleError), response.getJSONObject("content").getString("message")));
                    default:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(context.getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
                }
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, response.getJSONObject("content").getString("message"));
                throw new CheckException(String.format(context.getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
            }
        }
    }

    /**
     * Check if all the requiered information has been filled
     *
     * @throws RequiredInfoNotRegistred if it is not the case
     */
    private void checkRequieredInfoRegistred() throws RequiredInfoNotRegistred {
        SharedPreferences preferences = context.getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE);
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
    private boolean checkNetwork() {
        NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Prepare the provider
     */
    private void start() {
        this.connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.client = new DefaultHttpClient();

        SharedPreferences preferences = context.getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE);
        this.url = preferences.getString(TinyTinyFeedWidget.URL_KEY, "");
        this.user = preferences.getString(TinyTinyFeedWidget.USER_KEY, "");
        this.password = preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, "");
        this.numArticles = preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "");
    }


}