package org.poopeeland.tinytinyfeed.models;

import java.io.Serializable;

/**
 * Represents a TTRss category.
 * <p>
 * Created by eric on 25/05/17.
 */
public class Category implements Serializable {
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
}
