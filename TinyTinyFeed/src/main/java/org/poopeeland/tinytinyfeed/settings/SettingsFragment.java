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
import com.rx2androidnetworking.Rx2AndroidNetworking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private SharedPreferences preferences;


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
        this.preferences = screen.getSharedPreferences();


        if (ids.length > 0 && preferences.getBoolean(TinyTinyFeedWidget.CHECKED, false)) {


            PreferenceCategory category = new PreferenceCategory(screen.getContext());
            category.setTitle(R.string.choose_category_title);
            screen.addPreference(category);


            try {
                this.loginAndLoadCategories(screen, category, res, ids);
            } catch (JSONException e) {
                Log.e(TAG, "ow shit", e);
            }


        }
    }

    private void loginAndLoadCategories(final PreferenceScreen screen,
                                        final PreferenceCategory category,
                                        final Resources res,
                                        final int[] ids) throws JSONException {

        String u = preferences.getString(TinyTinyFeedWidget.URL_KEY, "") + "/api/";
        String user = preferences.getString(TinyTinyFeedWidget.USER_KEY, "");
        String password = preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, "");

        final JSONObject jsonLogin = new JSONObject();
        jsonLogin.put("user", user);
        jsonLogin.put("password", password);
        jsonLogin.put("op", "login");

        Rx2AndroidNetworking.post(u)
                .addJSONObjectBody(jsonLogin)
                .build()
                .getJSONObjectObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<JSONObject>() {
                    @Override
                    public void accept(@NonNull JSONObject jsonObject) throws Exception {
                        Log.d(TAG, "coucou j'ai fini");
                        handleLoginResponse(screen, category, jsonObject, res, ids);
                    }
                });
    }

    private void handleLoginResponse(final PreferenceScreen screen,
                                     final PreferenceCategory category,
                                     final JSONObject jsonObject,
                                     final Resources res,
                                     final int[] ids) throws JSONException {
        if (jsonObject.getJSONObject("content").has("error")) {
            Log.e(TAG, "Login error!");
        } else {
            final String sessionId = jsonObject.getJSONObject("content").getString("session_id");
            Log.d(TAG, "Session Id: " + sessionId);

            final List<CharSequence> entriesList = new ArrayList<>();
            final List<CharSequence> entryValuesList = new ArrayList<>();

            for (Category c : fetchCategories(sessionId)) {
                entryValuesList.add(c.getId());
                entriesList.add(c.getTitle());
            }
            final CharSequence[] entries = entriesList.toArray(new CharSequence[entriesList.size()]);
            final CharSequence[] entryValues = entryValuesList.toArray(new CharSequence[entryValuesList.size()]);

            for (int i : ids) {
                addPreferenceList(i, category, res, screen, entries, entryValues);
            }

            logout(sessionId);
        }
    }

    private void logout(final String session) throws JSONException {
        String url = preferences.getString(TinyTinyFeedWidget.URL_KEY, "") + "/api/";
        final JSONObject jsonLogin = new JSONObject();
        jsonLogin.put("sid", session);
        jsonLogin.put("op", "logout");

        Rx2AndroidNetworking.post(url)
                .addJSONObjectBody(jsonLogin)
                .build()
                .getStringObservable()
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String s) throws Exception {

                    }
                });
    }

    private List<Category> fetchCategories(final String sessionId) throws JSONException {
        final JSONObject json = new JSONObject();
        json.put("sid", sessionId);
        json.put("op", "getCategories");
        json.put("enable_nested", false);
        json.put("include_empty", true);

        String u = preferences.getString(TinyTinyFeedWidget.URL_KEY, "") + "/api/";
        return Rx2AndroidNetworking.post(u)
                .addJSONObjectBody(json)
                .build()
                .getJSONObjectObservable()
                .subscribeOn(Schedulers.io())
                .map(new Function<JSONObject, List<Category>>() {
                    @Override
                    public List<Category> apply(@NonNull JSONObject response) throws Exception {
                        List<Category> categories = new ArrayList<>();
                        Log.d(TAG, response.toString());
                        JSONArray array = response.getJSONArray("content");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject c = array.getJSONObject(i);
                            Category cat = new Category();
                            cat.setId(c.getString("id"));
                            cat.setTitle(c.getString("title"));
                            categories.add(cat);
                        }
                        return categories;
                    }
                })
                .blockingFirst();
    }

    private void addPreferenceList(final int id,
                                   final PreferenceCategory category,
                                   final Resources res,
                                   final PreferenceScreen screen,
                                   final CharSequence[] entries,
                                   final CharSequence[] entryValues) {
        MultiSelectListPreference p = new MultiSelectListPreference(screen.getContext());
        p.setKey(String.format(Locale.getDefault(), TinyTinyFeedWidget.WIDGET_CATEGORIES_KEY, id));
        p.setTitle(res.getString(R.string.widget_categories_title, id));
        p.setSummary(res.getString(R.string.widget_categories_summary, id));
        p.setEntries(entries);
        p.setEntryValues(entryValues);

        category.addPreference(p);
    }

}
