package org.poopeeland.tinytinyfeed.model;

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

    public String getDate() {
        return SDF.format(new Date(this.updated * 1000));
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public void setFeedTitle(String feedTitle) {
        this.feedTitle = feedTitle;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
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
}
