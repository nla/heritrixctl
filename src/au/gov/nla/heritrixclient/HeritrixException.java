package au.gov.nla.heritrixclient;

import java.io.IOException;
import java.net.URI;

class HeritrixException extends RuntimeException {

    HeritrixException(int statusCode, String reasonPhrase, URI uri) {
        super("" + statusCode + " " + reasonPhrase + " at " + uri);
    }

    HeritrixException(String message, Throwable e) {
        super(message, e);
    }

    public HeritrixException(Throwable e) {
        super(e);
    }

    public HeritrixException(String message) {
        super(message);
    }
}
