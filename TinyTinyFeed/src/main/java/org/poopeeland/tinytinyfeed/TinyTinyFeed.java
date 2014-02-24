package org.poopeeland.tinytinyfeed;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TinyTinyFeed extends ActionBarActivity {

    public static final String PREFERENCE_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_KEY";

    public static final String URL_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_URL";
    public static final String USER_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_USER";
    public static final String PASSWORD_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_PASSWORD";
    public static final String NUM_ARTICLE_KEY = "org.poopeeland.tinytinyfeed.NUM_ARTICLE_KEY";

    public static final String INTENT_VIEW_ARTICLE = "org.poopeeland.tinytinyfeed.VIEW_INTENT";

    private ArticleAdapter adapter;
    private ConnectivityManager connMgr;
    private HttpClient client;
    private String session = "";
    private String user = "";
    private String url = "";
    private String password = "";
    private String numArticles = "";

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiny_tiny_feed);


        List<Article> articleList = new ArrayList<Article>();


        this.adapter = new ArticleAdapter(this, articleList);
        this.listView = (ListView) findViewById(R.id.listView);
        this.listView.setAdapter(this.adapter);
        this.client = new DefaultHttpClient();
        this.connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Article article = (Article) parent.getItemAtPosition(position);
                setArticleToSeenState(article);
                Intent intent = new Intent(TinyTinyFeed.this, ViewArticleActivity.class);
                intent.putExtra(INTENT_VIEW_ARTICLE, article.getUrl());
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isRequieredInfoRegistred()) {
            Toast.makeText(getApplicationContext(), getText(R.string.pleaseSetup), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SetupActivity.class));
        }

        SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY, MODE_PRIVATE);
        this.url = preferences.getString(URL_KEY, "");
        this.user = preferences.getString(USER_KEY, "");
        this.password = preferences.getString(PASSWORD_KEY, "");
        this.numArticles = preferences.getString(NUM_ARTICLE_KEY, "");

        this.updateFeeds(null);
    }

    private void setArticleToSeenState(Article article) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sid", session);
            jsonObject.put("op", "updateArticle");
            jsonObject.put("article_ids", article.getId());
            jsonObject.put("mode", "0");
            jsonObject.put("field", "2");

            RequestTask task = new RequestTask(this.client);
            task.execute(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateFeeds(View view) {
        if (checkNetwork()) {
            if (!isLogged()) {
                login();
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("sid", session);
                jsonObject.put("op", "getHeadlines");
                jsonObject.put("feed_id", "-4");
                jsonObject.put("limit", this.numArticles);
                jsonObject.put("show_excerpt", "true");

                RequestTask task = new RequestTask(this.client);
                task.execute(jsonObject);
                JSONObject response = task.get();
                List<Article> articles = new ArrayList<Article>();
                this.adapter.clear();
                for (int i = 0; i < response.getJSONArray("content").length(); i++) {
                    JSONObject js = response.getJSONArray("content").getJSONObject(i);
                    this.adapter.add(new Article(js));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Vérifiez votre connexion data", Toast.LENGTH_LONG);
        }
    }

    private void login() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user", user);
            jsonObject.put("password", password);
            jsonObject.put("op", "login");

            RequestTask task = new RequestTask(this.client);
            task.execute(jsonObject);
            JSONObject response = task.get();

            this.session = response.getJSONObject("content").getString("session_id");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private boolean checkNetwork() {
        NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private boolean isLogged() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sid", session);
            jsonObject.put("op", "isLoggedIn");

            RequestTask task = new RequestTask(this.client);
            task.execute(jsonObject);
            JSONObject response = task.get();
            return response.getJSONObject("content").getBoolean("status");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isRequieredInfoRegistred() {
        SharedPreferences preferences = getSharedPreferences(TinyTinyFeed.PREFERENCE_KEY, MODE_PRIVATE);
        return (preferences.contains(TinyTinyFeed.URL_KEY) &&
                preferences.contains(TinyTinyFeed.USER_KEY) &&
                preferences.contains(TinyTinyFeed.PASSWORD_KEY) &&
                preferences.contains(TinyTinyFeed.NUM_ARTICLE_KEY));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tiny_tiny_feed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SetupActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }


    }


    private class RequestTask extends AsyncTask<JSONObject, Void, JSONObject> {

        private final HttpClient client;

        public RequestTask(HttpClient client) {
            this.client = client;
        }

        @Override
        protected JSONObject doInBackground(JSONObject... params) {
            JSONObject json = params[0];

            try {
                HttpPost post = new HttpPost(url);
                StringEntity entity = new StringEntity(json.toString());
                entity.setContentType("application/json");
                post.setEntity(entity);

                HttpResponse response = this.client.execute(post);
                BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder sb = new StringBuilder();
                String buffer;
                while ((buffer = r.readLine()) != null) {
                    sb.append(buffer);
                }
                r.close();

                return new JSONObject(sb.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

    }


}
