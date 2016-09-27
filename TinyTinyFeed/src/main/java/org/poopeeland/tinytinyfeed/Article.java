package org.poopeeland.tinytinyfeed;

import android.text.Html;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * TTRss Article
 * Created by setdemr on 03/09/13.
 */
public class Article implements Serializable {

    private static final DateFormat SDF = DateFormat.getDateTimeInstance();

    private final int id;
    private final String title;
    private final String content;
    private final String feedTitle;
    private final String date;
    private final String url;
    private final boolean read;


    public Article(JSONObject json) throws JSONException {
        this.id = json.getInt("id");
        this.title = json.getString("title");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            this.content = Html.fromHtml(json.getString("excerpt"), Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            this.content = Html.fromHtml(json.getString("excerpt")).toString();
        }
        this.url = json.getString("link");

        long timestamp = Long.parseLong(json.getString("updated")) * 1000;
        this.date = Article.SDF.format(new Date(timestamp));
        this.feedTitle = json.getString("feed_title");
        this.read = !json.getBoolean("unread");
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getFeeTitle() {
        return feedTitle;
    }

    public String getDate() {
        return date;
    }

    public String getUrl() {
        return url;
    }

    public int getId() {
        return id;
    }

    public boolean isRead() {
        return read;
    }
}
