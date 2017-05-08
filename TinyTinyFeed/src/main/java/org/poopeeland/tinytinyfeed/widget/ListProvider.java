package org.poopeeland.tinytinyfeed.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONException;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;
import org.poopeeland.tinytinyfeed.model.Article;
import org.poopeeland.tinytinyfeed.model.ArticleWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * ListProvider
 * Created by eric on 11/05/14.
 */
class ListProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = ListProvider.class.getSimpleName();
    private final Context context;
    private final String unreadSymbol;
    private final WidgetService service;
    private final File lastArticlesList;
    private final SharedPreferences pref;
    private List<Article> articleList;

    ListProvider(WidgetService service) {
        this.service = service;
        this.context = service.getApplicationContext();
        this.unreadSymbol = context.getString(R.string.unreadSymbol);
        this.lastArticlesList = new File(context.getApplicationContext().getFilesDir(), WidgetService.LIST_FILENAME);
        this.pref = PreferenceManager.getDefaultSharedPreferences(context);
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        this.articleList = this.loadLastList();
    }


    @Override
    public void onDataSetChanged() {
        ComponentName cn = new ComponentName(context, TinyTinyFeedWidget.class);
        RemoteViews rvs = new RemoteViews(context.getPackageName(), R.layout.tiny_tiny_feed_widget);

        CharSequence updatingText = context.getText(R.string.widget_update_text);
        rvs.setTextViewText(R.id.lastUpdateText, updatingText);
        AppWidgetManager.getInstance(context).updateAppWidget(cn, rvs);

        try {
            Log.d(TAG, "Refresh the articles list");
            articleList = service.updateFeeds();
        } catch (RequiredInfoNotRegistred ex) {
            Log.e(TAG, "Some informations are missing");
            this.articleList = this.loadLastList();
        } catch (CheckException e) {
            Log.e(TAG, e.getMessage());
            this.articleList = this.loadLastList();
        } catch (InterruptedException | ExecutionException | JSONException e) {
            this.articleList = this.loadLastList();
            Log.e(TAG, e.getLocalizedMessage());
        } catch (NoInternetException ex) {
            Log.e(TAG, context.getText(R.string.noInternetConnection).toString());
            this.articleList = this.loadLastList();
        }

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String dateStr = dateFormat.format(new Date());
        CharSequence text = context.getText(R.string.lastUpdateText);
        rvs.setTextViewText(R.id.lastUpdateText, String.format(text.toString(), dateStr));
        AppWidgetManager.getInstance(context).updateAppWidget(cn, rvs);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        this.articleList.clear();
    }

    @Override
    public int getCount() {
        return articleList.size();
    }


    @Override
    public RemoteViews getViewAt(int position) {
        Article listItem = articleList.get(position);
        int color = pref.getInt(TinyTinyFeedWidget.TEXT_COLOR_KEY, 0xffffffff);

        final RemoteViews rv;
        Intent fillInIntent = new Intent();
        fillInIntent.setData(Uri.parse(listItem.getLink()));
        fillInIntent.putExtra("article", listItem);
        String feedNameAndDate = String.format("%s - %s", listItem.getFeedTitle(), listItem.getDate());
        if (!listItem.isUnread()) {
            rv = new RemoteViews(context.getPackageName(), R.layout.read_article_layout);
            rv.setTextViewText(R.id.readTitle, listItem.getTitle());
            rv.setInt(R.id.readTitle, "setTextColor", color);
            rv.setTextViewText(R.id.readFeedNameAndDate, feedNameAndDate);
            rv.setInt(R.id.readFeedNameAndDate, "setTextColor", color);
            rv.setTextViewText(R.id.readResume, listItem.getExcerpt());
            rv.setInt(R.id.readResume, "setTextColor", color);
            rv.setOnClickFillInIntent(R.id.readArticleLayout, fillInIntent);
        } else {
            rv = new RemoteViews(context.getPackageName(), R.layout.article_layout);
            if (this.pref.getBoolean(TinyTinyFeedWidget.ONLY_UNREAD_KEY, false)) {
                rv.setTextViewText(R.id.title, listItem.getTitle());
            } else {
                rv.setTextViewText(R.id.title, String.format("%s %s", unreadSymbol, listItem.getTitle()));
            }
            rv.setInt(R.id.title, "setTextColor", color);
            rv.setTextViewText(R.id.feedNameAndDate, feedNameAndDate);
            rv.setInt(R.id.feedNameAndDate, "setTextColor", color);
            rv.setTextViewText(R.id.resume, listItem.getExcerpt());
            rv.setInt(R.id.resume, "setTextColor", color);
            rv.setOnClickFillInIntent(R.id.articleLayout, fillInIntent);
        }
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    private List<Article> loadLastList() {
        Log.d(TAG, String.format("Loading lastlist from %s", this.lastArticlesList.getAbsolutePath()));
        if (!this.lastArticlesList.isFile()) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader fis = new BufferedReader(new FileReader(this.lastArticlesList));
            String buffer;
            while ((buffer = fis.readLine()) != null) {
                sb.append(buffer);
            }
            fis.readLine();
            fis.close();
        } catch (IOException ex) {
            Log.wtf(TAG, "Error while reading the last article list", ex);
        }

        if (sb.toString().isEmpty()) {
            return Collections.emptyList();
        }

        List<Article> articles = new ArrayList<>();
        try {
            JSONArray response = new JSONArray(sb.toString());
            for (int i = 0; i < response.length(); i++) {
                articles.add(ArticleWrapper.fromJson(response.getJSONObject(i).toString()));
            }
        } catch (JSONException ex) {
            return Collections.emptyList();
        }
        return articles;
    }


}