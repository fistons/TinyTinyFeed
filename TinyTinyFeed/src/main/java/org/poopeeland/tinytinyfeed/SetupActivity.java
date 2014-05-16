package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class SetupActivity extends Activity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences preferences;
    private EditText url;
    private EditText user;
    private EditText password;
    private EditText numArticle;

    public SetupActivity() {
        super();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // On annule, comme ça si le user fait "back" sans faire ok avant, on est perché
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_setup);

        findViewById(R.id.setupOkButton).setOnClickListener(onClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
       /* if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }*/



    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            save();

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                setResult(RESULT_OK, resultValue);
            } else {
                setResult(RESULT_OK);
            }

            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        this.preferences = getSharedPreferences(TinyTinyFeed.PREFERENCE_KEY, MODE_PRIVATE);
        this.url = (EditText) findViewById(R.id.setupUrl);
        this.user = (EditText) findViewById(R.id.setupUser);
        this.password = (EditText) findViewById(R.id.setupPassword);
        this.numArticle = (EditText) findViewById(R.id.setupArticlesNum);

        this.url.setText(this.preferences.getString(TinyTinyFeed.URL_KEY, "http://"));
        this.user.setText(this.preferences.getString(TinyTinyFeed.USER_KEY, ""));
        this.password.setText(this.preferences.getString(TinyTinyFeed.PASSWORD_KEY, ""));
        this.numArticle.setText(this.preferences.getString(TinyTinyFeed.NUM_ARTICLE_KEY, ""));
    }

    private void save() {
        SharedPreferences.Editor editor = this.preferences.edit();

        try {
            new URL(url.getText().toString());
            editor.putString(TinyTinyFeed.URL_KEY, url.getText().toString());
        } catch (MalformedURLException ex) {
            Toast.makeText(getApplicationContext(), getText(R.string.urlMalFormed), Toast.LENGTH_LONG).show();
        }

        editor.putString(TinyTinyFeed.USER_KEY, user.getText().toString());
        editor.putString(TinyTinyFeed.PASSWORD_KEY, password.getText().toString());
        editor.putString(TinyTinyFeed.NUM_ARTICLE_KEY, numArticle.getText().toString());
        editor.commit();
    }
}
