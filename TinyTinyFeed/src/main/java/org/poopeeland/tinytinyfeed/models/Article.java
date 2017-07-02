package org.poopeeland.tinytinyfeed.models;

import android.text.Html;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * Represents a TTRss Article.
 * <p>
 * Created by setdemr on 27/09/2016.
 */
@SuppressWarnings("unused")
public class Article implements Serializable, Comparable<Article> {
    public static final long serialVersionUID = 1L;
    private static final DateFormat SDF = DateFormat.getDateTimeInstance();

    private String excerpt;
    private String feedTitle;
    private int id;
    private String link;
    private String title;
    private boolean unread;
    private long updated;

    @SuppressWarnings("deprecation")
    private String cleanHtml(final String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    public String getDate() {
        return SDF.format(new Date(this.updated * 1000));
    }

    public String getExcerpt() {
        return cleanHtml(excerpt);
    }

    public void setExcerpt(final String excerpt) {
        this.excerpt = excerpt;
    }

    public String getFeedTitle() {
        return cleanHtml(feedTitle);
    }

    public void setFeedTitle(final String feedTitle) {
        this.feedTitle = feedTitle;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(final String link) {
        this.link = link;
    }

    public String getTitle() {
        return cleanHtml(title);
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(final boolean unread) {
        this.unread = unread;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(final long updated) {
        this.updated = updated;
    }

    @Override
    public int compareTo(final Article o) {
        if (this.getUpdated() > o.getUpdated()) {
            return -1;
        }
        if (this.getUpdated() < o.getUpdated()) {
            return 1;
        }
        return 0;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Article article = (Article) o;

        return id == article.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
