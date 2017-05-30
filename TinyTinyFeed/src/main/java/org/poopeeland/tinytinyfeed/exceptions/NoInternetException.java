package org.poopeeland.tinytinyfeed.exceptions;

import org.poopeeland.tinytinyfeed.utils.FetchException;

/**
 * Exception launcged when the is no Internet
 * Created by eric on 14/04/14.
 */
public class NoInternetException extends FetchException {

    public static final long serialVersionUID = 1L;

    public NoInternetException() {
        super("No Internet connection right now");
    }
}
