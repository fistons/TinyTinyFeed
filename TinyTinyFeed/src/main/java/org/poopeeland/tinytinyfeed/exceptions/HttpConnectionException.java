package org.poopeeland.tinytinyfeed.exceptions;

import org.poopeeland.tinytinyfeed.utils.FetchException;

/**
 * {@link Exception} thrown when there is a problem with {@link java.net.HttpURLConnection}.
 *
 * Created by setdemr on 28/09/2016.
 */

public class HttpConnectionException extends FetchException {

    public static final long serialVersionUID = 1L;

    public HttpConnectionException(final Throwable cause) {
        super(cause);
    }
}
