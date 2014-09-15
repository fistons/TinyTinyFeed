package org.poopeeland.tinytinyfeed.exceptions;

/**
 * Exception launcged when the is no Internet
 * Created by eric on 14/04/14.
 */
public class NoInternetException extends Exception {

    public NoInternetException() {
        super("No Internet connection right now");
    }
}
