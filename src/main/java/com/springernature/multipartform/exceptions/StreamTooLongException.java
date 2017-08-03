package com.springernature.multipartform.exceptions;

import java.io.IOException;

public class StreamTooLongException extends IOException {
    public StreamTooLongException(String message) {
        super(message);
    }
}
