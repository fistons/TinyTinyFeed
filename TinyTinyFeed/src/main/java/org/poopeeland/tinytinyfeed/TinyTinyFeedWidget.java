package org.poopeeland.tinytinyfeed;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import org.poopeeland.tinytinyfeed.widget.WidgetService;

import java.text.DateFormat;
import java.util.Date;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link org.poopeeland.tinytinyfeed.SetupActivity TinyTinyFeedWidgetConfigureActivity}
 */
public class TinyTinyFeedWidget extends AppWidgetProvider {

    public static final String PREFERENCE_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_KEY";
    public static final String URL_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_URL";
    public static final String USER_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_USER";
    public static final String PASSWORD_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_PASSWORD";
    public static final String NUM_ARTICLE_KEY = "org.poopeeland.tinytinyfeed.NUM_ARTICLE_KEY";
    private static final String TAG = "TinyTinyFeedWidget";

    /**
     * Return a Pending Intent asking the refresh of the widget
     * @param context
     * @param ids
     * @return
     */
    private static PendingIntent actionPendingIntent(Context context, int[] ids) {
        Log.d(TAG, "Create pending intent");
        Intent intent = new Intent(context, TinyTinyFeedWidget.class);

        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "Widget update");
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listViewWidget);
        PendingIntent refreshIntent = actionPendingIntent(context, appWidgetIds);
        for (int i : appWidgetIds) {
            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.tiny_tiny_feed_widget);
            rv.setRemoteAdapter(R.id.listViewWidget, intent);
            rv.setEmptyView(R.id.listViewWidget, R.id.widgetEmptyList);

            rv.setOnClickPendingIntent(R.id.lastUpdateText, refreshIntent);
            rv.setOnClickPendingIntent(R.id.widgetEmptyList, refreshIntent);
            Intent startActivityIntent = new Intent(Intent.ACTION_VIEW);
            PendingIntent startActivityPendingIntent = PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.listViewWidget, startActivityPendingIntent);


            Date date = new Date();
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            String dateStr = dateFormat.format(date);
            CharSequence text = context.getText(R.string.lastUpdateText);
            rv.setTextViewText(R.id.lastUpdateText, String.format(text.toString(), dateStr));

            appWidgetManager.updateAppWidget(i, rv);
            super.onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

}


