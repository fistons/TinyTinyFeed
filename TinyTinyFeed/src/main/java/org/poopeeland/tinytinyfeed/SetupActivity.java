package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;
import org.poopeeland.tinytinyfeed.exceptions.UrlSuffixException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class SetupActivity extends Activity {

    private static final String TAG = "TinyTinyFeedSetup";
    private final String URL_SUFFIX = "/api/";
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    public View.OnClickListener onOkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            try {
                checkSetup();
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
        findViewById(R.id.setupOkButton).setOnClickListener(onOkClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.preferences = getSharedPreferences(TinyTinyFeedWidget.PREFERENCE_KEY, MODE_PRIVATE);
        this.url = (EditText) findViewById(R.id.setupUrl);
        this.user = (EditText) findViewById(R.id.setupUser);
        this.password = (EditText) findViewById(R.id.setupPassword);
        this.numArticle = (EditText) findViewById(R.id.setupArticlesNum);

        this.url.setText(this.preferences.getString(TinyTinyFeedWidget.URL_KEY, "http://"));
        this.user.setText(this.preferences.getString(TinyTinyFeedWidget.USER_KEY, ""));
        this.password.setText(this.preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, ""));
        this.numArticle.setText(this.preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "20"));
    }

    private void checkSetup() throws MalformedURLException, UrlSuffixException, JSONException, ExecutionException, InterruptedException, CheckException {
        String urlString = url.getText().toString();
        if (!urlString.endsWith(URL_SUFFIX)) {
            throw new UrlSuffixException();
        }
        new URL(urlString); // To check if the URL is a real one
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user.getText().toString());
        jsonObject.put("password", password.getText().toString());
        jsonObject.put("op", "login");
        RequestTask task = new RequestTask(new DefaultHttpClient(), urlString);
        task.execute(jsonObject);
        JSONObject response = task.get();
        if (response.getInt("status") != 0) {
            try {
                TtrssError reason = TtrssError.valueOf(response.getJSONObject("content").getString("error"));
                switch (reason) {
                    case LOGIN_ERROR:
                        Log.e(TAG, response.getJSONObject("content").getString("error"));
                        throw new CheckException(getText(R.string.badLogin).toString());
                    case CLIENT_PROTOCOL_EXCEPTION:
                    case UNREACHABLE_TTRSS:
                    case IO_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(getString(R.string.connectionError));
                    case UNSUPPORTED_ENCODING:
                    case JSON_EXCEPTION:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(getString(R.string.impossibleError), response.getJSONObject("content").getString("message")));
                    default:
                        Log.e(TAG, response.getJSONObject("content").getString("message"));
                        throw new CheckException(String.format(getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
                }
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, response.getJSONObject("content").getString("message"));
                throw new CheckException(String.format(getString(R.string.unknownError), response.getJSONObject("content").getString("message")));
            }
        }
    }

    private void save() {
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putString(TinyTinyFeedWidget.URL_KEY, url.getText().toString());
        editor.putString(TinyTinyFeedWidget.USER_KEY, user.getText().toString());
        editor.putString(TinyTinyFeedWidget.PASSWORD_KEY, password.getText().toString());
        editor.putString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, numArticle.getText().toString());
        editor.commit();
    }
}
