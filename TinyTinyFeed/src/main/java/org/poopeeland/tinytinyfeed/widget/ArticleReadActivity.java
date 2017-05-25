package org.poopeeland.tinytinyfeed.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistered;
import org.poopeeland.tinytinyfeed.model.Article;
import org.poopeeland.tinytinyfeed.utils.HttpUtils;

import java.util.concurrent.ExecutionException;


/**
 * Invisible Activity that set the {@link Article} as read
 * Created by eric on 11/06/14.
 */
public class ArticleReadActivity extends Activity {

    private static final String TAG = "ArticleReadActivity";
    private boolean bound;
    private Article article;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            WidgetService.LocalBinder mbinder = (WidgetService.LocalBinder) binder;
            WidgetService service = mbinder.getService();
            bound = true;
            Log.d(TAG, "Bounded");
            try {
                service.setArticleToRead(article);
            } catch (CheckException | InterruptedException | ExecutionException | JSONException | RequiredInfoNotRegistered e) {
                Log.e(TAG, "Error while trying to set the article to read", e);
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            } catch (NoInternetException e) {
                Log.e(TAG, "Internet unavailable", e);
                Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_LONG).show();
                return;
            }

            // Retrieve the widgets Ids
            int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TinyTinyFeedWidget.class));
            Intent updateIntent = new Intent(ArticleReadActivity.this, TinyTinyFeedWidget.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(updateIntent);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creation");

        try {
            HttpUtils.checkNetwork((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE));
        } catch (NoInternetException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_LONG).show();
            finish();
        }

        // Bound to service
        Intent intentBound = new Intent(this, WidgetService.class);
        intentBound.putExtra(WidgetService.ACTIVITY_FLAG, true);
        bindService(intentBound, mConnection, Context.BIND_AUTO_CREATE);

        // Retrieve the article
        this.article = (Article) getIntent().getExtras().getSerializable("article");
        if (article != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getLink()));
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), R.string.nullArticle, Toast.LENGTH_SHORT).show();
        }

        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            Log.d(TAG, "Unbound");
            unbindService(mConnection);
            bound = false;
        }
    }
}
