package org.poopeeland.tinytinyfeed.fragments;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;

import com.rarepebble.colorpicker.ColorPreference;

import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.interfaces.TrimmedEditTextPreference;
import org.poopeeland.tinytinyfeed.models.Feed;
import org.poopeeland.tinytinyfeed.network.Fetcher;
import org.poopeeland.tinytinyfeed.network.exceptions.FetchException;
import org.poopeeland.tinytinyfeed.utils.ExceptionAsyncTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget.*;

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
        this.textColorKey = String.format(Locale.getDefault(), TEXT_COLOR_KEY, widgetId);
        this.sourceColorKey = String.format(Locale.getDefault(), SOURCE_COLOR_KEY, widgetId);
        this.titleColorKey = String.format(Locale.getDefault(), TITLE_COLOR_KEY, widgetId);
        this.textSizeKey = String.format(Locale.getDefault(), TEXT_SIZE_KEY, widgetId);
        this.sourceSizeKey = String.format(Locale.getDefault(), SOURCE_SIZE_KEY, widgetId);
        this.titleSizeKey = String.format(Locale.getDefault(), TITLE_SIZE_KEY, widgetId);
        this.bgColorKey = String.format(Locale.getDefault(), BG_COLOR_KEY, widgetId);
        this.numArticlesKey = String.format(Locale.getDefault(), NUM_ARTICLE_KEY, widgetId);
        this.onlyUnreadKey = String.format(Locale.getDefault(), ONLY_UNREAD_KEY, widgetId);
        this.forceUpdateKey = String.format(Locale.getDefault(), FORCE_UPDATE_KEY, widgetId);
        this.excerptLengthKey = String.format(Locale.getDefault(), EXCERPT_LENGTH_KEY, widgetId);
        this.statusColorKey = String.format(Locale.getDefault(), STATUS_COLOR_KEY, widgetId);
        this.categoriesKey = String.format(Locale.getDefault(), WIDGET_CATEGORIES_KEY, widgetId);
        this.widgetNameKey = String.format(Locale.getDefault(), WIDGET_NAME_KEY, widgetId);
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

        this.initDefaultColor(preferences);
        try {
            AsyncCategoryFetcher fetcher = new AsyncCategoryFetcher(preferences, res, screen);
            fetcher.execute();
        } catch (FetchException e) {
            Log.e(TAG, "Exception while creating the fetcher", e);
        }

        screen.addPreference(createWidgetNamePref());

        screen.addPreference(createNumArticlePref());
        screen.addPreference(createExcerptSizePref());

        screen.addPreference(createOnlyUnreadPref());
        screen.addPreference(createForceUpdatePref());

        screen.addPreference(createTitleColorPref());
        screen.addPreference(createTitleSizePref());

        screen.addPreference(createSourceColorPref());
        screen.addPreference(createSourceSizePref());

        screen.addPreference(createTextColorPref());
        screen.addPreference(createTextSizePref());

        screen.addPreference(createStatusColorPref());

        screen.addPreference(createBackgroundColorPref());
    }

    private void initDefaultColor(final SharedPreferences preferences) {
        if (!preferences.contains(this.textColorKey) ||
                !preferences.contains(this.sourceColorKey) ||
                !preferences.contains(this.titleColorKey) ||
                !preferences.contains(this.statusColorKey) ||
                !preferences.contains(this.bgColorKey)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(this.textColorKey, DEFAULT_TEXT_COLOR);
            editor.putInt(this.sourceColorKey, DEFAULT_TEXT_COLOR);
            editor.putInt(this.titleColorKey, DEFAULT_TEXT_COLOR);
            editor.putInt(this.statusColorKey, DEFAULT_TEXT_COLOR);
            editor.putInt(this.bgColorKey, DEFAULT_BG_COLOR);
            editor.apply();
        }
    }

    private Preference createTextColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(DEFAULT_TEXT_COLOR);
        preference.setKey(textColorKey);
        preference.setSummary(R.string.preference_text_color_summary);
        preference.setTitle(R.string.preference_text_color_title);
        preference.setOrder(9);
        return preference;
    }

    private Preference createSourceColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(DEFAULT_TEXT_COLOR);
        preference.setKey(sourceColorKey);
        preference.setSummary(R.string.preference_source_color_summary);
        preference.setTitle(R.string.preference_source_color_title);
        preference.setOrder(7);
        return preference;
    }

    private Preference createTitleColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(DEFAULT_TEXT_COLOR);
        preference.setKey(titleColorKey);
        preference.setSummary(R.string.preference_title_color_summary);
        preference.setTitle(R.string.preference_title_color_title);
        preference.setOrder(5);
        return preference;
    }

    private Preference createStatusColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(DEFAULT_TEXT_COLOR);
        preference.setKey(statusColorKey);
        preference.setSummary(R.string.preference_status_color_summary);
        preference.setTitle(R.string.preference_status_color_title);
        preference.setOrder(11);
        return preference;
    }

    private Preference createBackgroundColorPref() {
        ColorPreference preference = new ColorPreference(screen.getContext());
        preference.setDefaultValue(DEFAULT_BG_COLOR);
        preference.setKey(bgColorKey);
        preference.setSummary(R.string.preference_background_color_summary);
        preference.setTitle(R.string.preference_background_color_title);
        preference.setOrder(12);
        return preference;
    }

    private Preference createTextSizePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue(DEFAULT_TEXT_SIZE);
        preference.setKey(textSizeKey);
        preference.setSummary(R.string.preference_text_size_summary);
        preference.setTitle(R.string.preference_text_size_title);
        preference.setOrder(10);
        return preference;
    }

    private Preference createTitleSizePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue(DEFAULT_TEXT_SIZE);
        preference.setKey(titleSizeKey);
        preference.setSummary(R.string.preference_title_size_summary);
        preference.setTitle(R.string.preference_title_size_title);
        preference.setOrder(6);
        return preference;
    }

    private Preference createSourceSizePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue(DEFAULT_TEXT_SIZE);
        preference.setKey(sourceSizeKey);
        preference.setSummary(R.string.preference_source_size_summary);
        preference.setTitle(R.string.preference_source_size_title);
        preference.setOrder(8);
        return preference;
    }

    private Preference createNumArticlePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue(DEFAULT_NUM_ARTICLE);
        preference.setKey(numArticlesKey);
        preference.setSummary(R.string.preference_article_number_summary);
        preference.setTitle(R.string.setupNumArticles);
        preference.setOrder(2);
        return preference;
    }

    private Preference createExcerptSizePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER);
        preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        preference.setDefaultValue(DEFAULT_EXCERPT_SIZE);
        preference.setKey(excerptLengthKey);
        preference.setSummary(R.string.preference_article_excerpt_lenght_summary);
        preference.setTitle(R.string.preference_article_excerpt_lenght);
        preference.setOrder(3);
        return preference;
    }

    private Preference createWidgetNamePref() {
        TrimmedEditTextPreference preference = new TrimmedEditTextPreference(screen.getContext());
        preference.setDefaultValue("Widget #" + this.widgetId);
        preference.setKey(widgetNameKey);
        preference.setSummary(R.string.widget_name_summary);
        preference.setTitle(R.string.widget_name_title);
        preference.setOrder(0);
        return preference;
    }

    private Preference createForceUpdatePref() {
        CheckBoxPreference preference = new CheckBoxPreference(screen.getContext());
        preference.setDefaultValue(false);
        preference.setKey(forceUpdateKey);
        preference.setSummary(R.string.preference_force_update_summary);
        preference.setTitle(R.string.preference_force_update_title);
        preference.setOrder(4);
        return preference;
    }


    private Preference createOnlyUnreadPref() {
        CheckBoxPreference preference = new CheckBoxPreference(screen.getContext());
        preference.setDefaultValue(false);
        preference.setKey(onlyUnreadKey);
        preference.setSummary(R.string.preference_unread_only_summary);
        preference.setTitle(R.string.setupRetrieveOnlyUnread);
        preference.setOrder(3);
        return preference;
    }


    private void addCategoriesList(final Resources res,
                                   final PreferenceScreen screen,
                                   final CharSequence[] entries,
                                   final CharSequence[] entryValues) {

        MultiSelectListPreference p = new MultiSelectListPreference(screen.getContext());
        p.setOrder(1);
        p.setKey(categoriesKey);
        p.setTitle(res.getString(R.string.widget_categories_title));
        p.setSummary(res.getString(R.string.widget_categories_summary));
        p.setEntries(entries);
        p.setEntryValues(entryValues);
        SharedPreferences preferences = screen.getSharedPreferences();
        if (!preferences.contains(categoriesKey)) {
            Set<String> values = new HashSet<>();
            values.add("-4");
            p.setValues(values);
            preferences.edit().putStringSet(categoriesKey, values).apply();
        }
        screen.addPreference(p);
    }

    private class AsyncCategoryFetcher extends ExceptionAsyncTask<Void, Void, List<Feed>> {

        private final Context context;
        private final Resources res;
        private final PreferenceScreen screen;
        private final Fetcher fetcher;
        private final EditTextPreference loadingPreferences;

        private AsyncCategoryFetcher(final SharedPreferences preferences,
                                     final Resources res,
                                     final PreferenceScreen screen) throws FetchException {
            super(screen.getContext());
            this.context = screen.getContext();

            this.res = res;
            this.screen = screen;
            this.fetcher = new Fetcher(preferences, this.context);

            loadingPreferences = new EditTextPreference(screen.getContext());
            loadingPreferences.setOrder(1);
            loadingPreferences.setSummary(R.string.preference_loading_categories);
            loadingPreferences.setEnabled(false);
            screen.addPreference(loadingPreferences);
        }

        @Override
        protected List<Feed> doInBackground() throws FetchException {
            return fetcher.fetchFeeds();
        }

        @Override
        protected void onSafePostExecute(final List<Feed> feeds) {
            if (onError()) {
                loadingPreferences.setSummary(R.string.preference_cant_load_categories);
                return;
            }

            final List<CharSequence> entriesList = new ArrayList<>();
            final List<CharSequence> entryValuesList = new ArrayList<>();

            for (Feed d : feeds) {
                entryValuesList.add(d.getId());
                entriesList.add(d.getTitle());
            }
            final CharSequence[] entries = entriesList.toArray(new CharSequence[entriesList.size()]);
            final CharSequence[] entryValues = entryValuesList.toArray(new CharSequence[entryValuesList.size()]);

            addCategoriesList(res, screen, entries, entryValues);

            screen.removePreference(loadingPreferences);
        }
    }
}
