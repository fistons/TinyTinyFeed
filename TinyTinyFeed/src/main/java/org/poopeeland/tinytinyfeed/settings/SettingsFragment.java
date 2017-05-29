package org.poopeeland.tinytinyfeed.settings;


import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.model.Category;
import org.poopeeland.tinytinyfeed.utils.FetchException;
import org.poopeeland.tinytinyfeed.utils.Fetcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen screen = getPreferenceScreen();
        Resources res = getResources();

        AppWidgetManager manager = AppWidgetManager.getInstance(screen.getContext());
        int[] ids = manager.getAppWidgetIds(new ComponentName("org.poopeeland.tinytinyfeed",
                "org.poopeeland.tinytinyfeed.TinyTinyFeedWidget"));


        AndroidNetworking.initialize(screen.getContext());
        SharedPreferences preferences = screen.getSharedPreferences();


        if (ids.length > 0 && preferences.getBoolean(TinyTinyFeedWidget.CHECKED, false)) {
            PreferenceCategory category = new PreferenceCategory(screen.getContext());
            category.setTitle(R.string.choose_category_title);
            screen.addPreference(category);

            try {
                Fetcher fetcher = new Fetcher(preferences, screen.getContext());
                final List<CharSequence> entriesList = new ArrayList<>();
                final List<CharSequence> entryValuesList = new ArrayList<>();

                for (final Category c : fetcher.fetchCategories()) {
                    entryValuesList.add(c.getId());
                    entriesList.add(c.getTitle());
                }
                final CharSequence[] entries = entriesList.toArray(new CharSequence[entriesList.size()]);
                final CharSequence[] entryValues = entryValuesList.toArray(new CharSequence[entryValuesList.size()]);

                for (int i : ids) {
                    addPreferenceList(i, category, res, screen, entries, entryValues);
                }
            } catch (FetchException e) {
                Log.e(TAG, "Data fetching exception", e);
                //TODO Add a Toast here.
            }

        }
    }


    private void addPreferenceList(final int id,
                                   final PreferenceCategory category,
                                   final Resources res,
                                   final PreferenceScreen screen,
                                   final CharSequence[] entries,
                                   final CharSequence[] entryValues) {
        String preferenceKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.WIDGET_CATEGORIES_KEY, id);
        MultiSelectListPreference p = new MultiSelectListPreference(screen.getContext());
        p.setKey(preferenceKey);
        p.setTitle(res.getString(R.string.widget_categories_title, id));
        p.setSummary(res.getString(R.string.widget_categories_summary, id));
        p.setEntries(entries);
        p.setEntryValues(entryValues);
        SharedPreferences preferences = screen.getSharedPreferences();
        if (!preferences.contains(preferenceKey)) {
            Set<String> values = new HashSet<>();
            for (int i = 0; i < entryValues.length - 3; i++) {
                values.add(entryValues[i].toString());
            }
            p.setValues(values);
            preferences.edit().putStringSet(preferenceKey, values).apply();
        }
        category.addPreference(p);
    }

}
