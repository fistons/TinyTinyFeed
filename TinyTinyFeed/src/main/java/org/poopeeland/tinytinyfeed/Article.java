package org.poopeeland.tinytinyfeed;

import android.text.Html;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TTRss Article
 * Created by setdemr on 03/09/13.
 */
public class Article implements Serializable {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

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
        this.content = Html.fromHtml(json.getString("excerpt")).toString();
        this.url = json.getString("link");

        long timestamp = Long.parseLong(json.getString("updated")) * 1000;
        Date date1 = new Date(timestamp);
        this.date = Article.sdf.format(date1);
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
