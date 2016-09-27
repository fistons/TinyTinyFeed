package org.poopeeland.tinytinyfeed;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.poopeeland.tinytinyfeed.settings.SettingsActivity;
import org.poopeeland.tinytinyfeed.widget.ArticleReadActivity;
import org.poopeeland.tinytinyfeed.widget.WidgetService;

import java.text.DateFormat;
import java.util.Date;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link org.poopeeland.tinytinyfeed.settings.SettingsActivity TinyTinyFeedWidgetConfigureActivity}
 */
public class TinyTinyFeedWidget extends AppWidgetProvider {

    public static final String URL_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_URL";
    public static final String USER_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_USER";
    public static final String PASSWORD_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_PASSWORD";
    public static final String ONLY_UNREAD_KEY = "org.poopeeland.tinytinyfeed.ONLY_UNREAD";
    public static final String HTTP_USER_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_HTTP_USER";
    public static final String HTTP_PASSWORD_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_HTTP_PASSWORD";
    public static final String BG_COLOR_KEY = "org.poopeeland.tinytinyfeed.BACKGROUND_COLOR";
    public static final String TEXT_COLOR_KEY = "org.poopeeland.tinytinyfeed.TEXT_COLOR";
    public static final String EXCERPT_LENGHT_KEY = "org.poopeeland.tinytinyfeed.EXCERPT_LENGHT_KEY";
    public static final String NUM_ARTICLE_KEY = "org.poopeeland.tinytinyfeed.NUM_ARTICLE_KEY";
    public static final String SESSION_KEY = "org.poopeeland.tinytinyfeed.SESSION_KEY";
    public static final String ALL_SLL_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_SSL_SELF";
    public static final String ALL_HOST_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_SSL_HOSTNAME";
    public static final String CHECKED = "org.poopeeland.tinytinyfeed.CHECKED";
    private static final String TAG = "TinyTinyFeedWidget";

    /**
     * Return a Pending Intent asking the refresh of the widget
     *
     * @param context the context
     * @param ids     the ids of the widgets to refresh
     * @return a pending intent
     */
    private static PendingIntent actionPendingIntent(Context context, int[] ids) {
        Log.d(TAG, "Create pending intent");
        Intent intent = new Intent(context, TinyTinyFeedWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.d(TAG, "Widget update");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int textColor = preferences.getInt(TEXT_COLOR_KEY, 0xffffff);
        if (preferences.getBoolean(CHECKED, false)) {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listViewWidget);
            PendingIntent refreshIntent = actionPendingIntent(context, appWidgetIds);
            for (int i : appWidgetIds) {
                Intent intent = new Intent(context, WidgetService.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

                RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.tiny_tiny_feed_widget);
                rv.setRemoteAdapter(R.id.listViewWidget, intent);
                rv.setEmptyView(R.id.listViewWidget, R.id.widgetEmptyList);
                rv.setOnClickPendingIntent(R.id.widgetEmptyList, refreshIntent);
                rv.setOnClickPendingIntent(R.id.refreshButton, refreshIntent);
                rv.setInt(R.id.refreshButton, "setColorFilter", textColor);
                rv.setInt(R.id.lastUpdateText, "setTextColor", textColor);
                rv.setInt(R.id.widgetEmptyList, "setTextColor", textColor);
                rv.setInt(R.id.widgetLayoutId, "setBackgroundColor", preferences.getInt(BG_COLOR_KEY, 0x80000000));
                Intent startActivityIntent = new Intent(context, ArticleReadActivity.class);
                PendingIntent startActivityPendingIntent = PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                rv.setPendingIntentTemplate(R.id.listViewWidget, startActivityPendingIntent);

                DateFormat dateFormat = DateFormat.getDateTimeInstance();
                String dateStr = dateFormat.format(new Date());
                CharSequence text = context.getText(R.string.lastUpdateText);
                rv.setTextViewText(R.id.lastUpdateText, String.format(text.toString(), dateStr));

                appWidgetManager.updateAppWidget(i, rv);
            }

        } else {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.configure_widget_layout);
            Intent intent = new Intent(context, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, SettingsActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.no_settings_layout, pendingIntent);
            rv.setInt(R.id.no_settings_layout, "setTextColor", textColor);
            appWidgetManager.updateAppWidget(appWidgetIds, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

}


