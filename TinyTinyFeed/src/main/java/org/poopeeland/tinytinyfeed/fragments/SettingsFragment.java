package org.poopeeland.tinytinyfeed.fragments;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.activities.WidgetSettingsActivity;
import org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget;

import java.util.Locale;

import static org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget.WIDGET_NAME_KEY;


/**
 * A simple {@link PreferenceFragment} subclass.
 * <p>
 * Tries to load the categories created on tt-rss when one or several widgets are on screen.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen screen = getPreferenceScreen();

        AppWidgetManager manager = AppWidgetManager.getInstance(screen.getContext());

        int[] ids = manager.getAppWidgetIds(new ComponentName(screen.getContext().getPackageName(),
                TinyTinyFeedWidget.class.getName()));

        SharedPreferences preferences = screen.getSharedPreferences();
        if (ids.length > 0) {
            PreferenceCategory category = new PreferenceCategory(screen.getContext());
            category.setTitle(R.string.widget_configuration_title);
            screen.addPreference(category);

            for (int i : ids) {
                PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(screen.getContext());
                String name = preferences.getString(String.format(Locale.getDefault(), WIDGET_NAME_KEY, i), "Widget #" + i);
                preferenceScreen.setTitle(getString(R.string.widget_preference_title, name));
                preferenceScreen.setShouldDisableView(true);
                preferenceScreen.setEnabled(preferences.getBoolean(TinyTinyFeedWidget.CHECKED, false));
                Intent intent = new Intent();
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
                intent.setComponent(new ComponentName(screen.getContext(), WidgetSettingsActivity.class.getName()));
                preferenceScreen.setIntent(intent);

                category.addPreference(preferenceScreen);
            }

        }
    }
}
