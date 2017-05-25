package org.poopeeland.tinytinyfeed.model;

import java.io.Serializable;

import lombok.Data;

/**
 * Represents a TTRss category.
 * <p>
 * Created by eric on 25/05/17.
 */
@Data
public class Category implements Serializable {
    public static final long serialVersionUID = 1L;

    private String id;
    private String title;
}
