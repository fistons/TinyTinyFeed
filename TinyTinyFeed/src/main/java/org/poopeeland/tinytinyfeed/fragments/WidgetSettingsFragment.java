package org.poopeeland.tinytinyfeed.fragments;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;

import com.rarepebble.colorpicker.ColorPreference;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.interfaces.TrimmedEditTextPreference;
import org.poopeeland.tinytinyfeed.models.Category;
import org.poopeeland.tinytinyfeed.network.Fetcher;
import org.poopeeland.tinytinyfeed.network.exceptions.FetchException;
import org.poopeeland.tinytinyfeed.utils.ExceptionAsyncTask;
import org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget.WIDGET_NAME_KEY;

/**
 * Widget settings fragment.
 * Created by eric on 03/06/17.
 */

public class WidgetSettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private String textColorKey;
    private String sourceColorKey;
    private String titleColorKey;
    private String textSizeKey;
    private String sourceSizeKey;
    private String titleSizeKey;
    private String bgColorKey;
    private String numArticlesKey;
    private String onlyUnreadKey;
    private String forceUpdateKey;
    private String excerptLengthKey;
    private String statusColorKey;
    private String categoriesKey;
    private String widgetNameKey;

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private PreferenceScreen screen;

    private void loadPreferencesKey() {
        this.textColorKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.TEXT_COLOR_KEY, widgetId);
        this.sourceColorKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.SOURCE_COLOR_KEY, widgetId);
        this.titleColorKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.TITLE_COLOR_KEY, widgetId);
        this.textSizeKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.TEXT_SIZE_KEY, widgetId);
        this.sourceSizeKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.SOURCE_SIZE_KEY, widgetId);
        this.titleSizeKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.TITLE_SIZE_KEY, widgetId);
        this.bgColorKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.BG_COLOR_KEY, widgetId);
        this.numArticlesKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.NUM_ARTICLE_KEY, widgetId);
        this.onlyUnreadKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.ONLY_UNREAD_KEY, widgetId);
        this.forceUpdateKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.FORCE_UPDATE_KEY, widgetId);
        this.excerptLengthKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.EXCERPT_LENGTH_KEY, widgetId);
        this.statusColorKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.STATUS_COLOR_KEY, widgetId);
        this.categoriesKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.WIDGET_CATEGORIES_KEY, widgetId);
        this.widgetNameKey = String.format(Locale.getDefault(), TinyTinyFeedWidget.WIDGET_NAME_KEY, widgetId);
    }


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.widget_preferences);


        Bundle extras = getActivity().getIntent().getExtras();


        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        this.loadPreferencesKey();


        this.screen = getPreferenceScreen();
        Resources res = getResources();
        SharedPreferences preferences = screen.getSharedPreferences();

        screen.addPreference(createWidgetNamePref());

        screen.addPreference(createTitleColorPref());
        screen.addPreference(createTitleSizePref());

        screen.addPreference(createSourceColorPref());
        screen.addPreference(createSourceSizePref());

        screen.addPreference(createTextColorPref());
        screen.addPreference(createTextSizePref());

        screen.addPreference(createStatusColorPref());
        screen.addPreference(createBackgroundColorPref());

        screen.addPreference(createNumArticlePref());
        screen.addPreference(createExcerptSizePref());

        screen.addPreference(createOnlyUnreadPref());
        screen.addPreference(createForceUpdatePref());


        try {
            String name = preferences.getString(String.format(Locale.getDefault(), WIDGET_NAME_KEY, widgetId), "Widget #" + widgetId);

            AsyncCategoryFetcher fetcher = new AsyncCategoryFetcher(name, preferences, res, screen);
            fetcher.execute();
        } catch (FetchException e) {
            Log.e(TAG, "Exception while creating the fetcher", e);
        }
    }

    private Preference createTextColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(0xff000000);
        preference.setKey(textColorKey);
        preference.setSummary(R.string.preference_text_color_summary);
        preference.setTitle(R.string.preference_text_color_title);
        return preference;
    }

    private Preference createSourceColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(0xff000000);
        preference.setKey(sourceColorKey);
        preference.setSummary(R.string.preference_source_color_summary);
        preference.setTitle(R.string.preference_source_color_title);
        return preference;
    }

    private Preference createTitleColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(0xff000000);
        preference.setKey(titleColorKey);
        preference.setSummary(R.string.preference_title_color_summary);
        preference.setTitle(R.string.preference_title_color_title);
        return preference;
    }

    private Preference createStatusColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(0xff000000);
        preference.setKey(statusColorKey);
        preference.setSummary(R.string.preference_status_color_summary);
        preference.setTitle(R.string.preference_status_color_title);
        return preference;
    }

    private Preference createBackgroundColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(0x80000000);
        preference.setKey(bgColorKey);
        preference.setSummary(R.string.preference_background_color_summary);
        preference.setTitle(R.string.preference_background_color_title);

        return preference;
    }

    private Preference createTextSizePref() {

        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue("10");
        preference.setKey(textSizeKey);
        preference.setSummary(R.string.preference_text_size_summary);
        preference.setTitle(R.string.preference_text_size_title);
        return preference;
    }

    private Preference createTitleSizePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue("10");
        preference.setKey(titleSizeKey);
        preference.setSummary(R.string.preference_title_size_summary);
        preference.setTitle(R.string.preference_title_size_title);
        return preference;
    }

    private Preference createSourceSizePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue("10");
        preference.setKey(sourceSizeKey);
        preference.setSummary(R.string.preference_source_size_summary);
        preference.setTitle(R.string.preference_source_size_title);
        return preference;
    }

    private Preference createNumArticlePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue("20");
        preference.setKey(numArticlesKey);
        preference.setSummary(R.string.preference_article_number_summary);
        preference.setTitle(R.string.setupNumArticles);
        return preference;
    }

    private Preference createExcerptSizePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue("200");
        preference.setKey(excerptLengthKey);
        preference.setSummary(R.string.preference_article_excerpt_lenght_summary);
        preference.setTitle(R.string.preference_article_excerpt_lenght);
        return preference;
    }

    private Preference createWidgetNamePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.setDefaultValue("Widget #" + this.widgetId);
        preference.setKey(widgetNameKey);
        preference.setSummary(R.string.widget_name_summary);
        preference.setTitle(R.string.widget_name_title);
        return preference;
    }

    private Preference createForceUpdatePref() {
        CheckBoxPreference preference = new CheckBoxPreference(screen.getContext());
        preference.setDefaultValue(false);
        preference.setKey(forceUpdateKey);
        preference.setSummary(R.string.preference_force_update_summary);
        preference.setTitle(R.string.preference_force_update_title);
        return preference;
    }


    private Preference createOnlyUnreadPref() {
        CheckBoxPreference preference = new CheckBoxPreference(screen.getContext());
        preference.setDefaultValue(false);
        preference.setKey(onlyUnreadKey);
        preference.setSummary(R.string.preference_unread_only_summary);
        preference.setTitle(R.string.setupRetrieveOnlyUnread);
        return preference;
    }


    private void addPreferenceList(final String name,
                                   final Resources res,
                                   final PreferenceScreen screen,
                                   final CharSequence[] entries,
                                   final CharSequence[] entryValues) {

        MultiSelectListPreference p = new MultiSelectListPreference(screen.getContext());
        p.setKey(categoriesKey);
        p.setTitle(res.getString(R.string.widget_categories_title, name));
        p.setSummary(res.getString(R.string.widget_categories_summary, name));
        p.setEntries(entries);
        p.setEntryValues(entryValues);
        SharedPreferences preferences = screen.getSharedPreferences();
        if (!preferences.contains(categoriesKey)) {
            Set<String> values = new HashSet<>();
            for (int i = 0; i < entryValues.length - 3; i++) {
                values.add(entryValues[i].toString());
            }
            p.setValues(values);
            preferences.edit().putStringSet(categoriesKey, values).apply();
        }
        screen.addPreference(p);
    }

    private class AsyncCategoryFetcher extends ExceptionAsyncTask<Void, Void, List<Category>> {

        private final String name;
        private final Context context;
        private final Resources res;
        private final PreferenceScreen screen;
        private final Fetcher fetcher;

        private AsyncCategoryFetcher(final String name,
                                     final SharedPreferences preferences,
                                     final Resources res,
                                     final PreferenceScreen screen) throws FetchException {
            super(screen.getContext());
            this.context = screen.getContext();

            this.name = name;
            this.res = res;
            this.screen = screen;
            this.fetcher = new Fetcher(preferences, this.context);
        }

        @Override
        protected List<Category> doInBackground() throws FetchException {
            return fetcher.fetchCategories();
        }

        @Override
        protected void onSafePostExecute(final List<Category> categories) {
            if (onError()) {
                return;
            }
            PreferenceCategory category = new PreferenceCategory(screen.getContext());
            category.setTitle(R.string.widget_configuration_title);
            screen.addPreference(category);

            final List<CharSequence> entriesList = new ArrayList<>();
            final List<CharSequence> entryValuesList = new ArrayList<>();

            for (Category c : categories) {
                entryValuesList.add(c.getId());
                entriesList.add(c.getTitle());
            }
            final CharSequence[] entries = entriesList.toArray(new CharSequence[entriesList.size()]);
            final CharSequence[] entryValues = entryValuesList.toArray(new CharSequence[entryValuesList.size()]);

            addPreferenceList(name, res, screen, entries, entryValues);
        }
    }
}
