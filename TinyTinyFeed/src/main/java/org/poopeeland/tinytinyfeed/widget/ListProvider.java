package org.poopeeland.tinytinyfeed.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.Article;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.RequestTask;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.ArticleNotUpdatedException;
import org.poopeeland.tinytinyfeed.exceptions.NoDataException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Created by eric on 11/05/14.
 */
public class ListProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "ListProvider";
    private List<Article> articleList = new ArrayList<Article>();
    private final Context context;
    private final int appWidgetId;
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

    private void populateListItem() {
        Log.d(TAG, "Refresh the articles list");
        try {
            articleList = updateFeeds();
        } catch (NoDataException e) {
            e.printStackTrace();
        } catch (RequiredInfoNotRegistred requiredInfoNotRegistred) {
            requiredInfoNotRegistred.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        start();
    }

    @Override
    public void onDataSetChanged() {
        this.articleList.clear();
        populateListItem();
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

    private void login() throws RequiredInfoNotRegistred {

        checkRequieredInfoRegistred();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user", user);
            jsonObject.put("password", password);
            jsonObject.put("op", "login");

            RequestTask task = new RequestTask(this.client, this.url);
            task.execute(jsonObject);
            JSONObject response = task.get();

            this.session = response.getJSONObject("content").getString("session_id");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private List<Article> updateFeeds() throws NoDataException, RequiredInfoNotRegistred {
        List<Article> list = new ArrayList<Article>();
        if (checkNetwork()) {
            if (!isLogged()) {
                login();
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("sid", session);
                jsonObject.put("op", "getHeadlines");
                jsonObject.put("feed_id", "-4");
                jsonObject.put("limit", this.numArticles);
                jsonObject.put("show_excerpt", "true");

                RequestTask task = new RequestTask(this.client, this.url);
                task.execute(jsonObject);
                JSONObject response = task.get();
                for (int i = 0; i < response.getJSONArray("content").length(); i++) {
                    list.add(new Article(response.getJSONArray("content").getJSONObject(i)));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            throw new NoDataException();
        }

        return list;
    }


    private boolean isLogged() throws RequiredInfoNotRegistred {

        checkRequieredInfoRegistred();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sid", session);
            jsonObject.put("op", "isLoggedIn");

            RequestTask task = new RequestTask(this.client, this.url);
            task.execute(jsonObject);
            JSONObject response = task.get();
            return response.getJSONObject("content").getBoolean("status");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void setArticleToSeenState(Article article) throws ArticleNotUpdatedException {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sid", session);
            jsonObject.put("op", "updateArticle");
            jsonObject.put("article_ids", article.getId());
            jsonObject.put("mode", "0");
            jsonObject.put("field", "2");

            RequestTask task = new RequestTask(this.client, this.url);
            task.execute(jsonObject);
        } catch (JSONException e) {
            throw new ArticleNotUpdatedException();
        }
    }


    private void checkRequieredInfoRegistred() throws RequiredInfoNotRegistred {
        SharedPreferences preferences = context.getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (!(preferences.contains(TinyTinyFeedWidget.URL_KEY) &&
                preferences.contains(TinyTinyFeedWidget.USER_KEY) &&
                preferences.contains(TinyTinyFeedWidget.PASSWORD_KEY) &&
                preferences.contains(TinyTinyFeedWidget.NUM_ARTICLE_KEY))) {
            throw new RequiredInfoNotRegistred();
        }
    }

    private boolean checkNetwork() {
        NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }


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