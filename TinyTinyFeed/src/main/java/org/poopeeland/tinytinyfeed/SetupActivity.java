package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.UrlSuffixException;
import org.poopeeland.tinytinyfeed.widget.WidgetService;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

public class SetupActivity extends Activity {

    public static final String URL_SUFFIX = "/api/";
    private static final String TAG = "TinyTinyFeedSetup";
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    public View.OnClickListener onOkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            try {
                service.checkSetup(url.getText().toString(), httpUser.getText().toString(), httpPassword.getText().toString(), user.getText().toString(), password.getText().toString());
            } catch (MalformedURLException e) {
                Toast.makeText(getApplicationContext(), R.string.urlMalFormed, Toast.LENGTH_LONG).show();
                return;
            } catch (UrlSuffixException e) {
                Toast.makeText(getApplicationContext(), String.format(getText(R.string.urlBadSuffix).toString(), URL_SUFFIX), Toast.LENGTH_LONG).show();
                return;
            } catch (InterruptedException e) {
                Toast.makeText(getApplicationContext(), String.format("%s", e.getMessage()), Toast.LENGTH_LONG).show();
                return;
            } catch (ExecutionException e) {
                Toast.makeText(getApplicationContext(), String.format("%s", e.getMessage()), Toast.LENGTH_LONG).show();
                return;
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), String.format("%s", e.getMessage()), Toast.LENGTH_LONG).show();
                return;
            } catch (CheckException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            } catch (NoInternetException ex) {
                Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();
                return;
            }

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
    private SharedPreferences preferences;
    private EditText url;
    private EditText user;
    private EditText password;
    private EditText httpPassword;
    private EditText httpUser;
    private EditText numArticle;
    private WidgetService service;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            WidgetService.LocalBinder mbinder = (WidgetService.LocalBinder) binder;
            service = mbinder.getService();
            Log.d(TAG, "bounded!");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


    public SetupActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // On annule, comme ça si le user fait "back" sans faire ok avant, on est perché
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_setup);
        findViewById(R.id.setupOkButton).setOnClickListener(onOkClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Intent intentBound = new Intent(this, WidgetService.class);
        intentBound.putExtra(WidgetService.ACTIVITY_FLAG, true);
        bindService(intentBound, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.preferences = getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, MODE_PRIVATE);
        this.url = (EditText) findViewById(R.id.setupUrl);
        this.user = (EditText) findViewById(R.id.setupUser);
        this.password = (EditText) findViewById(R.id.setupPassword);
        this.numArticle = (EditText) findViewById(R.id.setupArticlesNum);
        this.httpUser = (EditText) findViewById(R.id.setupHttpUser);
        this.httpPassword = (EditText) findViewById(R.id.setupHttpPassword);

        this.url.setText(this.preferences.getString(TinyTinyFeedWidget.URL_KEY, "http://"));
        this.user.setText(this.preferences.getString(TinyTinyFeedWidget.USER_KEY, ""));
        this.password.setText(this.preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, ""));
        this.numArticle.setText(this.preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "20"));
        this.httpUser.setText(this.preferences.getString(TinyTinyFeedWidget.HTTP_USER_KEY, ""));
        this.httpPassword.setText(this.preferences.getString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, ""));
    }


    private void save() {
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putString(TinyTinyFeedWidget.URL_KEY, url.getText().toString());
        editor.putString(TinyTinyFeedWidget.USER_KEY, user.getText().toString());
        editor.putString(TinyTinyFeedWidget.PASSWORD_KEY, password.getText().toString());
        editor.putString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, numArticle.getText().toString());
        editor.putString(TinyTinyFeedWidget.HTTP_USER_KEY, httpUser.getText().toString());
        editor.putString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, httpPassword.getText().toString());
        editor.commit();
    }
}
