package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class SetupActivity extends Activity {

    private SharedPreferences preferences;
    private EditText url;
    private EditText user;
    private EditText password;
    private EditText numArticle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
    }


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

    @Override
    protected void onPause() {
        super.onPause();

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
