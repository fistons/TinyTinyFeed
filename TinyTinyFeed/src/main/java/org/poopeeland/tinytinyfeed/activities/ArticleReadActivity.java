package org.poopeeland.tinytinyfeed.activities;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.poopeeland.tinytinyfeed.utils.ExceptionAsyncTask;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.models.Article;
import org.poopeeland.tinytinyfeed.network.exceptions.FetchException;
import org.poopeeland.tinytinyfeed.network.Fetcher;


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
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        Log.d(TAG, "Widget Id " + widgetId + " launched the intent");

        // Retrieve the article
        final Article article = (Article) intent.getExtras().getSerializable("article");
        if (article == null) {
            Log.e(TAG, "Article is null");
            Toast.makeText(getApplicationContext(), R.string.nullArticle, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (article.isUnread()) {
            AsyncSetRead async = new AsyncSetRead(PreferenceManager.getDefaultSharedPreferences(this),
                    this,
                    widgetId);
            async.execute(article);
        }

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(article.getLink())));
        finish();
    }

    private class AsyncSetRead extends ExceptionAsyncTask<Article, Void, Void> {

        private final SharedPreferences preferences;
        private final Context context;
        private final int emitterWidgetId;

        private AsyncSetRead(final SharedPreferences preferences,
                            final Context context,
                            final int emitterWidgetId) {
            super(context);
            this.preferences = preferences;
            this.context = context;
            this.emitterWidgetId = emitterWidgetId;
        }

        @Override
        protected Void doInBackground() throws FetchException {
            Fetcher fetcher = new Fetcher(this.preferences, context);
            for (Article article : getParams()) {
                fetcher.setArticleToRead(article);
            }
            return null;
        }

        @Override
        protected void onSafePostExecute(final Void aVoid) {

            if (onError()) {
                return;
            }

            Log.d(TAG, "Emitter widget id: " + emitterWidgetId);
            // Update all the widget. Maybe we should update only the widget which requires
            // the activity?
            int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TinyTinyFeedWidget.class));
            Intent updateIntent = new Intent(ArticleReadActivity.this, TinyTinyFeedWidget.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(updateIntent);
        }

    }

}
