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
public class Article implements Serializable {

    private static final DateFormat SDF = DateFormat.getDateTimeInstance();

    private String excerpt;
    private String feedTitle;
    private int id;
    private String link;
    private String title;
    private boolean unread;
    private long updated;

    public String getExcerpt() {
        return excerpt;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public int getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public String getTitle() {
        return title;
    }

    public boolean isUnread() {
        return unread;
    }

    public String getDate() {
        return SDF.format(new Date(this.updated * 1000));
    }

    @Override
    public String toString() {
        return "Article{" +
                "excerpt='" + excerpt + '\'' +
                ", feedTitle='" + feedTitle + '\'' +
                ", id=" + id +
                ", title='" + title + '\'' +
                ", unread=" + unread +
                ", updated=" + updated +
                ", getDate=" + getDate() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Article that = (Article) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
