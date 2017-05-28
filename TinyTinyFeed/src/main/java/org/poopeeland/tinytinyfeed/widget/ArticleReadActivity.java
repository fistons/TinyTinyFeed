package org.poopeeland.tinytinyfeed.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
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
        try {
            new Fetcher(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()), this).setArticleToRead(article);
        } catch (FetchException ex) {
            Log.e(TAG, "Can't update article!", ex);
        }

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


}
