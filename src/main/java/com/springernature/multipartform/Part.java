package com.springernature.multipartform;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class Part  extends InputStream implements Closeable {
    private String fieldName;
    private boolean formField;
    private String contentType;
    private String fileName;
    private final InputStream inputStream;

    public Part(String fieldName, boolean formField, String contentType, String fileName, InputStream inputStream) {
        this.fieldName = fieldName;
        this.formField = formField;
        this.contentType = contentType;
        this.fileName = fileName;
        this.inputStream = inputStream;
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
            System.out.println("count = " + count);
            if (count < 0) {
                break;
            }
            length += count;
        }
        return new String(bytes, 0, length);
    }
}
