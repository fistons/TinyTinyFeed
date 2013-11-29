package org.poopeeland.tinytinyfeed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class ViewArticleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_article);

        Intent intent = getIntent();

        WebView view = (WebView) findViewById(R.id.webView);
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        view.loadUrl(intent.getStringExtra(TinyTinyFeed.INTENT_VIEW_ARTICLE));
    }


}
