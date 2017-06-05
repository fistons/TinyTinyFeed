package org.poopeeland.tinytinyfeed.activities;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget;

import java.util.Locale;

/**
 * Widget Activity Screen
 * Created by eric on 03/06/17.
 */

public class WidgetSettingsActivity extends Activity {

    private static final String TAG = WidgetSettingsActivity.class.getSimpleName();

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_widget);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String widgetName = preferences.getString(String.format(Locale.getDefault(), TinyTinyFeedWidget.WIDGET_NAME_KEY, widgetId), "Widget #" + widgetId);

        setTitle(getString(R.string.widget_preference_title, widgetName));
        Log.d(TAG, "Caller widget: " + this.widgetId);

        this.findViewById(R.id.setup_activity_check_button).setOnClickListener(view -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setResult(RESULT_CANCELED);
    }
}
