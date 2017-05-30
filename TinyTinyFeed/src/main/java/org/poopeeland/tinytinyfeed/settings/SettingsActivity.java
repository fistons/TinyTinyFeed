package org.poopeeland.tinytinyfeed.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONException;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.TinyTinyFeedWidget;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.utils.FetchException;
import org.poopeeland.tinytinyfeed.utils.Fetcher;

import java.net.MalformedURLException;

public class SettingsActivity extends Activity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences preferences;
    private boolean checked;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_settings);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        this.preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        findViewById(R.id.setup_check_button).setOnClickListener(this::onClick);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        this.checked = false;
        setResult(RESULT_CANCELED);
    }

    public void onClick(final View view) {
        Log.d(TAG, "onClick");
        try {
            this.checkSetup();
        } catch (MalformedURLException e) {
            Toast.makeText(this, R.string.preference_ttrss_url_not_null_or_default, Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Toast.makeText(this, String.format("%s", e.getMessage()), Toast.LENGTH_LONG).show();
        } catch (NoInternetException ex) {
            Toast.makeText(this, R.string.noInternetConnection, Toast.LENGTH_SHORT).show();
        } catch (FetchException e) {
            Toast.makeText(this, "oooops", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSetup() throws JSONException, MalformedURLException, FetchException {
        Log.d(TAG, "checkSetup");
        String url = this.preferences.getString(TinyTinyFeedWidget.URL_KEY, "");

        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (!urlValidator.isValid(url)) {
            throw new MalformedURLException();
        }

        Async async = new Async(this.preferences, this);
        async.execute();
    }

    private class Async extends AsyncTask<Void, Void, String> {

        private final String TAG = Async.class.getSimpleName();
        private final SharedPreferences preferences;
        private final SettingsActivity activity;
        private ProgressDialog dialog;

        public Async(final SharedPreferences preferences, final SettingsActivity activity) {
            this.preferences = preferences;
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute");
            dialog = ProgressDialog.show(activity, getText(R.string.waitTitle), getText(R.string.waitCheck));
        }

        @Override
        protected String doInBackground(final Void... fetchers) {
            Log.d(TAG, "doInBackground");
            try {
                new Fetcher(preferences, activity).testConnection();
            } catch (RuntimeException | FetchException e) {
                Log.e(TAG, e.getMessage());
                return e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String s) {
            super.onPostExecute(s);
            Log.d(TAG, "onPostExecute " + dialog);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (s != null) {
                Toast.makeText(activity, s, Toast.LENGTH_LONG).show();
                return;
            }

            activity.checked = true;

            SharedPreferences.Editor editor = this.preferences.edit();
            editor.putBoolean(TinyTinyFeedWidget.CHECKED, activity.checked);
            editor.apply();

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                setResult(RESULT_OK, resultValue);
            } else {
                setResult(RESULT_OK);
            }

            int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TinyTinyFeedWidget.class));
            Intent updateIntent = new Intent(activity, TinyTinyFeedWidget.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(updateIntent);
            activity.finish();
        }
    }

}
