package org.poopeeland.tinytinyfeed.model;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import lombok.Data;

/**
 * Represents a TTRss Article.
 * <p>
 * Created by setdemr on 27/09/2016.
 */
@SuppressWarnings("unused")
@Data
public class Article implements Serializable {
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

}
