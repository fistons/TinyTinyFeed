package org.poopeeland.tinytinyfeed;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViewsService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.poopeeland.tinytinyfeed.exceptions.ArticleNotUpdatedException;
import org.poopeeland.tinytinyfeed.exceptions.NoDataException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Deprecated
public class FeedRetrieverService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private ConnectivityManager connMgr;

    private String session;
    private String url;
    private String password;
    private String user;
    private String numArticles;
    private HttpClient client;

    private void login() throws RequiredInfoNotRegistred {

        checkRequieredInfoRegistred();

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

    public List<Article> updateFeeds() throws NoDataException, RequiredInfoNotRegistred {
        List<Article> list = new ArrayList<Article>();
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
                for (int i = 0; i < response.getJSONArray("content").length(); i++) {
                    list.add(new Article(response.getJSONArray("content").getJSONObject(i)));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            throw new NoDataException();
        }

        return list;
    }


    private boolean isLogged() throws RequiredInfoNotRegistred {

        checkRequieredInfoRegistred();

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


    public void setArticleToSeenState(Article article) throws ArticleNotUpdatedException {
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
            throw new ArticleNotUpdatedException();
        }
    }


    private void checkRequieredInfoRegistred() throws RequiredInfoNotRegistred {
        SharedPreferences preferences = getSharedPreferences(TinyTinyFeed.PREFERENCE_KEY, MODE_PRIVATE);
        if (!(preferences.contains(TinyTinyFeed.URL_KEY) &&
                preferences.contains(TinyTinyFeed.USER_KEY) &&
                preferences.contains(TinyTinyFeed.PASSWORD_KEY) &&
                preferences.contains(TinyTinyFeed.NUM_ARTICLE_KEY))) {
            throw new RequiredInfoNotRegistred();
        }
    }

    private boolean checkNetwork() {
        NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }


    public class LocalBinder extends Binder {
        public FeedRetrieverService getService() {
            return FeedRetrieverService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        this.client = new DefaultHttpClient();

        SharedPreferences preferences = getSharedPreferences(TinyTinyFeed.PREFERENCE_KEY, MODE_PRIVATE);
        this.url = preferences.getString(TinyTinyFeed.URL_KEY, "");
        this.user = preferences.getString(TinyTinyFeed.USER_KEY, "");
        this.password = preferences.getString(TinyTinyFeed.PASSWORD_KEY, "");
        this.numArticles = preferences.getString(TinyTinyFeed.NUM_ARTICLE_KEY, "");

        return mBinder;
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
