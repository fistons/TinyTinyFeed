package org.poopeeland.tinytinyfeed.utils;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * Utility class to play with files.
 * Created by setdemr on 30/09/2016.
 */
public abstract class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    /**
     * Quietly close a closable.
     *
     * @param closeable the closable to close.
     */
    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ex) {
            // Ignore
            Log.wtf(TAG, "IOException while closing... a closable", ex);
        }
    }
}
