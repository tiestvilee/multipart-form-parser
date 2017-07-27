package com.springernature.multipartform;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class Part  extends InputStream implements Closeable {
    private final String fieldName;
    private final boolean formField;
    private final String contentType;
    private final String fileName;
    private final InputStream inputStream;
    private final Map<String, String> headers;
    private final Charset encoding;

    public Part(String fieldName, boolean formField, String contentType, String fileName, InputStream inputStream, Map<String, String> headers, Charset encoding) {
        this.fieldName = fieldName;
        this.formField = formField;
        this.contentType = contentType;
        this.fileName = fileName;
        this.inputStream = inputStream;
        this.headers = headers;
        this.encoding = encoding;
    }

    @Override public int read() throws IOException {
        return inputStream.read();
    }

    @Override public void close() throws IOException {
        inputStream.close();
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

    public String getFileName() {
        return fileName;
    }

    public void sink() {
        throw new UnsupportedOperationException("sink not implemented");
    }

    public String getContentsAsString() throws IOException {
        return getContentsAsString(4096);
    }

    public String getContentsAsString(int maxLength) throws IOException {
        byte[] bytes = new byte[maxLength];
        int length = 0;

        while (true) {
            int count = inputStream.read(bytes, length, maxLength - length);
            if (count < 0) {
                inputStream.close();
                break;
            }
            length += count;
        }
        return new String(bytes, 0, length, encoding);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
