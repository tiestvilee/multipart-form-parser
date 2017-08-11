package org.tiestvilee.multipartform.exceptions;

public class ParseError extends RuntimeException {
    public ParseError(Exception e) {
        super(e);
    }

    public ParseError(String message) {
        super(message);
    }
}
