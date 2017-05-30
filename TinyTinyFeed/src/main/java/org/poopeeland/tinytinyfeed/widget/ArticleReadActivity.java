package org.poopeeland.tinytinyfeed.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.model.Article;
import org.poopeeland.tinytinyfeed.utils.FetchException;
import org.poopeeland.tinytinyfeed.utils.Fetcher;


/**
 * Invisible Activity that set the {@link Article} as read
 * Created by eric on 11/06/14.
 */
public class ArticleReadActivity extends Activity {

    private static final String TAG = "ArticleReadActivity";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        Log.d(TAG, "Creation");


        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        Log.d(TAG, "Widget Id " + widgetId + " launched the intent");

        // Retrieve the article
        Article article = (Article) intent.getExtras().getSerializable("article");

        AsyncSetRead async = new AsyncSetRead(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()), this);
        async.execute(article);

        // Update all the widget. Maybe we should update only the widget which requires
        // the activity?
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TinyTinyFeedWidget.class));
        Intent updateIntent = new Intent(ArticleReadActivity.this, TinyTinyFeedWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(updateIntent);

        if (article != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(article.getLink())));
        } else {
            Toast.makeText(getApplicationContext(), R.string.nullArticle, Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private class AsyncSetRead extends AsyncTask<Article, Void, Void> {

        private final SharedPreferences preferences;
        private final Context context;

        public AsyncSetRead(final SharedPreferences preferences, final Context context) {
            this.preferences = preferences;
            this.context = context;
        }

        @Override
        protected Void doInBackground(final Article... params) {
            try {
                Fetcher fetcher = new Fetcher(this.preferences, context);
                for (Article article : params) {
                    fetcher.setArticleToRead(article);
                }
            } catch (FetchException ex) {
                Log.e(TAG, "Can't update article!", ex);
            }
            return null;
        }

    }

}
