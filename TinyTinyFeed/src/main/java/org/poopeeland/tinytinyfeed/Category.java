package org.poopeeland.tinytinyfeed;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * TTRss Category
 * Created by eric on 04/08/14.
 */
public class Category implements Serializable {

    public static final int NO_ORDER_ID = -1;
    private final int id;
    private final int orderId;
    private final String name;

    public static final Category UNCATEGORIZED = new Category();

    public Category(JSONObject json) throws JSONException {
        this.id = json.getInt("id");
        this.name = json.getString("title");
        this.orderId = json.has("order_id") ? json.getInt("order_id") : NO_ORDER_ID;
    }

    public Category() {
        this.id = 0;
        this.name = "";
        this.orderId = Category.NO_ORDER_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Category)) {
            return false;
        }

        Category c = (Category) o;
        return this.id == c.id;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public int getId() {
        return id;
    }

}
