package org.poopeeland.tinytinyfeed;

import android.text.Html;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by setdemr on 03/09/13.
 */
public class Article {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    private int id;
    private String title;
    private String content;
    private String feedTitle;
    private String date;
    private String url;
    private boolean read;


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

    public Article(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFeeTitle() {
        return feedTitle;
    }

    public void setFeedTitle(String feedTitle) {
        this.feedTitle = feedTitle;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
