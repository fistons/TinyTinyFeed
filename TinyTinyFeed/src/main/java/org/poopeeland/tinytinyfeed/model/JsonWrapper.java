package org.poopeeland.tinytinyfeed.model;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class used to create {@link Article} from Json string.
 * <p>
 * Created by setdemr on 27/09/2016.
 */
public abstract class JsonWrapper {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();


    public static <T> T fromJson(final String json, final Class<T> cl) {
        return GSON.fromJson(json, cl);
    }
}
