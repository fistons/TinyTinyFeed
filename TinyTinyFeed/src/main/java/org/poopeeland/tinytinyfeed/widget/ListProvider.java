package org.poopeeland.tinytinyfeed.widget;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONException;
import org.poopeeland.tinytinyfeed.Article;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * ListProvider
 * Created by eric on 11/05/14.
 */
public class ListProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = ListProvider.class.getSimpleName();
    private final Context context;
    private final String unreadSymbol;
    private WidgetService service;
    private List<Article> articleList;

    public ListProvider(WidgetService service) {
        this.service = service;
        this.context = service.getApplicationContext();
        this.unreadSymbol = context.getString(R.string.unreadSymbol);
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        this.articleList = new ArrayList<>();
    }

    @Override
    public void onDataSetChanged() {
        try {
            Log.d(TAG, "Refresh the articles list");
            articleList = service.updateFeeds();
        } catch (RequiredInfoNotRegistred ex) {
            Log.e(TAG, "Some informations are missing");
        } catch (CheckException e) {
            Log.e(TAG, e.getMessage());
        } catch (InterruptedException | ExecutionException | JSONException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (NoInternetException ex) {
            Log.e(TAG, context.getText(R.string.noInternetConnection).toString());
        }
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

        final RemoteViews remoteView;
        Intent fillInIntent = new Intent();
        fillInIntent.setData(Uri.parse(listItem.getUrl()));
        fillInIntent.putExtra("article", listItem);

        // TODO: should use a theme or a style or something like that
        String feedNameAndDate = String.format("%s - %s", listItem.getFeeTitle(), listItem.getDate());
        if (listItem.isRead()) {
            remoteView = new RemoteViews(context.getPackageName(), R.layout.read_article_layout);
            remoteView.setTextViewText(R.id.readTitle, listItem.getTitle());
            remoteView.setTextViewText(R.id.readFeedNameAndDate, feedNameAndDate);
            remoteView.setTextViewText(R.id.readResume, listItem.getContent());
            remoteView.setOnClickFillInIntent(R.id.readArticleLayout, fillInIntent);
        } else {
            remoteView = new RemoteViews(context.getPackageName(), R.layout.article_layout);
            remoteView.setTextViewText(R.id.title, String.format("%s %s", unreadSymbol, listItem.getTitle()));
            remoteView.setTextViewText(R.id.feedNameAndDate, feedNameAndDate);
            remoteView.setTextViewText(R.id.resume, listItem.getContent());
            remoteView.setOnClickFillInIntent(R.id.articleLayout, fillInIntent);
        }
        return remoteView;
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


}