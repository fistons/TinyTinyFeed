package org.poopeeland.tinytinyfeed.adapters;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.models.Article;
import org.poopeeland.tinytinyfeed.network.Fetcher;
import org.poopeeland.tinytinyfeed.network.exceptions.FetchException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget.*;


/**
 * ListProvider
 * Created by eric on 11/05/14.
 */
public class ListProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = ListProvider.class.getSimpleName();
    private final Context context;
    private final String unreadSymbol;
    private final File lastArticlesList;
    private final SharedPreferences pref;
    private final int widgetId;
    private List<Article> articleList;

    public ListProvider(final Context context, final int widgetId) {
        this.context = context;
        this.unreadSymbol = this.context.getString(R.string.unreadSymbol);
        this.lastArticlesList = new File(this.context.getApplicationContext().getFilesDir()
                , String.format(Locale.getDefault(), JSON_STORAGE_FILENAME_TEMPLATE, widgetId));
        this.pref = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.widgetId = widgetId;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        this.articleList = this.loadLastList();
    }

    @Override
    public void onDataSetChanged() {
        RemoteViews rvs = new RemoteViews(context.getPackageName(), R.layout.tiny_tiny_feed_widget);

        CharSequence updatingText = context.getText(R.string.widget_update_text);
        rvs.setTextViewText(R.id.lastUpdateText, updatingText);
        AppWidgetManager.getInstance(context).updateAppWidget(this.widgetId, rvs);

        try {
            Log.d(TAG, "Refresh the articles list");
            Set<String> categories = pref.getStringSet(String.format(Locale.getDefault(), WIDGET_CATEGORIES_KEY, this.widgetId), Collections.emptySet());
            this.articleList = new Fetcher(this.pref, context).fetchArticles(this.widgetId, categories);
        } catch (FetchException ex) {
            Log.e(TAG, "Error while fetching data: " + ex.getMessage(), ex);
            this.articleList = this.loadLastList();
        }

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String dateStr = dateFormat.format(new Date());
        CharSequence text = context.getText(R.string.lastUpdateText);
        rvs.setTextViewText(R.id.lastUpdateText, String.format(text.toString(), dateStr));
        AppWidgetManager.getInstance(context).updateAppWidget(this.widgetId, rvs);
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
    public RemoteViews getViewAt(final int position) {
        Log.d(TAG, "Getting article at position " + position);
        final Article article = articleList.get(position);
        final int textColor = pref.getInt(String.format(Locale.getDefault(), TEXT_COLOR_KEY, widgetId), DEFAULT_TEXT_COLOR);
        final int sourceColor = pref.getInt(String.format(Locale.getDefault(), SOURCE_COLOR_KEY, widgetId), DEFAULT_TEXT_COLOR);
        final int titleColor = pref.getInt(String.format(Locale.getDefault(), TITLE_COLOR_KEY, widgetId), DEFAULT_TEXT_COLOR);
        final float textSize = Float.parseFloat(pref.getString(String.format(Locale.getDefault(), TEXT_SIZE_KEY, widgetId), DEFAULT_TEXT_SIZE));
        final float sourceSize = Float.parseFloat(pref.getString(String.format(Locale.getDefault(), SOURCE_SIZE_KEY, widgetId), DEFAULT_TEXT_SIZE));
        final float titleSize = Float.parseFloat(pref.getString(String.format(Locale.getDefault(), TITLE_SIZE_KEY, widgetId), DEFAULT_TEXT_SIZE));

        final Intent fillInIntent = new Intent();
        fillInIntent.setData(Uri.parse(article.getLink()));
        fillInIntent.putExtra("article", article);
        fillInIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.widgetId);
        final String feedNameAndDate = String.format("%s - %s", article.getFeedTitle(), article.getDate());
        final int layout = article.isUnread() ? R.layout.article_layout : R.layout.read_article_layout;
        final int layoutId = article.isUnread() ? R.id.articleLayout : R.id.readArticleLayout;

        final RemoteViews rv = new RemoteViews(context.getPackageName(), layout);
        rv.setTextViewText(R.id.title, getTitle(article));
        rv.setInt(R.id.title, "setTextColor", titleColor);
        rv.setTextViewText(R.id.feedNameAndDate, feedNameAndDate);
        rv.setInt(R.id.feedNameAndDate, "setTextColor", sourceColor);
        rv.setTextViewText(R.id.resume, article.getExcerpt());
        rv.setInt(R.id.resume, "setTextColor", textColor);
        rv.setFloat(R.id.resume, "setTextSize", textSize);
        rv.setFloat(R.id.title, "setTextSize", titleSize);
        rv.setFloat(R.id.feedNameAndDate, "setTextSize", sourceSize);
        rv.setOnClickFillInIntent(layoutId, fillInIntent);

        return rv;
    }

    private String getTitle(final Article article) {
        return this.pref.getBoolean(String.format(Locale.getDefault(), ONLY_UNREAD_KEY, widgetId), false)
                || !article.isUnread() ? article.getTitle() : String.format("%s %s", unreadSymbol, article.getTitle());
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
    public long getItemId(final int position) {
        return articleList.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    @SuppressWarnings("unchecked")
    private List<Article> loadLastList() {
        Log.d(TAG, String.format("Loading last list from %s", this.lastArticlesList.getAbsolutePath()));
        if (!this.lastArticlesList.isFile()) {
            return Collections.emptyList();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(this.lastArticlesList))) {
            return (List<Article>) inputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Log.wtf(TAG, "Error while reading the last article list", ex);
            return Collections.emptyList();
        }
    }

}
