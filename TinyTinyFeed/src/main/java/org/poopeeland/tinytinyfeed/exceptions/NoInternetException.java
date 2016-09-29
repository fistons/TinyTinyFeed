package org.poopeeland.tinytinyfeed.exceptions;

/**
 * Exception launcged when the is no Internet
 * Created by eric on 14/04/14.
 */
public class NoInternetException extends Exception {

    public static final long serialVersionUID = 1L;

    public NoInternetException() {
        super("No Internet connection right now");
    }
}
