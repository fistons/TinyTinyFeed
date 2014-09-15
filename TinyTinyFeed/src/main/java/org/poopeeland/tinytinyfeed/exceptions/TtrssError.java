package org.poopeeland.tinytinyfeed.exceptions;

/**
 * Enum representing the different kind of Exception
 * Created by setdemr on 26/05/2014.
 */
public enum TtrssError {

    UNSUPPORTED_ENCODING,
    CLIENT_PROTOCOL_EXCEPTION,
    IO_EXCEPTION,
    JSON_EXCEPTION,
    LOGIN_ERROR,
    UNREACHABLE_TTRSS,
    HTTP_AUTH_REQUIERED,
    API_DISABLED
}
