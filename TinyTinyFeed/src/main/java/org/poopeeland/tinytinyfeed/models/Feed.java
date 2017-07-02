package org.poopeeland.tinytinyfeed.models;

import java.io.Serializable;

/**
 * Represent a tt-rss feed.
 * Created by emr on 02/07/2017.
 */

public class Feed implements Serializable, Comparable<Feed> {
    public static final long serialVersionUID = 1L;

    private String id;
    private String title;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public int compareTo(final Feed feed) {
        return this.getTitle().toLowerCase().compareTo(feed.getTitle().toLowerCase());
    }
}
