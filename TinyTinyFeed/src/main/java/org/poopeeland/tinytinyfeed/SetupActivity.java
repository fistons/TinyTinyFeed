package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.exceptions.CheckException;
import org.poopeeland.tinytinyfeed.exceptions.NoInternetException;
import org.poopeeland.tinytinyfeed.exceptions.TtrssError;
import org.poopeeland.tinytinyfeed.exceptions.UrlSuffixException;
import org.poopeeland.tinytinyfeed.widget.WidgetService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class SetupActivity extends Activity implements View.OnClickListener {

    public static final String URL_SUFFIX = "/api/";
    private static final String TAG = "TinyTinyFeedSetup";
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private ConnectivityManager connMgr;
    private SharedPreferences preferences;
    private EditText url;
    private EditText user;
    private EditText password;
    private CheckBox checkBox;
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

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.setupOkButton:
                save();

                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                    setResult(RESULT_OK, resultValue);
                } else {
                    setResult(RESULT_OK);
                }

                finish();
                break;
            case R.id.setupCheckButton:
                try {
                    this.checkSetup(url.getText().toString(), httpUser.getText().toString(), httpPassword.getText().toString(), user.getText().toString(), password.getText().toString());
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
                } catch (NoInternetException ex) {
                    Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();
                    return;
                }

                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // On annule, comme ça si le user fait "back" sans faire ok avant, on est perché
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_setup);
        findViewById(R.id.setupOkButton).setOnClickListener(this);
        findViewById(R.id.setupCheckButton).setOnClickListener(this);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Intent intentBound = new Intent(this, WidgetService.class);
        intentBound.putExtra(WidgetService.ACTIVITY_FLAG, true);
        bindService(intentBound, mConnection, Context.BIND_AUTO_CREATE);

        this.connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
        this.checkBox = (CheckBox) findViewById(R.id.setupOnlyUnread);
        this.numArticle = (EditText) findViewById(R.id.setupArticlesNum);
        this.httpUser = (EditText) findViewById(R.id.setupHttpUser);
        this.httpPassword = (EditText) findViewById(R.id.setupHttpPassword);

        this.url.setText(this.preferences.getString(TinyTinyFeedWidget.URL_KEY, "http://"));
        this.user.setText(this.preferences.getString(TinyTinyFeedWidget.USER_KEY, ""));
        this.password.setText(this.preferences.getString(TinyTinyFeedWidget.PASSWORD_KEY, ""));
        this.checkBox.setChecked(this.preferences.getBoolean(TinyTinyFeedWidget.ONLY_UNREAD_KEY, false));
        this.numArticle.setText(this.preferences.getString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, "20"));
        this.httpUser.setText(this.preferences.getString(TinyTinyFeedWidget.HTTP_USER_KEY, ""));
        this.httpPassword.setText(this.preferences.getString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, ""));
    }


    private void save() {
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putString(TinyTinyFeedWidget.URL_KEY, url.getText().toString());
        editor.putString(TinyTinyFeedWidget.USER_KEY, user.getText().toString());
        editor.putString(TinyTinyFeedWidget.PASSWORD_KEY, password.getText().toString());
        editor.putBoolean(TinyTinyFeedWidget.ONLY_UNREAD_KEY, checkBox.isChecked());
        editor.putString(TinyTinyFeedWidget.NUM_ARTICLE_KEY, numArticle.getText().toString());
        editor.putString(TinyTinyFeedWidget.HTTP_USER_KEY, httpUser.getText().toString());
        editor.putString(TinyTinyFeedWidget.HTTP_PASSWORD_KEY, httpPassword.getText().toString());
        editor.commit();
        Log.d(TAG, "Preferences saved");
    }

    private void checkNetwork() throws NoInternetException {
        NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new NoInternetException();
        }
    }

    public void checkSetup(String url, String httpUser, String httpPassword, String user, String password) throws MalformedURLException, UrlSuffixException, JSONException, ExecutionException, InterruptedException, NoInternetException {
        this.checkNetwork();
        if (!url.endsWith(SetupActivity.URL_SUFFIX)) {
            throw new UrlSuffixException();
        }
        new URL(url); // To check if the URL is a real one
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", user);
        jsonObject.put("password", password);
        jsonObject.put("op", "login");
        DefaultHttpClient client = new DefaultHttpClient();
        if (!httpUser.isEmpty()) {
            client.getCredentialsProvider().setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(httpUser, httpPassword));
        }
        CheckSetupTask task = new CheckSetupTask(client, url);
        task.execute(jsonObject);
    }


    public class CheckSetupTask extends RequestTask {

        private ProgressDialog dialog;

        public CheckSetupTask(HttpClient client, String url) {
            super(client, url);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.dialog = ProgressDialog.show(SetupActivity.this, "youpi", "youpi");

        }

        @Override
        protected void onPostExecute(JSONObject response) {
            super.onPostExecute(response);
            this.dialog.dismiss();

            try {
                if (response.getInt("status") != 0) {
                    TtrssError reason = TtrssError.valueOf(response.getJSONObject("content").getString("error"));
                    switch (reason) {
                        case LOGIN_ERROR:
                            Log.e(TAG, response.getJSONObject("content").getString("error"));
                            Toast.makeText(getApplicationContext(), R.string.badLogin, Toast.LENGTH_LONG).show();
                        case CLIENT_PROTOCOL_EXCEPTION:
                        case UNREACHABLE_TTRSS:
                        case IO_EXCEPTION:
                            Log.e(TAG, response.getJSONObject("content").getString("message"));
                            Toast.makeText(getApplicationContext(), R.string.connectionError, Toast.LENGTH_LONG).show();
                        case HTTP_AUTH_REQUIERED:
                            Log.e(TAG, response.getJSONObject("content").getString("message"));
                            Toast.makeText(getApplicationContext(), R.string.connectionAuthError, Toast.LENGTH_LONG).show();
                        case UNSUPPORTED_ENCODING:
                        case JSON_EXCEPTION:
                            Log.e(TAG, response.getJSONObject("content").getString("message"));
                            Toast.makeText(getApplicationContext(), R.string.impossibleError, Toast.LENGTH_LONG).show();
                        default:
                            Log.e(TAG, response.getJSONObject("content").getString("message"));
                            Toast.makeText(getApplicationContext(), R.string.unknownError, Toast.LENGTH_LONG).show();
                    }
                }
            } catch (JSONException ex) {

            }

            ((Button) findViewById(R.id.setupOkButton)).setEnabled(true);
        }
    }
}
