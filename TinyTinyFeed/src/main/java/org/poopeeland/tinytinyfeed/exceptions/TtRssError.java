package org.poopeeland.tinytinyfeed.exceptions;

/**
 * Enum representing the different kind of Exception
 * Created by setdemr on 26/05/2014.
 */
public enum TtRssError {

    UNSUPPORTED_ENCODING,
    CLIENT_PROTOCOL_EXCEPTION,
    IO_EXCEPTION,
    JSON_EXCEPTION,
    LOGIN_ERROR,
    UNREACHABLE_TT_RSS,
    HTTP_AUTH_REQUIRED,
    API_DISABLED,
    SSL_EXCEPTION,
    HTTP_CONNECTION_EXCEPTION
}
