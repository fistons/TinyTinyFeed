package org.poopeeland.tinytinyfeed.activities;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.commons.validator.routines.UrlValidator;
import org.poopeeland.tinytinyfeed.R;
import org.poopeeland.tinytinyfeed.network.Fetcher;
import org.poopeeland.tinytinyfeed.network.exceptions.FetchException;
import org.poopeeland.tinytinyfeed.utils.ExceptionAsyncTask;
import org.poopeeland.tinytinyfeed.widgets.TinyTinyFeedWidget;

import java.net.MalformedURLException;

public class SettingsActivity extends Activity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences preferences;
    private boolean checked;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        this.preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        findViewById(R.id.setup_check_button).setOnClickListener(v -> {
            try {
                String url = this.preferences.getString(TinyTinyFeedWidget.URL_KEY, "");
                String[] schemes = {"http", "https"};
                UrlValidator urlValidator = new UrlValidator(schemes);
                if (!urlValidator.isValid(url)) {
                    throw new MalformedURLException();
                }
                Async async = new Async(this.preferences, this);
                async.execute();
            } catch (MalformedURLException e) {
                Toast.makeText(this, R.string.preference_ttrss_url_not_null_or_default, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.checked = false;
        setResult(RESULT_CANCELED);
    }

    private class Async extends ExceptionAsyncTask<Void, Void, Void> {

        private final SharedPreferences preferences;
        private final SettingsActivity activity;
        private final ProgressBar progressBar;
        private final Button button;

        private Async(final SharedPreferences preferences, final SettingsActivity activity) {
            super(activity);
            this.preferences = preferences;
            this.activity = activity;
            this.progressBar = findViewById(R.id.progressBar);
            this.button = findViewById(R.id.setup_check_button);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.progressBar.setVisibility(View.VISIBLE);
            this.button.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground() throws FetchException {
            new Fetcher(preferences, activity).testConnection();
            return null;
        }

        @Override
        protected void onSafePostExecute(final Void result) {
            this.progressBar.setVisibility(View.GONE);
            this.button.setVisibility(View.VISIBLE);

            if (onError()) {
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
