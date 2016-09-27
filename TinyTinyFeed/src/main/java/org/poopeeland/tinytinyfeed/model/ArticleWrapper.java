package org.poopeeland.tinytinyfeed.model;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by setdemr on 27/09/2016.
 */

public class ArticleWrapper {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public static Article fromJson(final String jason) {
        return GSON.fromJson(jason, Article.class);
    }
}
