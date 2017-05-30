package org.poopeeland.tinytinyfeed.exceptions;

import org.poopeeland.tinytinyfeed.utils.FetchException;

/**
 * Exception launched when the JSON response report an error
 * Created by setdemr on 26/05/2014.
 */
public class CheckException extends FetchException {

    public static final long serialVersionUID = 1L;

    public CheckException(final String message) {
        super(message);
    }
}
