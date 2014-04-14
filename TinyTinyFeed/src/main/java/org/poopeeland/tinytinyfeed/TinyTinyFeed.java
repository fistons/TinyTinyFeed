package org.poopeeland.tinytinyfeed;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.poopeeland.tinytinyfeed.exceptions.ArticleNotUpdatedException;
import org.poopeeland.tinytinyfeed.exceptions.NoDataException;
import org.poopeeland.tinytinyfeed.exceptions.RequiredInfoNotRegistred;

import java.util.ArrayList;
import java.util.List;

public class TinyTinyFeed extends ActionBarActivity {

    public static final String PREFERENCE_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_KEY";

    public static final String URL_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_URL";
    public static final String USER_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_USER";
    public static final String PASSWORD_KEY = "org.poopeeland.tinytinyfeed.PREFERENCE_PASSWORD";
    public static final String NUM_ARTICLE_KEY = "org.poopeeland.tinytinyfeed.NUM_ARTICLE_KEY";

    public static final String INTENT_VIEW_ARTICLE = "org.poopeeland.tinytinyfeed.VIEW_INTENT";

    private ArticleAdapter adapter;
    private ConnectivityManager connMgr;

    private String session = "";
    private String user = "";
    private String url = "";
    private String password = "";
    private String numArticles = "";

    private ListView listView;


    private FeedRetriever service;
    private boolean bound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiny_tiny_feed);


        List<Article> articleList = new ArrayList<Article>();


        this.adapter = new ArticleAdapter(this, articleList);
        this.listView = (ListView) findViewById(R.id.listView);
        this.listView.setAdapter(this.adapter);

        this.connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Article article = (Article) parent.getItemAtPosition(position);
                try {
                    service.setArticleToSeenState(article);
                    Intent intent = new Intent(TinyTinyFeed.this, ViewArticleActivity.class);
                    intent.putExtra(INTENT_VIEW_ARTICLE, article.getUrl());
                    startActivity(intent);
                } catch (ArticleNotUpdatedException ex) {
                    Toast.makeText(TinyTinyFeed.this, "L'article n'a pas été mise à jour", Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    @Override
    protected void onPause() {
        super.onPause();
        // Unbind from the service
        if (bound) {
            unbindService(mConnection);
            bound = false;
        }
    }

    protected void onStart() {
        super.onStart();

        this.bound();
    }

    private void bound() {
        Intent intent = new Intent(this, FeedRetriever.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //this.updateFeeds(null);
    }


    public void updateFeeds(View view) {
        if (bound) {
            try {
                for (Article a : service.updateFeeds()) {
                    adapter.add(a);
                }
            } catch (NoDataException ex) {
                Toast.makeText(this, "Pas de données!", Toast.LENGTH_LONG).show();
            } catch (RequiredInfoNotRegistred requiredInfoNotRegistred) {
                Toast.makeText(this, "Pas loggé!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, SetupActivity.class));
            }
        } else {
            this.bound();
        }
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


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder mservice) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            FeedRetriever.LocalBinder binder = (FeedRetriever.LocalBinder) mservice;
            service = binder.getService();
            bound = true;
            updateFeeds(null);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };



}
