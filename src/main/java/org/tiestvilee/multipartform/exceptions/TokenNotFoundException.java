package org.tiestvilee.multipartform.exceptions;

import java.io.IOException;

public class TokenNotFoundException extends IOException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
