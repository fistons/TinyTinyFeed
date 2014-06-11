package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;


/**
 * Created by eric on 11/06/14.
 */
public class ArticleManagementActivity extends Activity {

    private static final String TAG = "ArticleManagementActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creation");

        // Retrieve the article
        Article article = (Article) getIntent().getExtras().getSerializable("article");

        // Open it
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
        startActivity(intent);

        // Set it to read

        // Retrieve the widgets Ids
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TinyTinyFeedWidget.class));
        Intent updateIntent = new Intent(this, TinyTinyFeedWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(updateIntent);


        finish();
    }

    private void setArticleToRead(Article article) {
        Log.d(TAG, String.format("%s [%d] read", article.getTitle(), article.getId()));
        // How?

    }

}
