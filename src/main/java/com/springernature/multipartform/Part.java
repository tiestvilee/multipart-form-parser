package com.springernature.multipartform;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class Part  extends InputStream implements Closeable {
    private String fieldName;
    private boolean formField;
    private String contentType;
    private String name;

    public Part(String fieldName, boolean formField, String contentType, String name) {
        this.fieldName = fieldName;
        this.formField = formField;
        this.contentType = contentType;
        this.name = name;
    }

    @Override public int read() throws IOException {
        throw new UnsupportedOperationException("read not implemented");
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isFormField() {
        return formField;
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return name;
    }

    public void sink() {
        throw new UnsupportedOperationException("sink not implemented");
    }
}
