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


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link org.poopeeland.tinytinyfeed.SetupActivity TinyTinyFeedWidgetConfigureActivity}
 */
public class TinyTinyFeedWidget extends AppWidgetProvider {

    private static final String TAG = "TinyTinyFeedWidget";

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.d(TAG,"Widget update");
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listViewWidget);

        for (int i = 0; i < appWidgetIds.length; ++i) {

            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.tiny_tiny_feed_widget);
            rv.setRemoteAdapter(R.id.listViewWidget, intent);
            rv.setEmptyView(R.id.listViewWidget, R.id.widgetEmptyList);
            rv.setOnClickPendingIntent(R.id.setupButton, actionPendingIntent(context));


            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            super.onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }


    public static PendingIntent actionPendingIntent(Context context) {
        Intent intent = new Intent(context, SetupActivity.class);
        intent.setAction("LAUNCH_ACTIVITY");
        return PendingIntent.getActivity(context, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

}


